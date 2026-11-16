import { NextRequest } from "next/server";
import { describe, expect, it, vi } from "vitest";

// Replace the Auth.js wrapper with a pass-through so the proxy's own logic runs on a fake request.
vi.mock("@/auth", () => ({ auth: (handler: unknown) => handler }));

import { proxy } from "./proxy";

type Handler = (request: NextRequest & { auth: unknown }) => Response | undefined;
const run = (path: string, signedIn: boolean) => {
  const request = new NextRequest(`http://localhost:3001${path}`) as NextRequest & {
    auth: unknown;
  };
  request.auth = signedIn ? { user: { name: "A", roles: [] } } : null;
  return (proxy as unknown as Handler)(request);
};

describe("proxy", () => {
  it("redirects an anonymous user on /account to sign-in, remembering where they were going", () => {
    const response = run("/account?tab=roles", false)!;
    const location = new URL(response.headers.get("location")!);
    expect(location.pathname).toBe("/signin");
    expect(location.searchParams.get("callbackUrl")).toBe("/account?tab=roles");
  });

  it("lets a signed-in user through", () => {
    expect(run("/account", true)).toBeUndefined();
  });

  it("does not guard public pages", () => {
    expect(run("/movies", false)).toBeUndefined();
  });
});
