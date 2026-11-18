import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import ActionForm, { Field } from "@/components/admin/ActionForm";
import AdminSection from "@/components/admin/AdminSection";
import BackLink from "@/components/BackLink";
import ButtonLink from "@/components/ButtonLink";
import LoadingMessage from "@/components/LoadingMessage";
import { createShowAction } from "@/lib/admin/actions";
import { getAdminMovie, getTheater, listAllTheaters, listScreens } from "@/lib/admin/api";
import { requireAdmin } from "@/lib/admin/guard";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { idSchema } from "@/lib/catalog/schemas";

export default function NewShowPage(props: PageProps<"/admin/movies/[id]/shows/new">) {
  return (
    <Suspense fallback={<LoadingMessage>Loading...</LoadingMessage>}>
      <NewShow {...props} />
    </Suspense>
  );
}

// Two steps on one page. Step 1 picks a theater (a link that adds ?theaterId= to the address).
// Step 2 shows the form with that theater's screens.
async function NewShow({ params, searchParams }: PageProps<"/admin/movies/[id]/shows/new">) {
  const { id } = await params;
  if (!idSchema.safeParse(id).success) notFound();
  const here = `/admin/movies/${id}/shows/new`;
  await requireAdmin(here);

  const rawTheater = (await searchParams).theaterId;
  const theaterId = idSchema.safeParse(Array.isArray(rawTheater) ? rawTheater[0] : rawTheater);

  const movie = await requireSignIn(here, () => getAdminMovie(id));
  if (!movie) notFound();
  const back = <BackLink href={`/admin/movies/${movie.id}`}>Back to {movie.title}</BackLink>;

  if (!theaterId.success) {
    const theaters = await requireSignIn(here, listAllTheaters);
    return (
      <>
        {back}
        <AdminSection id="pick-theater" title={`New show for ${movie.title}: pick a theater`}>
          {theaters.length === 0 ? (
            <div className="flex flex-col items-start gap-4">
              <p className="text-zinc-300">There are no theaters yet. Add one first.</p>
              <ButtonLink href="/admin/theaters">Go to theaters</ButtonLink>
            </div>
          ) : (
            <ul className="grid gap-3 sm:grid-cols-2">
              {theaters.map((theater) => (
                <li key={theater.id}>
                  <Link
                    href={`${here}?theaterId=${theater.id}`}
                    className="border-line bg-raised hover:border-brand-soft block rounded-2xl border p-4 transition-colors"
                  >
                    <span className="block font-semibold">{theater.name}</span>
                    <span className="block text-sm text-zinc-400">{theater.city}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </AdminSection>
      </>
    );
  }

  const [theater, screens] = await requireSignIn(here, () =>
    Promise.all([getTheater(theaterId.data), listScreens(theaterId.data)]),
  );
  if (!theater) notFound();

  return (
    <>
      {back}
      <AdminSection id="new-show" title={`New show for ${movie.title} at ${theater.name}`}>
        {screens.length === 0 ? (
          <div className="flex flex-col items-start gap-4">
            <p className="text-zinc-300">This theater has no screens yet. Add one first.</p>
            <ButtonLink href={`/admin/theaters/${theater.id}`}>Add a screen</ButtonLink>
          </div>
        ) : (
          <ActionForm
            action={createShowAction.bind(null, movie.id)}
            submitLabel="Add show"
            pendingLabel="Adding..."
          >
            <Field
              name="screenId"
              label="Screen"
              as="select"
              required
              options={screens.map((screen) => ({
                value: screen.id,
                label: `${screen.name} (${screen.seatRows * screen.seatsPerRow} seats)`,
              }))}
              defaultValue={screens[0].id}
            />
            <Field
              name="startsAt"
              label="Starts at (UTC)"
              type="datetime-local"
              required
              hint={`The show ends ${movie.durationMinutes} minutes later, when the movie does.`}
            />
            <Field name="basePrice" label="Base price" type="number" step="0.01" min="0" required />
          </ActionForm>
        )}
      </AdminSection>
    </>
  );
}
