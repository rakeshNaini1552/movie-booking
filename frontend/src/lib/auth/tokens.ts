import type { JWT } from "next-auth/jwt";

// Lives only inside the encrypted, httpOnly session cookie and server-side code.
// It is never copied into the session object that client code can read (see callbacks.ts).
export type AppToken = JWT & {
  accessToken?: string;
  refreshToken?: string;
  /** Access token expiry, seconds since epoch. */
  expiresAt?: number;
  roles?: string[];
};

export type KeycloakConfig = { issuer: string; clientId: string; clientSecret: string };

export type RefreshDeps = {
  fetch?: typeof fetch;
  /** Current time in seconds since epoch. */
  now?: () => number;
};

// Refresh this long before the access token really expires, so a request that starts
// just before expiry does not reach the API with a dead token.
export const REFRESH_LEEWAY_SECONDS = 30;
const REFRESH_TIMEOUT_MS = 10_000;

export const nowSeconds = () => Math.floor(Date.now() / 1000);

export function extractRealmRoles(payload: unknown): string[] {
  if (typeof payload !== "object" || payload === null) return [];
  const roles = (payload as { realm_access?: { roles?: unknown } }).realm_access?.roles;
  return Array.isArray(roles)
    ? roles.filter((role): role is string => typeof role === "string")
    : [];
}

// Reads the payload without verifying the signature: this token came straight from
// Keycloak's token endpoint over the server-side exchange, and the roles are only used for
// display. The API that receives the token must (and does) validate it itself.
export function rolesFromAccessToken(accessToken: string): string[] {
  try {
    const payload = accessToken.split(".")[1] ?? "";
    return extractRealmRoles(JSON.parse(Buffer.from(payload, "base64url").toString("utf8")));
  } catch {
    return [];
  }
}

export function isExpiring(token: AppToken, now: number): boolean {
  return token.expiresAt === undefined || token.expiresAt - REFRESH_LEEWAY_SECONDS <= now;
}

// Concurrent renders (layout and page both read the session) would otherwise spend the same
// refresh token twice, which fails if Keycloak rotates refresh tokens.
const inflight = new Map<string, Promise<AppToken | null>>();

// Returns the refreshed token, or null when it cannot be refreshed. Null ends the session
// (Auth.js clears the cookie), so a dead refresh token is tried once, never in a loop.
export function refreshTokens(
  token: AppToken,
  config: KeycloakConfig,
  deps: RefreshDeps = {},
): Promise<AppToken | null> {
  const { refreshToken } = token;
  if (!refreshToken) return Promise.resolve(null);

  const pending = inflight.get(refreshToken);
  if (pending) return pending;

  const request = requestRefresh(token, refreshToken, config, deps).finally(() =>
    inflight.delete(refreshToken),
  );
  inflight.set(refreshToken, request);
  return request;
}

async function requestRefresh(
  token: AppToken,
  refreshToken: string,
  { issuer, clientId, clientSecret }: KeycloakConfig,
  { fetch: fetchFn = fetch, now = nowSeconds }: RefreshDeps,
): Promise<AppToken | null> {
  try {
    const response = await fetchFn(`${issuer.replace(/\/$/, "")}/protocol/openid-connect/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: refreshToken,
        client_id: clientId,
        client_secret: clientSecret,
      }),
      signal: AbortSignal.timeout(REFRESH_TIMEOUT_MS),
    });
    if (!response.ok) {
      console.warn(`Keycloak token refresh rejected (HTTP ${response.status})`);
      return null;
    }

    const data: unknown = await response.json();
    const { access_token, refresh_token, expires_in } = (data ?? {}) as Record<string, unknown>;
    if (typeof access_token !== "string" || typeof expires_in !== "number") return null;

    return {
      ...token,
      accessToken: access_token,
      // Keycloak may rotate the refresh token; keep the old one if it did not.
      refreshToken: typeof refresh_token === "string" ? refresh_token : refreshToken,
      expiresAt: now() + expires_in,
      roles: rolesFromAccessToken(access_token),
    };
  } catch {
    // Network error, timeout or bad JSON. Deliberately no error details in the logs.
    console.warn("Keycloak token refresh failed");
    return null;
  }
}
