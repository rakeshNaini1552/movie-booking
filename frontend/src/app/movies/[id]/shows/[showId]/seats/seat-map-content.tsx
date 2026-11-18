import { notFound } from "next/navigation";
import BackLink from "@/components/BackLink";
import ButtonLink from "@/components/ButtonLink";
import SeatPicker from "@/components/SeatPicker";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { createBookingAction } from "@/lib/bookings/actions";
import { getMovie } from "@/lib/catalog/movies";
import { idSchema } from "@/lib/catalog/schemas";
import { formatShowTime } from "@/lib/format";
import { getSeatMap } from "@/lib/seats/seat-map";

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

  // A server action defined inline: it runs on the server when the seat picker calls onBook.
  // The movie and show ids come from this page, the seats and the key from the picker; the
  // action validates all four again (see createBookingAction).
  async function book(seatIds: readonly string[], idempotencyKey: string) {
    "use server";
    const result = await createBookingAction({ movieId: id, showId, seatIds, idempotencyKey });
    return "error" in result ? { error: result.error } : undefined;
  }

  const { screen } = show;
  return (
    <>
      <BackLink href={`/movies/${movie.id}`}>Back to {movie.title}</BackLink>
      <div className="mb-8">
        <p className="text-brand-soft mb-2 text-xs font-semibold tracking-[0.2em] uppercase">
          Select seats
        </p>
        <h1 className="font-display text-4xl font-bold tracking-tight text-balance sm:text-5xl">
          {movie.title}
        </h1>
        <p className="mt-3 text-zinc-300">
          <time dateTime={show.startsAt}>{formatShowTime(show.startsAt)}</time>
          <span aria-hidden> · </span>
          {screen.theater.name}, {screen.theater.city}
          <span aria-hidden> · </span>
          {screen.name}
        </p>
      </div>
      {seats === null ? (
        <div className="border-line bg-panel flex flex-col items-start gap-4 rounded-2xl border p-6">
          <p>Seats are not available for this show yet.</p>
          <ButtonLink href={`/movies/${movie.id}`} variant="secondary">
            Back to the show list
          </ButtonLink>
        </div>
      ) : (
        <SeatPicker seats={seats} onBook={book} />
      )}
    </>
  );
}
