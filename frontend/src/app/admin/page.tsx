import Link from "next/link";
import { Suspense } from "react";
import Header from "@/components/Header";
import LoadingMessage from "@/components/LoadingMessage";
import { requireAdmin } from "@/lib/admin/guard";

const areas = [
  {
    href: "/admin/movies",
    title: "Movies",
    text: "Add movies, edit their details, schedule and cancel shows.",
  },
  {
    href: "/admin/theaters",
    title: "Theaters",
    text: "Add theaters and the screens inside them.",
  },
] as const;

export default function AdminPage() {
  return (
    <>
      <Header title="Catalog admin" />
      {/* The role check reads the session, so it sits behind a boundary. */}
      <Suspense fallback={<LoadingMessage>Loading...</LoadingMessage>}>
        <AdminOverview />
      </Suspense>
    </>
  );
}

async function AdminOverview() {
  await requireAdmin("/admin");
  return (
    <ul className="grid gap-4 sm:grid-cols-2">
      {areas.map((area) => (
        <li key={area.href}>
          <Link
            href={area.href}
            className="border-line bg-panel hover:border-brand-soft block rounded-3xl border p-6 transition-colors"
          >
            <h2 className="font-display text-2xl font-bold">{area.title}</h2>
            <p className="mt-2 text-zinc-400">{area.text}</p>
          </Link>
        </li>
      ))}
    </ul>
  );
}
