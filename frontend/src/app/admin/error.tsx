"use client"; // Error boundaries must be Client Components

import ErrorPanel from "@/components/ErrorPanel";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

export default function AdminError({ error, reset }: ErrorProps) {
  return <ErrorPanel error={error} reset={reset} subject="the catalog" contained={false} />;
}
