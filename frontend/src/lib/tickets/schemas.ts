import { z } from "zod";
import { idSchema } from "@/lib/catalog/schemas";

export const ticketStatusSchema = z.enum(["ISSUED", "USED", "VOID"]);

// Mirrors TicketResponse in ticket-service.
export const ticketSchema = z.object({
  ticketId: idSchema,
  bookingId: idSchema,
  status: ticketStatusSchema,
  qrPayload: z.string().min(1),
  issuedAt: z.iso.datetime(),
  usedAt: z.iso.datetime().nullable(),
});

export type Ticket = z.infer<typeof ticketSchema>;
export type TicketStatus = z.infer<typeof ticketStatusSchema>;
