import { z } from "zod";
import { idSchema } from "@/lib/catalog/schemas";

// These mirror the REST responses of catalog-service (MovieResponse, TheaterResponse, ...).
// The customer pages use GraphQL; the admin pages use REST because only REST can write.

export const movieStatusSchema = z.enum(["ACTIVE", "ARCHIVED"]);

export const adminMovieSchema = z.object({
  id: idSchema,
  title: z.string(),
  description: z.string().nullable(),
  durationMinutes: z.number().int().positive(),
  language: z.string(),
  rating: z.string().nullable(),
  releaseDate: z.string().nullable(),
  status: movieStatusSchema,
});

export const adminTheaterSchema = z.object({
  id: idSchema,
  name: z.string(),
  city: z.string(),
  address: z.string(),
});

export const adminScreenSchema = z.object({
  id: idSchema,
  theaterId: idSchema,
  name: z.string(),
  seatRows: z.number().int().positive(),
  seatsPerRow: z.number().int().positive(),
});

export const adminShowSchema = z.object({
  id: idSchema,
  movieId: idSchema,
  screenId: idSchema,
  startsAt: z.iso.datetime(),
  endsAt: z.iso.datetime(),
  basePrice: z.number().nonnegative(),
  status: z.enum(["SCHEDULED", "CANCELLED"]),
});

// Mirrors PageResponse<T>.
export const pageOf = <S extends z.ZodType>(item: S) =>
  z.object({
    content: z.array(item),
    page: z.number().int().nonnegative(),
    size: z.number().int().positive(),
    totalElements: z.number().int().nonnegative(),
    totalPages: z.number().int().nonnegative(),
  });

export type AdminMovie = z.infer<typeof adminMovieSchema>;
export type AdminTheater = z.infer<typeof adminTheaterSchema>;
export type AdminScreen = z.infer<typeof adminScreenSchema>;
export type AdminShow = z.infer<typeof adminShowSchema>;

// The limits catalog-service enforces (FieldLimits). Checked here first so the admin gets a
// clear message next to the form instead of a generic "rejected".
export const LIMITS = {
  title: 255,
  language: 50,
  rating: 10,
  theaterName: 255,
  city: 100,
  address: 500,
  screenName: 100,
  maxSeatRows: 26,
  maxSeatsPerRow: 99,
} as const;
