"use client"; // Error boundaries must be Client Components

import Button from "@/components/Button";
import PageContainer from "@/components/PageContainer";

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

export default function MoviesError({ reset }: ErrorProps) {
  return (
    <PageContainer>
      <div role="alert" className="flex flex-col items-start gap-4">
        <p>Something went wrong while loading movies.</p>
        <Button onClick={reset}>Try again</Button>
      </div>
    </PageContainer>
  );
}
