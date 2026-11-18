"use client"; // Error boundaries must be Client Components

import ErrorPanel from "@/components/ErrorPanel";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

// Covers /movies and /movies/[id].
export default function MoviesError({ error, reset }: ErrorProps) {
  return <ErrorPanel error={error} reset={reset} subject="movies" />;
}
