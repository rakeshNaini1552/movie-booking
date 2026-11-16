import "server-only";
import { idSchema } from "@/lib/catalog/schemas";
import { GatewayNotFoundError } from "@/lib/gateway/errors";
import { gatewayGet } from "@/lib/gateway/rest";
import { type Seat, seatMapSchema } from "./schemas";

// Returns null when the show has no seats (the seat service answers 404) or the id is not an id.
export async function getSeatMap(showId: string): Promise<Seat[] | null> {
  if (!idSchema.safeParse(showId).success) return null;
  try {
    return await gatewayGet("seats", `/api/v1/shows/${showId}/seats`, seatMapSchema);
  } catch (error) {
    if (error instanceof GatewayNotFoundError) return null;
    throw error;
  }
}
