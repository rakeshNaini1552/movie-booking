import type { DefaultSession } from "next-auth";

// The only things client code can read from the session. No tokens, on purpose.
declare module "next-auth" {
  interface Session {
    user: { roles: string[] } & DefaultSession["user"];
  }
}
