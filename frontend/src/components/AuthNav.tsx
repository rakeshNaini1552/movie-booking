import Link from "next/link";
import Button from "@/components/Button";
import { navLinkClass } from "@/lib/ui/nav-styles";

type AuthNavProps = {
  user: { name?: string | null; isAdmin?: boolean } | null;
  signInAction: (formData: FormData) => void | Promise<void>;
  signOutAction: () => void | Promise<void>;
};

// Presentational on purpose: the session read and the server actions are passed in by AuthStatus.
export default function AuthNav({ user, signInAction, signOutAction }: AuthNavProps) {
  if (!user) {
    return (
      <section aria-label="Account">
        <form action={signInAction}>
          <Button type="submit" size="sm">
            Sign in
          </Button>
        </form>
      </section>
    );
  }

  const name = user.name ?? "unknown user";
  return (
    <section aria-label="Account" className="flex flex-wrap items-center gap-1">
      <Link href="/bookings" className={navLinkClass}>
        My bookings
      </Link>
      {user.isAdmin && (
        <Link href="/admin" className={navLinkClass}>
          Admin
        </Link>
      )}
      <Link href="/account" className={`${navLinkClass} flex items-center gap-2`}>
        <span
          aria-hidden
          className="bg-brand flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold text-white uppercase"
        >
          {name.charAt(0)}
        </span>
        <span>
          <span className="sr-only">Signed in as </span>
          <strong className="font-medium">{name}</strong>
        </span>
      </Link>
      <form action={signOutAction}>
        <Button type="submit" variant="secondary" size="sm">
          Sign out
        </Button>
      </form>
    </section>
  );
}
