import { Suspense } from "react";
import Header from "@/components/Header";
import AccountDetails from "./account-details";

export default function AccountPage() {
  return (
    <main>
      <Header title="Account" />
      {/* Reads the session at request time, so it streams in behind a boundary. */}
      <Suspense fallback={<p>Loading account...</p>}>
        <AccountDetails />
      </Suspense>
    </main>
  );
}
