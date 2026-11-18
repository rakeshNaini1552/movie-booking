import Link from "next/link";
import { Suspense } from "react";
import AuthStatus from "@/components/AuthStatus";
import Logo from "@/components/Logo";
import { navLinkClass } from "@/lib/ui/nav-styles";

export default function Navbar() {
  return (
    <header className="border-line/70 bg-night/80 sticky top-0 z-40 border-b backdrop-blur-md">
      <nav
        aria-label="Main"
        className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3 sm:px-6"
      >
        <div className="flex items-center gap-2">
          <Link href="/" aria-label="Movie Booking home" className="mr-3 rounded-md">
            <Logo />
          </Link>
          <Link href="/movies" className={navLinkClass}>
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
