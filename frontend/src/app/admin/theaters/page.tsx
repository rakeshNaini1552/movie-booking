import Link from "next/link";
import { Suspense } from "react";
import { z } from "zod";
import ActionForm, { Field } from "@/components/admin/ActionForm";
import AdminSection from "@/components/admin/AdminSection";
import Header from "@/components/Header";
import LoadingMessage from "@/components/LoadingMessage";
import Pagination from "@/components/Pagination";
import { createTheaterAction } from "@/lib/admin/actions";
import { listTheaters } from "@/lib/admin/api";
import { requireAdmin } from "@/lib/admin/guard";
import { requireSignIn } from "@/lib/auth/require-sign-in";

const pageSchema = z.coerce.number().int().min(0).max(10_000).catch(0);
const pageHref = (page: number) => (page > 0 ? `/admin/theaters?page=${page}` : "/admin/theaters");

export default function AdminTheatersPage({ searchParams }: PageProps<"/admin/theaters">) {
  return (
    <>
      <Header title="Theaters" />
      <Suspense fallback={<LoadingMessage>Loading theaters...</LoadingMessage>}>
        <AdminTheaters searchParams={searchParams} />
      </Suspense>
    </>
  );
}

async function AdminTheaters({ searchParams }: Pick<PageProps<"/admin/theaters">, "searchParams">) {
  const raw = (await searchParams).page;
  const page = pageSchema.parse(Array.isArray(raw) ? raw[0] : (raw ?? 0));
  const here = pageHref(page);
  await requireAdmin(here);
  const theaters = await requireSignIn(here, () => listTheaters(page));

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_24rem] lg:items-start">
      <AdminSection id="theater-list" title="All theaters">
        {theaters.content.length === 0 ? (
          <p className="text-zinc-300">There are no theaters yet.</p>
        ) : (
          <ul className="divide-line divide-y">
            {theaters.content.map((theater) => (
              <li key={theater.id} className="py-3">
                <Link
                  href={`/admin/theaters/${theater.id}`}
                  className="rounded-md font-medium hover:underline"
                >
                  {theater.name}
                </Link>
                <p className="text-sm text-zinc-400">
                  {theater.city} · {theater.address}
                </p>
              </li>
            ))}
          </ul>
        )}
        <Pagination
          previousHref={page > 0 ? pageHref(page - 1) : null}
          nextHref={page + 1 < theaters.totalPages ? pageHref(page + 1) : null}
        />
      </AdminSection>

      <AdminSection id="new-theater" title="Add a theater">
        <ActionForm action={createTheaterAction} submitLabel="Add theater" pendingLabel="Adding...">
          <Field name="name" label="Name" required />
          <Field name="city" label="City" required />
          <Field name="address" label="Address" required />
        </ActionForm>
      </AdminSection>
    </div>
  );
}
