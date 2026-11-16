import Link from "next/link";
import { notFound } from "next/navigation";
import Header from "@/components/Header";
import SeatPicker from "@/components/SeatPicker";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { getMovie } from "@/lib/catalog/movies";
import { idSchema } from "@/lib/catalog/schemas";
import { formatPrice, formatShowTime } from "@/lib/format";
import { getSeatMap } from "@/lib/seats/seat-map";

const linkClass =
  "rounded-md font-medium text-indigo-700 underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 dark:text-indigo-300 dark:focus-visible:outline-indigo-400";

export default async function SeatMapContent({
  params,
}: Pick<PageProps<"/movies/[id]/shows/[showId]/seats">, "params">) {
  const { id, showId } = await params;
  // Both ids come from the address bar: check their shape before anything goes to the gateway.
  if (!idSchema.safeParse(id).success || !idSchema.safeParse(showId).success) notFound();

  const [movie, seats] = await requireSignIn(`/movies/${id}/shows/${showId}/seats`, () =>
    Promise.all([getMovie(id), getSeatMap(showId)]),
  );
  // The show must belong to this movie (and still be on): the seat service alone cannot tell.
  const show = movie?.shows.find((s) => s.id === showId && s.status !== "CANCELLED");
  if (!movie || !show) notFound();

  const { screen } = show;
  return (
    <>
      <Link href={`/movies/${movie.id}`} className={`${linkClass} mb-4 inline-block text-sm`}>
        Back to {movie.title}
      </Link>
      <Header title={`Select seats: ${movie.title}`} />
      <dl className="mb-8 grid max-w-md grid-cols-[auto_1fr] gap-x-6 gap-y-1">
        <dt className="font-medium text-zinc-700 dark:text-zinc-300">Theater</dt>
        <dd>
          {screen.theater.name}, {screen.theater.city}
        </dd>
        <dt className="font-medium text-zinc-700 dark:text-zinc-300">Screen</dt>
        <dd>{screen.name}</dd>
        <dt className="font-medium text-zinc-700 dark:text-zinc-300">Starts</dt>
        <dd>
          <time dateTime={show.startsAt}>{formatShowTime(show.startsAt)}</time>
        </dd>
        <dt className="font-medium text-zinc-700 dark:text-zinc-300">Base price</dt>
        <dd>{formatPrice(show.basePrice)}</dd>
      </dl>
      {seats === null ? (
        <div className="flex flex-col items-start gap-3">
          <p>Seats are not available for this show yet.</p>
          <Link href={`/movies/${movie.id}`} className={linkClass}>
            Back to the show list
          </Link>
        </div>
      ) : (
        <SeatPicker seats={seats} />
      )}
    </>
  );
}
