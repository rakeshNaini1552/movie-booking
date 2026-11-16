// A post-login destination must be a same-site path. Anything else (absolute URLs,
// "//evil.com", "/\evil.com", control characters browsers strip, javascript:) falls back.
export function safeCallbackPath(value: unknown, fallback = "/"): string {
  if (typeof value !== "string") return fallback;
  if (!value.startsWith("/") || value.startsWith("//") || /[\\\u0000-\u001f]/.test(value)) {
    return fallback;
  }
  // Belt and braces: resolve it and make sure it stays on the same origin.
  const base = "http://same-site.invalid";
  return new URL(value, base).origin === base ? value : fallback;
}

// RP-initiated logout. Without client_id and post_logout_redirect_uri Keycloak would leave
// the user on its own page. No id_token_hint on purpose: that would put a token in the
// browser URL, so Keycloak asks the user to confirm the logout instead.
export function buildLogoutUrl(
  issuer: string,
  clientId: string,
  postLogoutRedirectUri: string,
): string {
  const url = new URL(`${issuer.replace(/\/$/, "")}/protocol/openid-connect/logout`);
  url.searchParams.set("client_id", clientId);
  url.searchParams.set("post_logout_redirect_uri", postLogoutRedirectUri);
  return url.toString();
}
