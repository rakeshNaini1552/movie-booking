"use server";

import { redirect } from "next/navigation";
import { z } from "zod";
import { idSchema } from "@/lib/catalog/schemas";
import { NotSignedInError } from "@/lib/gateway/errors";
import { MAX_SEATS } from "@/lib/seats/limits";
import { cancelBooking, createBooking, payBooking } from "./api";
import { PAYMENT_METHOD_IDS, PAYMENT_TOKENS } from "./payment-methods";

// Server actions are public POST endpoints: anyone can call them with any arguments, not only
// our own buttons. So every action validates its input and relies on the gateway (which checks
// the user's token and booking ownership) for authorization.

export type ActionResult = { error: string } | { ok: true };

const UNAVAILABLE = "The booking service is not responding. Please try again in a moment.";

const signInPath = (returnTo: string) => `/signin?callbackUrl=${encodeURIComponent(returnTo)}`;

const createInputSchema = z.object({
  movieId: idSchema,
  showId: idSchema,
  // booking-service limits a seat id to 3 characters ("A12").
  seatIds: z.array(z.string().min(1).max(3)).min(1).max(MAX_SEATS),
  idempotencyKey: z.uuid(),
});

// Holds the seats and creates the booking, then goes to the booking page (where payment happens).
// Returns only when it did not work; on success redirect() ends the action.
export async function createBookingAction(input: unknown): Promise<ActionResult> {
  const parsed = createInputSchema.safeParse(input);
  if (!parsed.success) return { error: "That seat selection is not valid. Please pick again." };
  const { movieId, showId, seatIds, idempotencyKey } = parsed.data;

  let target: string;
  try {
    const result = await createBooking({ showId, seatIds, idempotencyKey });
    if (!result.ok) {
      return {
        error:
          result.reason === "seats-unavailable"
            ? "One or more of those seats was just taken. Refresh the seat map and pick again."
            : "That seat selection is not valid. Please pick again.",
      };
    }
    target = `/bookings/${result.bookingId}`;
  } catch (error) {
    if (!(error instanceof NotSignedInError)) return { error: UNAVAILABLE };
    target = signInPath(`/movies/${movieId}/shows/${showId}/seats`);
  }
  // redirect() works by throwing, so it is called outside the try/catch.
  redirect(target);
}

const payInputSchema = z.object({
  bookingId: idSchema,
  method: z.enum(PAYMENT_METHOD_IDS),
  idempotencyKey: z.uuid(),
});

export async function payBookingAction(input: unknown): Promise<ActionResult> {
  const parsed = payInputSchema.safeParse(input);
  if (!parsed.success) return { error: "That payment request is not valid." };
  const { bookingId, method, idempotencyKey } = parsed.data;

  let signedOut = false;
  try {
    const result = await payBooking({
      bookingId,
      paymentToken: PAYMENT_TOKENS[method],
      idempotencyKey,
    });
    if (!result.ok) {
      return {
        error:
          result.reason === "not-payable"
            ? "This booking can no longer be paid. The seat hold may have expired."
            : result.reason === "not-found"
              ? "We could not find that booking."
              : "That payment request is not valid.",
      };
    }
    if (!result.paid) return { error: "The card was declined. Try another card." };
    return { ok: true };
  } catch (error) {
    if (!(error instanceof NotSignedInError)) return { error: UNAVAILABLE };
    signedOut = true;
  }
  if (signedOut) redirect(signInPath(`/bookings/${bookingId}`));
  return { ok: true };
}

export async function cancelBookingAction(bookingId: unknown): Promise<ActionResult> {
  const parsed = idSchema.safeParse(bookingId);
  if (!parsed.success) return { error: "We could not find that booking." };

  let signedOut = false;
  try {
    const result = await cancelBooking(parsed.data);
    if (!result.ok) {
      return {
        error:
          result.reason === "not-cancellable"
            ? "This booking can no longer be cancelled."
            : "We could not find that booking.",
      };
    }
    return { ok: true };
  } catch (error) {
    if (!(error instanceof NotSignedInError)) return { error: UNAVAILABLE };
    signedOut = true;
  }
  if (signedOut) redirect(signInPath(`/bookings/${parsed.data}`));
  return { ok: true };
}
