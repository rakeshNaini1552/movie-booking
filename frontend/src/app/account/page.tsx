import type { Metadata } from "next";
import { Suspense } from "react";
import Header from "@/components/Header";
import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";
import AccountDetails from "./account-details";

export const metadata: Metadata = { title: "Account" };

export default function AccountPage() {
  return (
    <PageContainer width="narrow">
      <Header title="Account" />
      {/* Reads the session at request time, so it streams in behind a boundary. */}
      <Suspense fallback={<LoadingMessage>Loading account...</LoadingMessage>}>
        <AccountDetails />
      </Suspense>
    </PageContainer>
  );
}
