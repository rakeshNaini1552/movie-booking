import { encode } from "next-auth/jwt";
import type { BrowserContext } from "@playwright/test";
import { E2E_AUTH_SECRET, E2E_BASE_URL } from "./settings";

// Auth.js stores the session as an encrypted cookie. The tests do not run Keycloak, so they
// create that cookie themselves with the test-only secret the app under test was started with.
// This cannot be done against a real deployment: it needs the app's AUTH_SECRET.
const COOKIE_NAME = "authjs.session-token";

export type TestUser = { name: string; token: "e2e-user" | "e2e-admin"; roles: string[] };

export const CUSTOMER: TestUser = { name: "Casey Customer", token: "e2e-user", roles: ["user"] };
export const ADMIN: TestUser = {
  name: "Avery Admin",
  token: "e2e-admin",
  roles: ["user", "catalog-admin"],
};

export async function signInAs(context: BrowserContext, user: TestUser): Promise<void> {
  const farFuture = Math.floor(Date.now() / 1000) + 24 * 60 * 60;
  const value = await encode({
    secret: E2E_AUTH_SECRET,
    salt: COOKIE_NAME,
    token: {
      name: user.name,
      email: `${user.token}@example.test`,
      sub: user.token,
      // The mock gateway treats the bearer token as the user's name.
      accessToken: user.token,
      refreshToken: "e2e-refresh",
      expiresAt: farFuture,
      roles: user.roles,
    },
  });
  await context.addCookies([
    { name: COOKIE_NAME, value, url: E2E_BASE_URL, httpOnly: true, sameSite: "Lax" },
  ]);
}
