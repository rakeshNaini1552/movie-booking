# Movie Booking frontend

Next.js 16 (App Router, TypeScript strict) UI for the movie booking platform. Server-side code talks to the backend through the gateway service.

## Run it

Requires Node 26 (see `.nvmrc`).

```bash
npm ci
npm run dev    # http://localhost:3001
```

Override the port with `PORT=4000 npm run dev`. The scripts pass `-p ${PORT:-<default>}` because Next reads `PORT` from the shell only, before `.env` files are loaded, so a `PORT` in `.env.local` does not move the server. The default (3001, since Grafana uses 3000) lives in the `config.port` field of `package.json`. The scripts use POSIX shell expansion (macOS, Linux, WSL).

## Environment variables

Server-only, validated with Zod at startup (`src/lib/env-schema.ts`, run from `next.config.ts`), so `next dev`, `next build` and `next start` fail fast and name every invalid variable (never its value). Copy `.env.example` to `.env.local` to override; `.env.local` is git-ignored.

| Variable               | Default                                      | Purpose                                                 |
| ---------------------- | -------------------------------------------- | ------------------------------------------------------- |
| `GATEWAY_URL`          | `http://localhost:8090`                      | Gateway service base URL                                |
| `GATEWAY_TIMEOUT_MS`   | `5000`                                       | Timeout for one gateway call (100 to 60000)             |
| `KEYCLOAK_ISSUER_URI`  | `http://localhost:8180/realms/movie-booking` | Keycloak realm issuer                                   |
| `PORT`                 | `3001`                                       | Port, set in the shell (see above)                      |
| `AUTH_URL`             | `http://localhost:3001`                      | Public origin of this app (callback, post-logout)       |
| `AUTH_KEYCLOAK_ID`     | `movie-booking-web`                          | Keycloak client id                                      |
| `AUTH_SECRET`          | **none, required**, min 32 chars             | Encrypts the session cookie (`openssl rand -base64 32`) |
| `AUTH_KEYCLOAK_SECRET` | **none, required**                           | Client secret of the confidential client                |

Secrets have no default: a missing one stops startup. Never use `NEXT_PUBLIC_` for any of these. App code reads them from `@/lib/env`, which is `server-only`.

## Login (Keycloak)

Auth.js v5 (`next-auth`, pinned beta) with the Keycloak provider: Authorization Code flow with PKCE (S256), code exchange on the server with the client secret. The session is an encrypted, httpOnly JWT cookie. Access and refresh tokens live only in that cookie and in server code; the session that client code can read has just `name`, `email` and `roles`. Server code gets the access token from `getAccessToken()` (`src/lib/auth/access-token.ts`, `server-only`).

Keycloak client `movie-booking-web` must exist in the realm (not in the realm export yet):

- Client authentication: on (confidential), Standard flow on, Direct access grants off
- Valid redirect URIs: `<AUTH_URL>/api/auth/callback/keycloak`
- Valid post logout redirect URIs: `<AUTH_URL>/`
- Copy its secret from the Credentials tab into `AUTH_KEYCLOAK_SECRET`

`src/proxy.ts` guards `/account` and keeps the session cookie refreshed; pages also check the session themselves. Sign out clears the cookie and ends the Keycloak session (Keycloak asks to confirm, because no `id_token_hint` is sent, so no token goes through the browser URL).

## Reaching the gateway

The catalog pages (`/movies`, `/movies/[id]`) call the gateway at `POST {GATEWAY_URL}/graphql` with the signed-in user's access token (`src/lib/catalog`, `server-only`; the token never reaches the browser). The default `GATEWAY_URL` assumes the gateway is on `localhost:8090`. To run against the backend in Kubernetes:

```bash
kubectl port-forward -n movie-booking svc/gateway-service 8090:8090
```

Keycloak must be reachable too (`KEYCLOAK_ISSUER_URI`, default `localhost:8180`), because the gateway validates the token against it; forward it the same way if it runs in the cluster. Then `npm run dev`, sign in, and open `/movies`.

What you see when something is off:

- Not signed in, or the gateway answers 401: redirect to `/signin`, then back to the page you asked for.
- Gateway down, slow (over `GATEWAY_TIMEOUT_MS`), or a GraphQL error: the error page ("Something went wrong...") with a digest you can find in the server log. The cause is never shown in the browser.
- Unknown or malformed movie id: the 404 page.

Seat map: `/movies/[id]/shows/[showId]/seats` reads `GET /api/v1/shows/{showId}/seats` through the gateway (the gateway must have the `seat-service` route, and `SEAT_SERVICE_URL` set). It only lets the user pick up to 6 seats (`MAX_SEATS`); nothing is held or booked yet, and "Continue to booking" is disabled. "Refresh availability" re-reads the seat map (no polling), and a selected seat that was taken meanwhile is dropped from the selection. A show with no seats shows a "not available yet" message. Both gateway clients (GraphQL and REST) share `src/lib/gateway/request.ts`: token, timeout, 401/404 mapping and the one-line failure log.

Notes: the catalog returns no total count, so the list asks for one extra row to know whether "Next" applies. Show times are shown in UTC because the catalog has no theater time zone, and prices have no currency symbol because the catalog does not state one. Cancelled shows are hidden.

## Scripts

| Script                 | What it does                                            |
| ---------------------- | ------------------------------------------------------- |
| `npm run dev`          | Dev server                                              |
| `npm run build`        | Production build                                        |
| `npm start`            | Serve the production build                              |
| `npm run lint`         | ESLint (Next + Prettier-compatible rules)               |
| `npm run format`       | Prettier, write                                         |
| `npm run format:check` | Prettier, check only (CI)                               |
| `npm run typecheck`    | `next typegen` then `tsc --noEmit` (needs `.env.local`) |
| `npm test`             | Vitest (env tests in Node, component tests in jsdom)    |

`npm run typecheck` loads `next.config.ts`, which validates the env, so it needs `.env.local` like `npm run dev` does: copy `.env.example` to `.env.local` and fill in `AUTH_SECRET` and `AUTH_KEYCLOAK_SECRET`.

CI (`test (frontend)`) runs lint, format check, typecheck, tests and build. The typecheck and build steps get fake placeholder secrets in their own `env`.

## Layout

- `src/app`: routes (`/`, `/movies`, `/movies/[id]`, `/movies/[id]/shows/[showId]/seats`, `/account`, `/signin`, `/api/auth/*`, `not-found`) and `loading`/`error` pages for `/movies`
- `src/components`: shared components
- `src/lib`: env validation and formatting; `src/lib/auth`: token refresh, callbacks, server actions; `src/lib/gateway`: shared gateway request, REST helper and errors; `src/lib/catalog`: GraphQL client and queries; `src/lib/seats`: seat map
- `src/auth.ts`, `src/proxy.ts`: Auth.js setup and the route guard
- Tests sit next to the code as `*.test.ts` (Node) and `*.test.tsx` (jsdom)

Before changing Next.js code, read `AGENTS.md`: this Next.js version differs from older ones, and its docs are in `node_modules/next/dist/docs/`.
