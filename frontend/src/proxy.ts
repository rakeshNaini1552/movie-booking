import { NextResponse } from "next/server";
import { auth } from "@/auth";

// Paths that need a signed-in user. Pages under them also re-check the session themselves.
const PROTECTED_PREFIXES = ["/account", "/movies"];

// Runs on every page request, not only the protected ones: the proxy can set cookies, so this
// is where a refreshed token gets written back to the session cookie. Server components cannot.
export const proxy = auth((request) => {
  const { pathname, search } = request.nextUrl;
  const isProtected = PROTECTED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
  if (request.auth || !isProtected) return;

  const signIn = request.nextUrl.clone();
  signIn.pathname = "/signin";
  signIn.search = "";
  // The sign-in page and action validate this again before using it.
  signIn.searchParams.set("callbackUrl", pathname + search);
  return NextResponse.redirect(signIn);
});

export const config = {
  matcher: ["/((?!api/auth|_next/static|_next/image|favicon.ico).*)"],
};
