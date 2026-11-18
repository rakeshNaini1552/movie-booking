// Content-Security-Policy: tells the browser which sources this site may load code, styles and
// images from, and where its forms may post. If an attacker ever manages to inject markup into a
// page, the browser refuses to load their script from another site or send a form to it.
//
// script-src and style-src allow 'unsafe-inline' because Next.js writes small inline scripts
// into every page to start React. The stricter option is a per-request nonce, which forces every
// page to render at request time; that is a later hardening step, not done here.
export function contentSecurityPolicy({
  keycloakOrigin,
  development,
}: {
  // Sign-in and sign-out end in a redirect to Keycloak, which browsers treat as a form target.
  keycloakOrigin: string;
  // `next dev` needs eval for hot reloading. Never allowed in a production build.
  development: boolean;
}): string {
  return [
    "default-src 'self'",
    `script-src 'self' 'unsafe-inline'${development ? " 'unsafe-eval'" : ""}`,
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data:",
    "font-src 'self'",
    `connect-src 'self'${development ? " ws:" : ""}`,
    `form-action 'self' ${keycloakOrigin}`,
    "frame-ancestors 'none'",
    "base-uri 'self'",
    "object-src 'none'",
  ].join("; ");
}
