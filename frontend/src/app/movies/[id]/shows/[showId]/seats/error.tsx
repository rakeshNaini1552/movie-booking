"use client"; // Error boundaries must be Client Components

import Button from "@/components/Button";
import PageContainer from "@/components/PageContainer";
import { gatewayErrorMessage, isGatewayDigest } from "@/lib/gateway/errors";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

// Same rules as the movies error page: the kind comes from the digest, the cause stays in the server log.
export default function SeatsError({ error, reset }: ErrorProps) {
  return (
    <PageContainer>
      <div role="alert" className="flex flex-col items-start gap-4">
        <p>{gatewayErrorMessage(error.digest, "the seat map")}</p>
        {error.digest && !isGatewayDigest(error.digest) && (
          <p className="text-sm text-zinc-700 dark:text-zinc-300">Reference: {error.digest}</p>
        )}
        <Button onClick={reset}>Try again</Button>
      </div>
    </PageContainer>
  );
}
