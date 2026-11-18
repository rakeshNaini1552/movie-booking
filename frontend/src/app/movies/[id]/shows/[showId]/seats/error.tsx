"use client"; // Error boundaries must be Client Components

import ErrorPanel from "@/components/ErrorPanel";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

export default function SeatsError({ error, reset }: ErrorProps) {
  return <ErrorPanel error={error} reset={reset} subject="the seat map" />;
}
