"use client"; // Error boundaries must be Client Components

type ErrorProps = {
  error: Error & { digest?: string };
  reset: () => void;
};

export default function MoviesError({ reset }: ErrorProps) {
  return (
    <div role="alert">
      <p>Something went wrong while loading movies.</p>
      <button onClick={reset}>Try again</button>
    </div>
  );
}
