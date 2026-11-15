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

| Variable              | Default                                      | Purpose                            |
| --------------------- | -------------------------------------------- | ---------------------------------- |
| `GATEWAY_URL`         | `http://localhost:8090`                      | Gateway service base URL           |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/movie-booking` | Keycloak realm issuer              |
| `PORT`                | `3001`                                       | Port, set in the shell (see above) |

None are secrets. Future secrets (client secret, auth secret) get no default. Never use `NEXT_PUBLIC_` for these. App code reads them from `@/lib/env`, which is `server-only`.

## Reaching the gateway

The default `GATEWAY_URL` assumes the gateway is on `localhost:8090`. When the backend runs in Kubernetes, forward it:

```bash
kubectl port-forward -n movie-booking svc/gateway-service 8090:8090
```

The pages do not call the gateway yet; this is needed once they fetch real data.

## Scripts

| Script                 | What it does                                         |
| ---------------------- | ---------------------------------------------------- |
| `npm run dev`          | Dev server                                           |
| `npm run build`        | Production build                                     |
| `npm start`            | Serve the production build                           |
| `npm run lint`         | ESLint (Next + Prettier-compatible rules)            |
| `npm run format`       | Prettier, write                                      |
| `npm run format:check` | Prettier, check only (CI)                            |
| `npm run typecheck`    | `next typegen` then `tsc --noEmit`                   |
| `npm test`             | Vitest (env tests in Node, component tests in jsdom) |

CI (`test (frontend)`) runs lint, format check, typecheck, tests and build.

## Layout

- `src/app`: routes (`/`, `/movies`, `not-found`) and `loading`/`error` pages for `/movies`
- `src/components`: shared components
- `src/lib`: env validation
- Tests sit next to the code as `*.test.ts` (Node) and `*.test.tsx` (jsdom)

Before changing Next.js code, read `AGENTS.md`: this Next.js version differs from older ones, and its docs are in `node_modules/next/dist/docs/`.
