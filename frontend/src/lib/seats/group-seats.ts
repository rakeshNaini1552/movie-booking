import type { Seat } from "./schemas";

export type SeatRow = { row: string; seats: Seat[] };

// Rows A, B, ... Z, AA (shorter labels first), seats in number order.
export function groupSeatsByRow(seats: readonly Seat[]): SeatRow[] {
  const rows = new Map<string, Seat[]>();
  for (const seat of seats) rows.set(seat.row, [...(rows.get(seat.row) ?? []), seat]);
  return [...rows.entries()]
    .sort(([a], [b]) => a.length - b.length || a.localeCompare(b))
    .map(([row, rowSeats]) => ({ row, seats: rowSeats.toSorted((a, b) => a.number - b.number) }));
}
