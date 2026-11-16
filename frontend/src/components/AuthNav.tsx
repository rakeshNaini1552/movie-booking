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
          <button type="submit">Sign in</button>
        </form>
      </section>
    );
  }

  return (
    <section aria-label="Account">
      <p>
        Signed in as <strong>{user.name ?? "unknown user"}</strong>
      </p>
      <form action={signOutAction}>
        <button type="submit">Sign out</button>
      </form>
    </section>
  );
}
