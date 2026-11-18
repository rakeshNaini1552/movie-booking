import "server-only";
import { GatewayNotFoundError } from "@/lib/gateway/errors";
import { gatewayGet } from "@/lib/gateway/rest";
import { type Ticket, ticketSchema } from "./schemas";

// Returns null while there is no ticket. That is normal right after payment: ticket-service
// issues the ticket when it consumes the BookingConfirmed event, a moment after the booking
// is confirmed. It also answers 404 for a ticket that belongs to someone else.
export async function getTicketByBooking(bookingId: string): Promise<Ticket | null> {
  try {
    return await gatewayGet("tickets", `/api/v1/tickets/by-booking/${bookingId}`, ticketSchema);
  } catch (error) {
    if (error instanceof GatewayNotFoundError) return null;
    throw error;
  }
}
