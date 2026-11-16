import { z } from "zod";

export const seatTypeSchema = z.enum(["SILVER", "GOLD", "PLATINUM"]);
export const seatStatusSchema = z.enum(["AVAILABLE", "BOOKED", "BLOCKED", "HELD"]);

// Mirrors SeatResponse in seat-service.
export const seatSchema = z.object({
  seatId: z.string().min(1),
  row: z.string().min(1),
  number: z.number().int().positive(),
  type: seatTypeSchema,
  status: seatStatusSchema,
});

export const seatMapSchema = z.array(seatSchema);

export type Seat = z.infer<typeof seatSchema>;
export type SeatType = z.infer<typeof seatTypeSchema>;
export type SeatStatus = z.infer<typeof seatStatusSchema>;
