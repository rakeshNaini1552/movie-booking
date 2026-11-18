import "server-only";
import { z } from "zod";
import { GatewayError, GatewayNotFoundError } from "@/lib/gateway/errors";
import { gatewayGet } from "@/lib/gateway/rest";
import { gatewayQuery } from "./client";
import { idSchema } from "./schemas";

// What a booking page shows about the show a booking is for.
export type ShowContext = {
  movieId: string;
  movieTitle: string;
  startsAt: string;
  theaterName: string;
  city: string;
  screenName: string;
};

// The REST show: the only way to get from a show id to its movie id.
const restShowSchema = z.object({ id: idSchema, movieId: idSchema });

const contextMovieSchema = z.object({
  id: idSchema,
  title: z.string(),
  shows: z.array(
    z.object({
      id: idSchema,
      startsAt: z.iso.datetime(),
      screen: z.object({
        name: z.string(),
        theater: z.object({ name: z.string(), city: z.string() }),
      }),
    }),
  ),
});

// A booking only stores a show id. This looks up the movie, theater and start time for a set of
// shows: one REST call per show (to learn its movie), then ONE GraphQL call for all the movies
// (each movie is an aliased field: m0, m1, ...). Keeping the number of calls low matters because
// the gateway rate-limits each user.
//
// A show that cannot be resolved is left out of the map, and the page shows the booking without
// movie details: an outage in the catalog must not hide someone's bookings or tickets.
export async function getShowContexts(
  showIds: readonly string[],
): Promise<Map<string, ShowContext>> {
  const unique = [...new Set(showIds)];
  const movieIdByShow = new Map<string, string>();
  await Promise.all(
    unique.map(async (showId) => {
      try {
        const show = await gatewayGet("catalog", `/api/v1/shows/${showId}`, restShowSchema);
        movieIdByShow.set(showId, show.movieId);
      } catch (error) {
        if (!(error instanceof GatewayNotFoundError) && !(error instanceof GatewayError)) {
          throw error;
        }
      }
    }),
  );

  const contexts = new Map<string, ShowContext>();
  const movieIds = [...new Set(movieIdByShow.values())];
  if (movieIds.length === 0) return contexts;

  const fields = movieIds
    .map(
      (_, index) =>
        `m${index}: movie(id: $id${index}) { id title shows { id startsAt screen { name theater { name city } } } }`,
    )
    .join("\n");
  const signature = movieIds.map((_, index) => `$id${index}: ID!`).join(", ");
  const variables = Object.fromEntries(movieIds.map((id, index) => [`id${index}`, id]));

  let movies: Record<string, z.infer<typeof contextMovieSchema> | null>;
  try {
    movies = await gatewayQuery(
      `query BookingMovies(${signature}) {\n${fields}\n}`,
      variables,
      z.record(z.string(), contextMovieSchema.nullable()),
    );
  } catch (error) {
    if (error instanceof GatewayError) return contexts;
    throw error;
  }

  for (const movie of Object.values(movies)) {
    if (!movie) continue;
    for (const show of movie.shows) {
      if (movieIdByShow.get(show.id) !== movie.id) continue;
      contexts.set(show.id, {
        movieId: movie.id,
        movieTitle: movie.title,
        startsAt: show.startsAt,
        theaterName: show.screen.theater.name,
        city: show.screen.theater.city,
        screenName: show.screen.name,
      });
    }
  }
  return contexts;
}
