import { notFound } from "next/navigation";
import { Suspense } from "react";
import ActionForm, { Field } from "@/components/admin/ActionForm";
import AdminSection from "@/components/admin/AdminSection";
import BackLink from "@/components/BackLink";
import LoadingMessage from "@/components/LoadingMessage";
import { createScreenAction, deleteTheaterAction } from "@/lib/admin/actions";
import { getTheater, listScreens } from "@/lib/admin/api";
import { requireAdmin } from "@/lib/admin/guard";
import { LIMITS } from "@/lib/admin/schemas";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { idSchema } from "@/lib/catalog/schemas";

export default function AdminTheaterPage({ params }: PageProps<"/admin/theaters/[id]">) {
  return (
    <>
      <BackLink href="/admin/theaters">All theaters</BackLink>
      <Suspense fallback={<LoadingMessage>Loading theater...</LoadingMessage>}>
        <AdminTheater params={params} />
      </Suspense>
    </>
  );
}

async function AdminTheater({ params }: Pick<PageProps<"/admin/theaters/[id]">, "params">) {
  const { id } = await params;
  if (!idSchema.safeParse(id).success) notFound();
  const here = `/admin/theaters/${id}`;
  await requireAdmin(here);

  const [theater, screens] = await requireSignIn(here, () =>
    Promise.all([getTheater(id), listScreens(id).catch(() => [])]),
  );
  if (!theater) notFound();

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-4xl font-bold tracking-tight text-balance">
          {theater.name}
        </h1>
        <p className="mt-2 text-zinc-300">
          {theater.city} · {theater.address}
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_24rem] lg:items-start">
        <AdminSection id="screens" title="Screens">
          {screens.length === 0 ? (
            <p className="text-zinc-300">This theater has no screens yet.</p>
          ) : (
            <ul className="divide-line divide-y">
              {screens.map((screen) => (
                <li key={screen.id} className="flex flex-wrap justify-between gap-3 py-3">
                  <span className="font-medium">{screen.name}</span>
                  <span className="text-sm text-zinc-400">
                    {screen.seatRows} rows of {screen.seatsPerRow} ·{" "}
                    {screen.seatRows * screen.seatsPerRow} seats
                  </span>
                </li>
              ))}
            </ul>
          )}
        </AdminSection>

        <AdminSection id="new-screen" title="Add a screen">
          <ActionForm
            action={createScreenAction.bind(null, theater.id)}
            submitLabel="Add screen"
            pendingLabel="Adding..."
          >
            <Field name="name" label="Name" required hint="For example Screen 1." />
            <Field
              name="seatRows"
              label="Rows"
              type="number"
              min="1"
              max={String(LIMITS.maxSeatRows)}
              required
            />
            <Field
              name="seatsPerRow"
              label="Seats per row"
              type="number"
              min="1"
              max={String(LIMITS.maxSeatsPerRow)}
              required
            />
          </ActionForm>
        </AdminSection>
      </div>

      <AdminSection id="delete-theater" title="Delete this theater">
        <p className="mb-4 text-sm text-zinc-400">
          Only possible while the theater has no screens or shows.
        </p>
        <ActionForm
          action={deleteTheaterAction.bind(null, theater.id)}
          submitLabel="Delete theater"
          pendingLabel="Deleting..."
          submitVariant="danger"
        />
      </AdminSection>
    </div>
  );
}
