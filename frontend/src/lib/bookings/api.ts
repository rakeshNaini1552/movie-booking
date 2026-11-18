import "server-only";
import { env } from "@/lib/env";
import { GatewayNotFoundError, httpStatusOf } from "@/lib/gateway/errors";
import { gatewayGet, gatewaySend } from "@/lib/gateway/rest";
import {
  type Booking,
  type BookingPage,
  type BookingStatus,
  bookingPageSchema,
  bookingSchema,
  cancelBookingResponseSchema,
  createBookingResponseSchema,
  payBookingResponseSchema,
} from "./schemas";

const BOOKINGS = "/api/v1/bookings";

// Why a write did not happen, in terms a page can explain. Anything not listed (timeouts, 5xx)
// is thrown as a GatewayError and ends up as "unavailable" in the server actions.
export type CreateBookingResult =
  { ok: true; bookingId: string } | { ok: false; reason: "seats-unavailable" | "invalid" };
export type PayBookingResult =
  | { ok: true; status: BookingStatus; paid: boolean }
  | { ok: false; reason: "not-payable" | "not-found" | "invalid" };
export type CancelBookingResult =
  { ok: true; status: BookingStatus } | { ok: false; reason: "not-cancellable" | "not-found" };

// Creates the booking and holds its seats. The idempotency key makes a repeated request (double
// click, retry after a timeout) return the first booking instead of creating a second one.
export async function createBooking(input: {
  showId: string;
  seatIds: readonly string[];
  idempotencyKey: string;
}): Promise<CreateBookingResult> {
  try {
    const { bookingId } = await gatewaySend(
      "bookings",
      BOOKINGS,
      {
        method: "POST",
        body: { showId: input.showId, seatIds: input.seatIds },
        headers: { "idempotency-key": input.idempotencyKey },
        timeoutMs: env.GATEWAY_WRITE_TIMEOUT_MS,
      },
      createBookingResponseSchema,
    );
    return { ok: true, bookingId };
  } catch (error) {
    const status = httpStatusOf(error);
    if (status === 409) return { ok: false, reason: "seats-unavailable" };
    if (status === 400) return { ok: false, reason: "invalid" };
    throw error;
  }
}

// A declined card is not an error: the service answers 200 with paymentStatus FAILED and the
// booking stays PENDING, so the customer can try again.
export async function payBooking(input: {
  bookingId: string;
  paymentToken: string;
  idempotencyKey: string;
}): Promise<PayBookingResult> {
  try {
    const result = await gatewaySend(
      "bookings",
      `${BOOKINGS}/${input.bookingId}/payments`,
      {
        method: "POST",
        body: { paymentToken: input.paymentToken },
        headers: { "idempotency-key": input.idempotencyKey },
        timeoutMs: env.GATEWAY_WRITE_TIMEOUT_MS,
      },
      payBookingResponseSchema,
    );
    return { ok: true, status: result.status, paid: result.paymentStatus === "SUCCESS" };
  } catch (error) {
    const status = httpStatusOf(error);
    if (status === 409) return { ok: false, reason: "not-payable" };
    if (status === 404) return { ok: false, reason: "not-found" };
    if (status === 400) return { ok: false, reason: "invalid" };
    throw error;
  }
}

export async function cancelBooking(bookingId: string): Promise<CancelBookingResult> {
  try {
    const result = await gatewaySend(
      "bookings",
      `${BOOKINGS}/${bookingId}/cancel`,
      { method: "POST" },
      cancelBookingResponseSchema,
    );
    return { ok: true, status: result.status };
  } catch (error) {
    const status = httpStatusOf(error);
    if (status === 409) return { ok: false, reason: "not-cancellable" };
    if (status === 404) return { ok: false, reason: "not-found" };
    throw error;
  }
}

// Returns null when the booking does not exist or belongs to someone else (the service answers
// 404 for both, so one user cannot learn that another user's booking exists).
export async function getBooking(bookingId: string): Promise<Booking | null> {
  try {
    return await gatewayGet("bookings", `${BOOKINGS}/${bookingId}`, bookingSchema);
  } catch (error) {
    if (error instanceof GatewayNotFoundError) return null;
    throw error;
  }
}

// The signed-in user's bookings, newest first. `page` is zero-based.
export async function listBookings(page: number, size: number): Promise<BookingPage> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return gatewayGet("bookings", `${BOOKINGS}?${query}`, bookingPageSchema);
}
