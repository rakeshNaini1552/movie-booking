import { describe, expect, it } from "vitest";
import { groupShowsByTheater } from "./group-shows";
import type { Show } from "./schemas";

const show = (
  id: string,
  theaterId: string,
  theaterName: string,
  startsAt: string,
  status = "SCHEDULED",
): Show => ({
  id: `00000000-0000-0000-0000-00000000000${id}`,
  startsAt,
  basePrice: 10,
  status,
  screen: { name: "Screen 1", theater: { id: theaterId, name: theaterName, city: "Austin" } },
});

describe("groupShowsByTheater", () => {
  it("groups by theater, orders theaters by name and shows by start time", () => {
    const groups = groupShowsByTheater([
      show("1", "t-b", "Zeta Cinema", "2026-12-01T20:00:00Z"),
      show("2", "t-a", "Alpha Cinema", "2026-12-02T10:00:00Z"),
      show("3", "t-b", "Zeta Cinema", "2026-12-01T09:00:00Z"),
    ]);

    expect(groups.map((g) => g.theaterName)).toEqual(["Alpha Cinema", "Zeta Cinema"]);
    expect(groups[1].shows.map((s) => s.startsAt)).toEqual([
      "2026-12-01T09:00:00Z",
      "2026-12-01T20:00:00Z",
    ]);
  });

  it("drops cancelled shows, and a theater left with none", () => {
    const groups = groupShowsByTheater([
      show("1", "t-a", "Alpha", "2026-12-01T10:00:00Z", "CANCELLED"),
      show("2", "t-b", "Beta", "2026-12-01T10:00:00Z"),
    ]);
    expect(groups.map((g) => g.theaterName)).toEqual(["Beta"]);
  });

  it("returns nothing for no shows", () => {
    expect(groupShowsByTheater([])).toEqual([]);
  });
});
