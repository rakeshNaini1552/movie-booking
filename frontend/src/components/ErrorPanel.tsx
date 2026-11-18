"use client";

import Button from "@/components/Button";
import PageContainer from "@/components/PageContainer";
import { gatewayErrorMessage, isGatewayDigest } from "@/lib/gateway/errors";

type ErrorPanelProps = {
  error: Error & { digest?: string };
  reset: () => void;
  // Names what failed to load, for example "movies" or "the seat map".
  subject: string;
  // False when a layout already provides the page container (the admin area).
  contained?: boolean;
};

// The body of every error.tsx. The cause stays in the server log. A gateway failure carries its
// kind in the digest, which picks the message; any other error's digest is a log reference.
export default function ErrorPanel({ error, reset, subject, contained = true }: ErrorPanelProps) {
  const panel = (
    <div
      role="alert"
      className="border-line bg-panel flex flex-col items-start gap-4 rounded-2xl border p-8"
    >
      <h1 className="font-display text-2xl font-bold">That did not work</h1>
      <p className="text-zinc-300">{gatewayErrorMessage(error.digest, subject)}</p>
      {error.digest && !isGatewayDigest(error.digest) && (
        <p className="text-sm text-zinc-400">Reference: {error.digest}</p>
      )}
      <Button onClick={reset}>Try again</Button>
    </div>
  );
  return contained ? <PageContainer width="narrow">{panel}</PageContainer> : panel;
}
