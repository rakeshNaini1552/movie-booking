import type { Metadata } from "next";
import { Suspense } from "react";
import BackLink from "@/components/BackLink";
import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";
import BookingContent from "./booking-content";

export const metadata: Metadata = { title: "Booking" };

export default function BookingPage({ params }: PageProps<"/bookings/[bookingId]">) {
  return (
    <PageContainer>
      <BackLink href="/bookings">My bookings</BackLink>
      {/* params and the session are request data: read behind a boundary. */}
      <Suspense fallback={<LoadingMessage>Loading booking...</LoadingMessage>}>
        <BookingContent params={params} />
      </Suspense>
    </PageContainer>
  );
}
