import { NextRequest } from "next/server";
import { describe, expect, it, vi } from "vitest";

// Replace the Auth.js wrapper with a pass-through so the proxy's own logic runs on a fake request.
vi.mock("@/auth", () => ({ auth: (handler: unknown) => handler }));
vi.mock("@/lib/env", () => ({
  env: { KEYCLOAK_ISSUER_URI: "http://keycloak.test:8180/realms/movie-booking" },
}));

import { proxy } from "./proxy";

type Handler = (request: NextRequest & { auth: unknown }) => Response | undefined;
const run = (path: string, signedIn: boolean) => {
  const request = new NextRequest(`http://localhost:3001${path}`) as NextRequest & {
    auth: unknown;
  };
  request.auth = signedIn ? { user: { name: "A", roles: [] } } : null;
  return (proxy as unknown as Handler)(request);
};

// A request that is let through carries no redirect, and gets the Content-Security-Policy.
const expectPassThrough = (response: Response | undefined) => {
  expect(response?.headers.get("location")).toBeNull();
  expect(response?.headers.get("content-security-policy")).toContain(
    "form-action 'self' http://keycloak.test:8180",
  );
};

describe("proxy", () => {
  it("redirects an anonymous user on /account to sign-in, remembering where they were going", () => {
    const response = run("/account?tab=roles", false)!;
    const location = new URL(response.headers.get("location")!);
    expect(location.pathname).toBe("/signin");
    expect(location.searchParams.get("callbackUrl")).toBe("/account?tab=roles");
  });

  it("lets a signed-in user through", () => {
    expectPassThrough(run("/account", true));
  });

  it.each([
    "/movies",
    "/movies/3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b",
    "/movies/3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b/shows/00000000-0000-0000-0000-000000000001/seats",
    "/bookings",
    "/bookings/3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b",
    "/admin/movies",
  ])("redirects an anonymous user on %s to sign-in with a 307", (path) => {
    const response = run(`${path}?q=dark`, false)!;
    expect(response.status).toBe(307);
    const location = new URL(response.headers.get("location")!);
    expect(location.pathname).toBe("/signin");
    expect(location.searchParams.get("callbackUrl")).toBe(`${path}?q=dark`);
  });

  it("lets a signed-in user through to the movies", () => {
    expectPassThrough(run("/movies", true));
  });

  it("does not match look-alike paths such as /moviesfoo", () => {
    expectPassThrough(run("/moviesfoo", false));
  });

  it("does not guard public pages", () => {
    expectPassThrough(run("/", false));
    expectPassThrough(run("/signin", false));
  });
});
