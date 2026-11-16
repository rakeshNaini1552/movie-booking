"use server";

import { redirect } from "next/navigation";
import { signIn, signOut } from "@/auth";
import { env } from "@/lib/env";
import { buildLogoutUrl, safeCallbackPath } from "./urls";

export async function signInAction(formData: FormData): Promise<void> {
  // The field is user-controlled (it comes from a query string), so it is re-checked here.
  await signIn("keycloak", { redirectTo: safeCallbackPath(formData.get("callbackUrl")) });
}

export async function signOutAction(): Promise<void> {
  // Clear our cookie first, then end the Keycloak SSO session too. Without the second step
  // the next "Sign in" would log straight back in without asking for a password.
  await signOut({ redirect: false });
  redirect(
    buildLogoutUrl(env.KEYCLOAK_ISSUER_URI, env.AUTH_KEYCLOAK_ID, new URL("/", env.AUTH_URL).href),
  );
}
