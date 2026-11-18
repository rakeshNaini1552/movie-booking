"use client"; // Error boundaries must be Client Components

import ErrorPanel from "@/components/ErrorPanel";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

// Covers /bookings and /bookings/[bookingId].
export default function BookingsError({ error, reset }: ErrorProps) {
  return <ErrorPanel error={error} reset={reset} subject="your bookings" />;
}
