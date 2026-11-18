import { redirect } from "next/navigation";
import { auth } from "@/auth";

const termClass = "text-sm text-zinc-400";

// Second line of defence: the proxy already redirects anonymous users, but a proxy matcher
// mistake must not expose this page. Every protected page checks the session itself.
export default async function AccountDetails() {
  const session = await auth();
  if (!session) redirect(`/signin?callbackUrl=${encodeURIComponent("/account")}`);

  const { name, email, roles } = session.user;
  return (
    <dl className="border-line bg-panel grid grid-cols-[auto_1fr] items-baseline gap-x-8 gap-y-4 rounded-2xl border p-6">
      <dt className={termClass}>Name</dt>
      <dd>{name ?? "-"}</dd>
      <dt className={termClass}>Email</dt>
      <dd>{email ?? "-"}</dd>
      <dt className={termClass}>Roles</dt>
      <dd>{roles.length > 0 ? roles.join(", ") : "none"}</dd>
    </dl>
  );
}
