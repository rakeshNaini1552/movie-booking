"use client"; // Error boundaries must be Client Components

import Button from "@/components/Button";
import PageContainer from "@/components/PageContainer";
import { gatewayErrorMessage, isGatewayDigest } from "@/lib/gateway/errors";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

// Covers /movies and /movies/[id]. The cause stays in the server log. A gateway failure carries
// its kind in the digest, which picks the message; any other error's digest is a log reference.
export default function MoviesError({ error, reset }: ErrorProps) {
  return (
    <PageContainer>
      <div role="alert" className="flex flex-col items-start gap-4">
        <p>{gatewayErrorMessage(error.digest)}</p>
        {error.digest && !isGatewayDigest(error.digest) && (
          <p className="text-sm text-zinc-700 dark:text-zinc-300">Reference: {error.digest}</p>
        )}
        <Button onClick={reset}>Try again</Button>
      </div>
    </PageContainer>
  );
}
