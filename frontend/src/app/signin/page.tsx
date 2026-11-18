import type { Metadata } from "next";
import { Suspense } from "react";
import Button from "@/components/Button";
import PageContainer from "@/components/PageContainer";
import { signInAction } from "@/lib/auth/actions";
import { safeCallbackPath } from "@/lib/auth/urls";

export const metadata: Metadata = { title: "Sign in" };

export default function SignInPage({ searchParams }: PageProps<"/signin">) {
  return (
    <PageContainer width="narrow">
      <div className="border-line bg-panel animate-rise mx-auto mt-10 max-w-md rounded-3xl border p-10 text-center">
        <h1 className="font-display text-3xl font-bold">Sign in</h1>
        <p className="mt-3 mb-8 text-zinc-400">
          Sign in to browse movies, pick seats and see your tickets.
        </p>
        <Suspense fallback={null}>
          <SignInForm searchParams={searchParams} />
        </Suspense>
      </div>
    </PageContainer>
  );
}

// searchParams is request data, so it is read behind a boundary. The value is sanitised here
// and again in the server action, because a hidden field can be edited in the browser.
async function SignInForm({ searchParams }: Pick<PageProps<"/signin">, "searchParams">) {
  const { callbackUrl } = await searchParams;
  const target = safeCallbackPath(Array.isArray(callbackUrl) ? callbackUrl[0] : callbackUrl);
  return (
    <form action={signInAction}>
      <input type="hidden" name="callbackUrl" value={target} />
      <Button type="submit" size="lg" className="w-full">
        Sign in with Keycloak
      </Button>
    </form>
  );
}
