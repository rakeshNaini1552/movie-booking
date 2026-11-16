import type { ReactNode } from "react";

// Shared by the route loading.tsx files and the Suspense fallbacks inside pages.
export default function LoadingMessage({ children }: { children: ReactNode }) {
  return (
    <p role="status" className="text-zinc-700 dark:text-zinc-300">
      {children}
    </p>
  );
}
