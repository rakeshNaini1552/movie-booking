import "server-only";
import { notFound, redirect } from "next/navigation";
import { connection } from "next/server";
import { auth } from "@/auth";
import { env } from "@/lib/env";

export async function isAdmin(): Promise<boolean> {
  const session = await auth();
  return session?.user.roles.includes(env.CATALOG_ADMIN_ROLE) ?? false;
}

// For admin pages. A signed-out visitor goes to sign-in. A signed-in user without the admin
// role gets the normal "not found" page, so the admin area is not advertised to customers.
// This check is for the page only: catalog-service checks the role again on every write.
export async function requireAdmin(returnTo: string): Promise<void> {
  // auth() draws random values, which Next.js rejects while prerendering. Opting into request
  // time first avoids that (same reason as in AuthStatus).
  await connection();
  const session = await auth();
  if (!session) redirect(`/signin?callbackUrl=${encodeURIComponent(returnTo)}`);
  if (!session.user.roles.includes(env.CATALOG_ADMIN_ROLE)) notFound();
}
