import "server-only";
import { headers } from "next/headers";
import { getToken } from "next-auth/jwt";
import { env } from "@/lib/env";
import { type AppToken, isExpiring, nowSeconds, refreshTokens } from "./tokens";

// For server components and server actions that call the gateway on the user's behalf.
// Returns null when there is no session or it can no longer be refreshed.
// The token must stay on the server: never pass it to a client component or return it from an action.
export async function getAccessToken(): Promise<string | null> {
  const token = (await getToken({
    req: { headers: await headers() },
    secret: env.AUTH_SECRET,
    // Auth.js names the cookie differently over https, and that name is also the decryption salt.
    secureCookie: new URL(env.AUTH_URL).protocol === "https:",
  })) as AppToken | null;
  if (!token?.accessToken) return null;
  if (!isExpiring(token, nowSeconds())) return token.accessToken;

  // Server components cannot set cookies, so a refresh here is not persisted; the proxy
  // persists it on the next page request. Good enough to make this call succeed.
  const refreshed = await refreshTokens(token, {
    issuer: env.KEYCLOAK_ISSUER_URI,
    clientId: env.AUTH_KEYCLOAK_ID,
    clientSecret: env.AUTH_KEYCLOAK_SECRET,
  });
  return refreshed?.accessToken ?? null;
}
