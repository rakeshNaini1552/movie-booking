import Link from "next/link";
import type { ReactNode } from "react";

export default function BackLink({ href, children }: { href: string; children: ReactNode }) {
  return (
    <Link
      href={href}
      className="mb-6 inline-flex items-center gap-1.5 rounded-md text-sm font-medium text-zinc-400 transition-colors hover:text-white"
    >
      <span aria-hidden>←</span>
      {children}
    </Link>
  );
}
