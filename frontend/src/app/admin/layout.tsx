import type { Metadata } from "next";
import Link from "next/link";
import PageContainer from "@/components/PageContainer";
import { navLinkClass } from "@/lib/ui/nav-styles";

export const metadata: Metadata = { title: "Admin" };

// Shared frame of every /admin page: a second row of navigation. It reads no request data, so
// it needs no Suspense; each page checks the admin role itself (requireAdmin).
export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    <PageContainer>
      <nav
        aria-label="Admin"
        className="border-line mb-8 flex flex-wrap items-center gap-1 border-b pb-4"
      >
        <span className="text-brand-soft mr-3 text-xs font-semibold tracking-[0.2em] uppercase">
          Admin
        </span>
        <Link href="/admin" className={navLinkClass}>
          Overview
        </Link>
        <Link href="/admin/movies" className={navLinkClass}>
          Movies
        </Link>
        <Link href="/admin/theaters" className={navLinkClass}>
          Theaters
        </Link>
      </nav>
      {children}
    </PageContainer>
  );
}
