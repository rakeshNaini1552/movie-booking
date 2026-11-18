import { notFound } from "next/navigation";
import ButtonLink from "@/components/ButtonLink";
import CancelBookingButton from "@/components/CancelBookingButton";
import MoviePoster from "@/components/MoviePoster";
import PaymentPanel from "@/components/PaymentPanel";
import StatusBadge from "@/components/StatusBadge";
import TicketPanel from "@/components/TicketPanel";
import { UNKNOWN_SHOW_TITLE } from "@/components/BookingCard";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { cancelBookingAction, payBookingAction } from "@/lib/bookings/actions";
import { getBooking } from "@/lib/bookings/api";
import { BOOKING_STATUS } from "@/lib/bookings/status";
import { idSchema } from "@/lib/catalog/schemas";
import { getShowContexts } from "@/lib/catalog/show-context";
import { formatPrice, formatShowTime } from "@/lib/format";
import { getTicketByBooking } from "@/lib/tickets/api";

const termClass = "text-xs font-semibold tracking-wider text-zinc-400 uppercase";

export default async function BookingContent({
  params,
}: Pick<PageProps<"/bookings/[bookingId]">, "params">) {
  const { bookingId } = await params;
  // The id comes from the address bar: check its shape before it goes anywhere near the gateway.
  if (!idSchema.safeParse(bookingId).success) notFound();

  const { booking, show, ticket } = await requireSignIn(`/bookings/${bookingId}`, async () => {
    const booking = await getBooking(bookingId);
    if (!booking) return { booking: null, show: undefined, ticket: null };
    // Only a confirmed booking has a ticket, so the ticket call is skipped for every other status.
    const [contexts, ticket] = await Promise.all([
      getShowContexts([booking.showId]),
      booking.status === "CONFIRMED" ? getTicketByBooking(bookingId) : null,
    ]);
    return { booking, show: contexts.get(booking.showId), ticket };
  });
  // Also the answer for someone else's booking: the service reports it as not found.
  if (!booking) notFound();

  const status = BOOKING_STATUS[booking.status];
  const title = show?.movieTitle ?? UNKNOWN_SHOW_TITLE;
  const total = formatPrice(booking.totalAmount);
  // A scanned ticket means the customer went in: booking-service refuses to cancel it.
  const cancellable =
    booking.status === "PENDING" || (booking.status === "CONFIRMED" && ticket?.status !== "USED");
  const ended = ["FAILED", "EXPIRED", "CANCELLED"].includes(booking.status);

  return (
    <div className="animate-rise grid gap-6 lg:grid-cols-[1fr_22rem] lg:items-start">
      <section
        aria-labelledby="booking-heading"
        className="border-line bg-panel flex flex-col gap-6 rounded-3xl border p-6 sm:flex-row"
      >
        <MoviePoster title={title} className="mx-auto w-40 shrink-0 sm:mx-0" />
        <div className="flex min-w-0 flex-1 flex-col gap-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <h1 id="booking-heading" className="font-display text-3xl font-bold text-balance">
              {title}
            </h1>
            <StatusBadge label={status.label} tone={status.tone} />
          </div>
          <p className="text-zinc-300">{status.text}</p>
          <dl className="grid grid-cols-2 gap-x-6 gap-y-4">
            {show && (
              <>
                <div>
                  <dt className={termClass}>When</dt>
                  <dd className="mt-1">
                    <time dateTime={show.startsAt}>{formatShowTime(show.startsAt)}</time>
                  </dd>
                </div>
                <div>
                  <dt className={termClass}>Where</dt>
                  <dd className="mt-1">
                    {show.theaterName}, {show.city} · {show.screenName}
                  </dd>
                </div>
              </>
            )}
            <div>
              <dt className={termClass}>Seats</dt>
              <dd className="mt-1 font-medium">{booking.seatIds.join(", ")}</dd>
            </div>
            <div>
              <dt className={termClass}>Total</dt>
              <dd className="mt-1 font-medium">{total}</dd>
            </div>
          </dl>
          <p className="font-mono text-xs break-all text-zinc-400">Booking {booking.bookingId}</p>
          <div className="mt-auto flex flex-wrap gap-3">
            {cancellable && (
              <CancelBookingButton bookingId={booking.bookingId} onCancel={cancelBookingAction} />
            )}
            {ended && (
              <ButtonLink href={show ? `/movies/${show.movieId}` : "/movies"}>
                Book again
              </ButtonLink>
            )}
          </div>
        </div>
      </section>

      {booking.status === "PENDING" && (
        <PaymentPanel bookingId={booking.bookingId} total={total} onPay={payBookingAction} />
      )}
      {booking.status === "CONFIRMED" && <TicketPanel ticket={ticket} />}
    </div>
  );
}
