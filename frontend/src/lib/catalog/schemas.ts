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
});

export const movieDetailSchema = movieSummarySchema.extend({
  description: z.string().nullable(),
  releaseDate: z.string().nullable(),
  shows: z.array(showSchema),
});

export const moviesDataSchema = z.object({ movies: z.array(movieSummarySchema) });
export const movieDataSchema = z.object({ movie: movieDetailSchema.nullable() });

export type MovieSummary = z.infer<typeof movieSummarySchema>;
export type MovieDetail = z.infer<typeof movieDetailSchema>;
export type Show = MovieDetail["shows"][number];
