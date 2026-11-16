import { redirect } from "next/navigation";
import { NotSignedInError } from "@/lib/catalog/errors";

// Runs a catalog call and sends a signed-out user (no session, or a token the gateway rejected)
// to /signin, returning to `returnTo` afterwards. Other errors go to the nearest error.tsx.
// redirect() throws, so it is called outside the try/catch.
export async function requireSignIn<T>(returnTo: string, call: () => Promise<T>): Promise<T> {
  let signedOut = false;
  let result: T | undefined;
  try {
    result = await call();
  } catch (error) {
    if (!(error instanceof NotSignedInError)) throw error;
    signedOut = true;
  }
  if (signedOut) redirect(`/signin?callbackUrl=${encodeURIComponent(returnTo)}`);
  return result as T;
}
