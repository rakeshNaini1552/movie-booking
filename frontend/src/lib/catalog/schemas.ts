import { z } from "zod";

// guid, not uuid: it accepts any 8-4-4-4-12 hex id, including hand-seeded ones like 0000...0001.
export const idSchema = z.guid();

const theaterSchema = z.object({ id: idSchema, name: z.string(), city: z.string() });
const screenSchema = z.object({ name: z.string(), theater: theaterSchema });

const showSchema = z.object({
  id: idSchema,
  // The catalog serialises Instant as ISO-8601 UTC; reject anything else instead of showing "Invalid Date".
  startsAt: z.iso.datetime(),
  basePrice: z.number().nonnegative(),
  status: z.string(),
  screen: screenSchema,
});

// Only the fields the list shows.
export const movieSummarySchema = z.object({
  id: idSchema,
  title: z.string(),
  durationMinutes: z.number().int().nonnegative(),
  language: z.string(),
  rating: z.string().nullable(),
  // A path on this site (/posters/dune.jpg) or an https URL; null when the movie has no poster.
  posterUrl: z.string().nullish(),
});

export const movieDetailSchema = movieSummarySchema.extend({
  description: z.string().nullable(),
  releaseDate: z.string().nullable(),
  shows: z.array(showSchema),
});

export const moviesDataSchema = z.object({ movies: z.array(movieSummarySchema) });
// The home page row: each movie with just enough of its shows to tell whether it is bookable.
export const nowShowingDataSchema = z.object({
  movies: z.array(
    movieSummarySchema.extend({
      shows: z.array(z.object({ startsAt: z.iso.datetime(), status: z.string() })),
    }),
  ),
});
export const movieDataSchema = z.object({ movie: movieDetailSchema.nullable() });

export type MovieSummary = z.infer<typeof movieSummarySchema>;
export type MovieDetail = z.infer<typeof movieDetailSchema>;
export type Show = MovieDetail["shows"][number];
