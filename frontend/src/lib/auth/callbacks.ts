import type { Account, Session } from "next-auth";
import type { JWT } from "next-auth/jwt";
import {
  type AppToken,
  type KeycloakConfig,
  type RefreshDeps,
  isExpiring,
  nowSeconds,
  refreshTokens,
  rolesFromAccessToken,
} from "./tokens";

// Auth.js `jwt` callback: runs at sign-in and every time the session is read.
// Returning null ends the session, which is how a failed refresh forces a new sign-in.
export async function jwtCallback(
  { token, account }: { token: JWT; account?: Account | null },
  config: KeycloakConfig,
  deps: RefreshDeps = {},
): Promise<JWT | null> {
  const current = token as AppToken;

  if (account) {
    if (!account.access_token || !account.refresh_token) return null;
    return {
      ...current,
      accessToken: account.access_token,
      refreshToken: account.refresh_token,
      expiresAt: account.expires_at,
      roles: rolesFromAccessToken(account.access_token),
    };
  }

  const now = (deps.now ?? nowSeconds)();
  if (!isExpiring(current, now)) return current;
  return refreshTokens(current, config, deps);
}

// Auth.js `session` callback: builds what client code can read. Fields are listed one by one
// on purpose, so a token added to the JWT later cannot leak by spreading it into the session.
export function sessionCallback({ session, token }: { session: Session; token: JWT }): Session {
  const { roles } = token as AppToken;
  return {
    expires: session.expires,
    user: { name: token.name, email: token.email, roles: roles ?? [] },
  };
}
