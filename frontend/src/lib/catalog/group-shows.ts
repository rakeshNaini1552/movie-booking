import type { Show } from "./schemas";

export type TheaterShows = {
  theaterId: string;
  theaterName: string;
  city: string;
  shows: Show[];
};

// Cancelled shows are dropped. Theaters are ordered by name, shows by start time.
export function groupShowsByTheater(shows: readonly Show[]): TheaterShows[] {
  const byTheater = new Map<string, TheaterShows>();
  for (const show of shows) {
    if (show.status === "CANCELLED") continue;
    const { theater } = show.screen;
    const group = byTheater.get(theater.id) ?? {
      theaterId: theater.id,
      theaterName: theater.name,
      city: theater.city,
      shows: [],
    };
    group.shows.push(show);
    byTheater.set(theater.id, group);
  }
  return [...byTheater.values()]
    .map((g) => ({ ...g, shows: g.shows.toSorted((a, b) => a.startsAt.localeCompare(b.startsAt)) }))
    .sort((a, b) => a.theaterName.localeCompare(b.theaterName));
}
