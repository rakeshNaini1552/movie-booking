import { z } from "zod";
import BookingCard from "@/components/BookingCard";
import ButtonLink from "@/components/ButtonLink";
import Pagination from "@/components/Pagination";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { listBookings } from "@/lib/bookings/api";
import { getShowContexts } from "@/lib/catalog/show-context";

// Small on purpose: every booking on the page needs a catalog lookup for its movie and show
// time, and the gateway rate-limits each user (see getShowContexts).
export const BOOKINGS_PAGE_SIZE = 5;
const MAX_PAGE = 10_000;

const pageSchema = z.coerce.number().int().min(0).max(MAX_PAGE).catch(0);
const pageHref = (page: number) => (page > 0 ? `/bookings?page=${page}` : "/bookings");

export default async function BookingsContent({
  searchParams,
}: Pick<PageProps<"/bookings">, "searchParams">) {
  const raw = (await searchParams).page;
  // The page number comes from the address bar: anything invalid falls back to the first page.
  const page = pageSchema.parse(Array.isArray(raw) ? raw[0] : (raw ?? 0));

  const { bookings, contexts } = await requireSignIn(pageHref(page), async () => {
    const bookings = await listBookings(page, BOOKINGS_PAGE_SIZE);
    const contexts = await getShowContexts(bookings.items.map((booking) => booking.showId));
    return { bookings, contexts };
  });

  if (bookings.items.length === 0) {
    return (
      <div className="border-line bg-panel flex flex-col items-start gap-4 rounded-2xl border p-8">
        <p className="text-zinc-300">
          {page > 0 ? "There are no more bookings." : "You have not booked anything yet."}
        </p>
        <ButtonLink href="/movies">Browse movies</ButtonLink>
      </div>
    );
  }

  return (
    <>
      <ul className="flex flex-col gap-4">
        {bookings.items.map((booking) => (
          <li key={booking.bookingId} className="animate-rise">
            <BookingCard booking={booking} show={contexts.get(booking.showId)} />
          </li>
        ))}
      </ul>
      <Pagination
        previousHref={page > 0 ? pageHref(page - 1) : null}
        nextHref={page + 1 < bookings.totalPages ? pageHref(page + 1) : null}
      />
    </>
  );
}
