import { redirect } from "next/navigation";
import { auth } from "@/auth";

// Second line of defence: the proxy already redirects anonymous users, but a proxy matcher
// mistake must not expose this page. Every protected page checks the session itself.
export default async function AccountDetails() {
  const session = await auth();
  if (!session) redirect(`/signin?callbackUrl=${encodeURIComponent("/account")}`);

  const { name, email, roles } = session.user;
  return (
    <dl>
      <dt>Name</dt>
      <dd>{name ?? "-"}</dd>
      <dt>Email</dt>
      <dd>{email ?? "-"}</dd>
      <dt>Roles</dt>
      <dd>{roles.length > 0 ? roles.join(", ") : "none"}</dd>
    </dl>
  );
}
