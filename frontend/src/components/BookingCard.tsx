import Link from "next/link";
import MoviePoster from "@/components/MoviePoster";
import StatusBadge from "@/components/StatusBadge";
import type { BookingSummary } from "@/lib/bookings/schemas";
import { BOOKING_STATUS } from "@/lib/bookings/status";
import type { ShowContext } from "@/lib/catalog/show-context";
import { formatDateTime, formatPrice, formatShowTime } from "@/lib/format";

type BookingCardProps = {
  booking: BookingSummary;
  // Missing when the catalog could not tell us about the show: the booking is still listed.
  show?: ShowContext;
};

export const UNKNOWN_SHOW_TITLE = "Show details unavailable";

export default function BookingCard({ booking, show }: BookingCardProps) {
  const status = BOOKING_STATUS[booking.status];
  const title = show?.movieTitle ?? UNKNOWN_SHOW_TITLE;
  return (
    <article className="border-line bg-panel relative flex gap-5 rounded-2xl border p-4 transition-colors hover:border-zinc-500">
      <MoviePoster title={title} compact className="hidden w-20 shrink-0 sm:flex" />
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h2 className="font-display text-xl font-bold">
            {/* The link covers the whole card, like the movie cards. */}
            <Link
              href={`/bookings/${booking.bookingId}`}
              className="rounded-md after:absolute after:inset-0"
            >
              {title}
            </Link>
          </h2>
          <StatusBadge label={status.label} tone={status.tone} />
        </div>
        {show && (
          <p className="text-sm text-zinc-300">
            <time dateTime={show.startsAt}>{formatShowTime(show.startsAt)}</time>
            <span aria-hidden> · </span>
            {show.theaterName}, {show.city}
            <span aria-hidden> · </span>
            {show.screenName}
          </p>
        )}
        <dl className="mt-auto flex flex-wrap gap-x-8 gap-y-1 text-sm">
          <div className="flex gap-2">
            <dt className="text-zinc-400">Seats</dt>
            <dd className="font-medium">{booking.seatIds.join(", ")}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="text-zinc-400">Total</dt>
            <dd className="font-medium">{formatPrice(booking.totalAmount)}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="text-zinc-400">Booked</dt>
            <dd>
              <time dateTime={booking.createdAt}>{formatDateTime(booking.createdAt)}</time>
            </dd>
          </div>
        </dl>
      </div>
    </article>
  );
}
