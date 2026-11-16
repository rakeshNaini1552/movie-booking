import Link from "next/link";
import { Suspense } from "react";
import AuthStatus from "@/components/AuthStatus";

const linkClass =
  "rounded-md px-2 py-1 text-sm font-medium hover:bg-zinc-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 dark:hover:bg-zinc-800 dark:focus-visible:outline-indigo-400";

export default function Navbar() {
  return (
    <header className="border-b border-zinc-300 dark:border-zinc-700">
      <nav
        aria-label="Main"
        className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3 sm:px-6"
      >
        <div className="flex items-center gap-4">
          <Link href="/" className="text-lg font-bold">
            Movie Booking
          </Link>
          <Link href="/" className={linkClass}>
            Home
          </Link>
          <Link href="/movies" className={linkClass}>
            Movies
          </Link>
        </div>
        {/* AuthStatus reads the session, so it stays behind its own Suspense boundary. */}
        <Suspense fallback={null}>
          <AuthStatus />
        </Suspense>
      </nav>
    </header>
  );
}
