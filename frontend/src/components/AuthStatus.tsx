import { connection } from "next/server";
import { auth } from "@/auth";
import AuthNav from "@/components/AuthNav";
import { signInAction, signOutAction } from "@/lib/auth/actions";
import { env } from "@/lib/env";

// Reads the session, so it must render behind a <Suspense> boundary (Cache Components).
export default async function AuthStatus() {
  // auth() draws random values (crypto.getRandomValues) before it reads any request data, which
  // Cache Components rejects while prerendering. Opting into request time first removes that.
  await connection();
  const session = await auth();
  return (
    <AuthNav
      user={
        session
          ? {
              name: session.user.name,
              // Only decides whether the Admin link is shown. The admin pages and the catalog
              // service both check the role again.
              isAdmin: session.user.roles.includes(env.CATALOG_ADMIN_ROLE),
            }
          : null
      }
      signInAction={signInAction}
      signOutAction={signOutAction}
    />
  );
}
