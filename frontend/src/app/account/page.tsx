import { Suspense } from "react";
import Header from "@/components/Header";
import PageContainer from "@/components/PageContainer";
import AccountDetails from "./account-details";

export default function AccountPage() {
  return (
    <PageContainer>
      <Header title="Account" />
      {/* Reads the session at request time, so it streams in behind a boundary. */}
      <Suspense fallback={<p role="status">Loading account...</p>}>
        <AccountDetails />
      </Suspense>
    </PageContainer>
  );
}
