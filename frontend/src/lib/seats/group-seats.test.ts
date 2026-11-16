import { describe, expect, it } from "vitest";
import { groupSeatsByRow } from "./group-seats";
import type { Seat } from "./schemas";

const seat = (row: string, number: number): Seat => ({
  seatId: `${row}${number}`,
  row,
  number,
  type: "SILVER",
  status: "AVAILABLE",
});

describe("groupSeatsByRow", () => {
  it("orders rows alphabetically (AA after Z) and seats by number", () => {
    const rows = groupSeatsByRow([
      seat("B", 2),
      seat("AA", 1),
      seat("A", 10),
      seat("A", 2),
      seat("B", 1),
      seat("Z", 1),
    ]);
    expect(rows.map((r) => r.row)).toEqual(["A", "B", "Z", "AA"]);
    expect(rows[0].seats.map((s) => s.number)).toEqual([2, 10]);
  });

  it("returns nothing for no seats", () => {
    expect(groupSeatsByRow([])).toEqual([]);
  });
});
