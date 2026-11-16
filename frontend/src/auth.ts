import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";
import { jwtCallback, sessionCallback } from "@/lib/auth/callbacks";
import { env } from "@/lib/env";

// Auth.js only reads AUTH_URL from process.env, so hand it the validated value (with its default).
process.env.AUTH_URL ??= env.AUTH_URL;

const keycloak = {
  issuer: env.KEYCLOAK_ISSUER_URI,
  clientId: env.AUTH_KEYCLOAK_ID,
  clientSecret: env.AUTH_KEYCLOAK_SECRET,
};

export const { handlers, auth, signIn, signOut } = NextAuth({
  secret: env.AUTH_SECRET,
  // JWT strategy: the session is an encrypted (JWE), httpOnly cookie, no database.
  session: { strategy: "jwt" },
  pages: { signIn: "/signin" },
  providers: [
    // Authorization Code flow; the code exchange happens on the server with the client secret.
    // PKCE (S256) plus state are explicit so a later config change cannot drop them silently.
    Keycloak({ ...keycloak, checks: ["pkce", "state"] }),
  ],
  callbacks: {
    jwt: (params) => jwtCallback(params, keycloak),
    session: sessionCallback,
  },
});
