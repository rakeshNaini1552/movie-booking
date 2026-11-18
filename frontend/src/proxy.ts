import { NextResponse } from "next/server";
import { auth } from "@/auth";
import { env } from "@/lib/env";
import { contentSecurityPolicy } from "@/lib/security/csp";

// Paths that need a signed-in user. Pages under them also re-check the session themselves.
const PROTECTED_PREFIXES = ["/account", "/movies", "/bookings", "/admin"];

// Built once: both inputs are fixed for the life of the server process. It is set here and not
// in next.config.ts because the config is evaluated when the image is built, and the Keycloak
// address is only known when the container starts.
const csp = contentSecurityPolicy({
  keycloakOrigin: new URL(env.KEYCLOAK_ISSUER_URI).origin,
  development: process.env.NODE_ENV === "development",
});

// Runs on every page request, not only the protected ones: the proxy can set cookies, so this
// is where a refreshed token gets written back to the session cookie. Server components cannot.
export const proxy = auth((request) => {
  const { pathname, search } = request.nextUrl;
  const isProtected = PROTECTED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
  if (request.auth || !isProtected) {
    const response = NextResponse.next();
    response.headers.set("Content-Security-Policy", csp);
    return response;
  }

  const signIn = request.nextUrl.clone();
  signIn.pathname = "/signin";
  signIn.search = "";
  // The sign-in page and action validate this again before using it.
  signIn.searchParams.set("callbackUrl", pathname + search);
  return NextResponse.redirect(signIn);
});

export const config = {
  // api/health is left out so Kubernetes probes do not go through session handling.
  matcher: ["/((?!api/auth|api/health|_next/static|_next/image|favicon.ico).*)"],
};
