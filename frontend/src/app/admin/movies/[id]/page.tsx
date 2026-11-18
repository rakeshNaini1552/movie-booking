import { notFound } from "next/navigation";
import { Suspense } from "react";
import ActionForm from "@/components/admin/ActionForm";
import AdminSection from "@/components/admin/AdminSection";
import MovieFields from "@/components/admin/MovieFields";
import BackLink from "@/components/BackLink";
import ButtonLink from "@/components/ButtonLink";
import LoadingMessage from "@/components/LoadingMessage";
import StatusBadge from "@/components/StatusBadge";
import { cancelShowAction, saveMovieAction } from "@/lib/admin/actions";
import { getAdminMovie } from "@/lib/admin/api";
import { requireAdmin } from "@/lib/admin/guard";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { getMovie } from "@/lib/catalog/movies";
import { idSchema } from "@/lib/catalog/schemas";
import { formatPrice, formatShowTime } from "@/lib/format";

export default function AdminMoviePage(props: PageProps<"/admin/movies/[id]">) {
  return (
    <>
      <BackLink href="/admin/movies">All movies</BackLink>
      <Suspense fallback={<LoadingMessage>Loading movie...</LoadingMessage>}>
        <AdminMovie {...props} />
      </Suspense>
    </>
  );
}

async function AdminMovie({ params, searchParams }: PageProps<"/admin/movies/[id]">) {
  const { id } = await params;
  if (!idSchema.safeParse(id).success) notFound();
  const here = `/admin/movies/${id}`;
  await requireAdmin(here);

  // REST gives the editable fields (including status). GraphQL gives the shows with their
  // screen and theater names in the same call, which REST would need one call per show for.
  const [movie, withShows] = await requireSignIn(here, () =>
    Promise.all([getAdminMovie(id), getMovie(id)]),
  );
  if (!movie) notFound();
  const shows = (withShows?.shows ?? []).toSorted((a, b) => a.startsAt.localeCompare(b.startsAt));
  const saved = (await searchParams).saved === "1";

  return (
    <div className="flex flex-col gap-6">
      <h1 className="font-display text-4xl font-bold tracking-tight text-balance">{movie.title}</h1>
      {saved && (
        <p role="status" className="text-sm text-emerald-300">
          Saved.
        </p>
      )}

      <AdminSection id="movie-details" title="Details">
        <ActionForm
          action={saveMovieAction.bind(null, movie.id)}
          submitLabel="Save changes"
          pendingLabel="Saving..."
        >
          <MovieFields movie={movie} />
        </ActionForm>
      </AdminSection>

      <AdminSection
        id="movie-shows"
        title="Shows"
        action={
          <ButtonLink href={`/admin/movies/${movie.id}/shows/new`} variant="secondary" size="sm">
            Add show
          </ButtonLink>
        }
      >
        {shows.length === 0 ? (
          <p className="text-zinc-300">No shows are scheduled for this movie yet.</p>
        ) : (
          <ul className="divide-line divide-y">
            {shows.map((show) => (
              <li key={show.id} className="flex flex-wrap items-center justify-between gap-4 py-3">
                <div>
                  <p className="font-medium">
                    <time dateTime={show.startsAt}>{formatShowTime(show.startsAt)}</time>
                  </p>
                  <p className="text-sm text-zinc-400">
                    {show.screen.theater.name}, {show.screen.theater.city} · {show.screen.name} ·
                    from {formatPrice(show.basePrice)}
                  </p>
                </div>
                {show.status === "CANCELLED" ? (
                  <StatusBadge label="Cancelled" tone="zinc" />
                ) : (
                  <ActionForm
                    action={cancelShowAction.bind(null, movie.id, show.id)}
                    submitLabel="Cancel show"
                    pendingLabel="Cancelling..."
                    submitVariant="danger"
                    className="flex flex-col items-end gap-2"
                  />
                )}
              </li>
            ))}
          </ul>
        )}
      </AdminSection>
    </div>
  );
}
