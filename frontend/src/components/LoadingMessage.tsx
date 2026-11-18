import type { ReactNode } from "react";

// Shared by the route loading.tsx files and the Suspense fallbacks inside pages.
export default function LoadingMessage({ children }: { children: ReactNode }) {
  return (
    <p role="status" className="flex items-center gap-3 text-zinc-400">
      <span
        aria-hidden
        className="border-line border-t-brand-soft inline-block h-4 w-4 animate-spin rounded-full border-2"
      />
      {children}
    </p>
  );
}
