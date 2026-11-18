import { z } from "zod";
import { idSchema } from "@/lib/catalog/schemas";

export const bookingStatusSchema = z.enum([
  "PENDING",
  "CONFIRMED",
  "FAILED",
  "EXPIRED",
  "CANCELLED",
]);
export const paymentStatusSchema = z.enum(["SUCCESS", "FAILED"]);

// Mirrors BookingResponse in booking-service.
export const bookingSchema = z.object({
  bookingId: idSchema,
  showId: idSchema,
  status: bookingStatusSchema,
  totalAmount: z.number().nonnegative(),
  seatIds: z.array(z.string().min(1)),
});

// Mirrors BookingSummary: a booking plus when it was made.
export const bookingSummarySchema = bookingSchema.extend({ createdAt: z.iso.datetime() });

// Mirrors BookingPage.
export const bookingPageSchema = z.object({
  items: z.array(bookingSummarySchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
});

export const createBookingResponseSchema = z.object({ bookingId: idSchema });
export const payBookingResponseSchema = z.object({
  bookingId: idSchema,
  status: bookingStatusSchema,
  paymentStatus: paymentStatusSchema,
});
export const cancelBookingResponseSchema = z.object({
  bookingId: idSchema,
  status: bookingStatusSchema,
});

export type Booking = z.infer<typeof bookingSchema>;
export type BookingSummary = z.infer<typeof bookingSummarySchema>;
export type BookingPage = z.infer<typeof bookingPageSchema>;
export type BookingStatus = z.infer<typeof bookingStatusSchema>;
