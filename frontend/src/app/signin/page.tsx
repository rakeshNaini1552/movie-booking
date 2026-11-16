import { Suspense } from "react";
import Button from "@/components/Button";
import Header from "@/components/Header";
import PageContainer from "@/components/PageContainer";
import { signInAction } from "@/lib/auth/actions";
import { safeCallbackPath } from "@/lib/auth/urls";

export default function SignInPage({ searchParams }: PageProps<"/signin">) {
  return (
    <PageContainer>
      <Header title="Sign in" />
      <Suspense fallback={null}>
        <SignInForm searchParams={searchParams} />
      </Suspense>
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
      <Button type="submit">Sign in with Keycloak</Button>
    </form>
  );
}
