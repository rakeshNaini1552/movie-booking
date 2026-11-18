import type { Metadata } from "next";
import { Suspense } from "react";
import Header from "@/components/Header";
import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";
import BookingsContent from "./bookings-content";

export const metadata: Metadata = { title: "My bookings" };

export default function BookingsPage({ searchParams }: PageProps<"/bookings">) {
  return (
    <PageContainer>
      <Header title="My bookings" eyebrow="Your tickets" />
      {/* searchParams and the session are request data: read behind a boundary. */}
      <Suspense fallback={<LoadingMessage>Loading your bookings...</LoadingMessage>}>
        <BookingsContent searchParams={searchParams} />
      </Suspense>
    </PageContainer>
  );
}
