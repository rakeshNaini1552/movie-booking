import Button from "@/components/Button";

type AuthNavProps = {
  user: { name?: string | null } | null;
  signInAction: (formData: FormData) => void | Promise<void>;
  signOutAction: () => void | Promise<void>;
};

// Presentational on purpose: the session read and the server actions are passed in by AuthStatus.
export default function AuthNav({ user, signInAction, signOutAction }: AuthNavProps) {
  if (!user) {
    return (
      <section aria-label="Account">
        <form action={signInAction}>
          <Button type="submit">Sign in</Button>
        </form>
      </section>
    );
  }

  return (
    <section aria-label="Account" className="flex items-center gap-3">
      <p className="text-sm">
        Signed in as <strong>{user.name ?? "unknown user"}</strong>
      </p>
      <form action={signOutAction}>
        <Button type="submit" variant="secondary">
          Sign out
        </Button>
      </form>
    </section>
  );
}
