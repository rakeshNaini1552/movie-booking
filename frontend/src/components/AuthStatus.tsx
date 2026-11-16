import { connection } from "next/server";
import { auth } from "@/auth";
import AuthNav from "@/components/AuthNav";
import { signInAction, signOutAction } from "@/lib/auth/actions";

// Reads the session, so it must render behind a <Suspense> boundary (Cache Components).
export default async function AuthStatus() {
  // auth() draws random values (crypto.getRandomValues) before it reads any request data, which
  // Cache Components rejects while prerendering. Opting into request time first removes that.
  await connection();
  const session = await auth();
  return (
    <AuthNav
      user={session ? { name: session.user.name } : null}
      signInAction={signInAction}
      signOutAction={signOutAction}
    />
  );
}
