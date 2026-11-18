import type { BookingStatus } from "./schemas";

// What each booking status means to the customer, and the colour of its badge.
export const BOOKING_STATUS: Record<BookingStatus, { label: string; tone: Tone; text: string }> = {
  PENDING: {
    label: "Awaiting payment",
    tone: "amber",
    text: "Your seats are held for a few minutes. Pay to confirm them.",
  },
  CONFIRMED: { label: "Confirmed", tone: "green", text: "You are all set. Enjoy the movie." },
  FAILED: {
    label: "Failed",
    tone: "red",
    text: "This booking could not be completed. If you paid, the amount is being refunded.",
  },
  EXPIRED: {
    label: "Expired",
    tone: "zinc",
    text: "The seats were held too long without payment and have been released.",
  },
  CANCELLED: {
    label: "Cancelled",
    tone: "zinc",
    text: "This booking was cancelled. If you paid, the amount is being refunded.",
  },
};

export type Tone = "amber" | "green" | "red" | "zinc";
