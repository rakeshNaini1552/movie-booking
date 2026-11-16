"use client"; // Error boundaries must be Client Components

import Button from "@/components/Button";
import PageContainer from "@/components/PageContainer";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

// Covers /movies and /movies/[id]. The cause stays in the server log: the page only says what
// the user can do, plus the digest that links this screen to that log line.
export default function MoviesError({ error, reset }: ErrorProps) {
  return (
    <PageContainer>
      <div role="alert" className="flex flex-col items-start gap-4">
        <p>
          Something went wrong while loading movies. The catalog may be temporarily unavailable, so
          please try again in a moment.
        </p>
        {error.digest && (
          <p className="text-sm text-zinc-700 dark:text-zinc-300">Reference: {error.digest}</p>
        )}
        <Button onClick={reset}>Try again</Button>
      </div>
    </PageContainer>
  );
}
