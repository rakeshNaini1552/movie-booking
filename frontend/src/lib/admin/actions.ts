"use server";

import { redirect } from "next/navigation";
import { z } from "zod";
import { idSchema } from "@/lib/catalog/schemas";
import { httpStatusOf, NotSignedInError } from "@/lib/gateway/errors";
import {
  cancelShow,
  createMovie,
  createScreen,
  createShow,
  createTheater,
  deleteTheater,
  getAdminMovie,
  updateMovie,
} from "./api";
import type { FormState } from "./form-state";
import { isAdmin } from "./guard";
import { LIMITS, movieStatusSchema } from "./schemas";

// Every action here: (1) checks the admin role, (2) validates the form with Zod, (3) calls the
// catalog through the gateway, (4) redirects on success or returns a message for the form.
// Step 1 is a courtesy for a clear message; the real check is in catalog-service.

const NOT_ALLOWED: FormState = { error: "You need the catalog admin role to do that." };

const text = (max: number) =>
  z.string().trim().min(1, "Required").max(max, `At most ${max} characters`);
const optionalText = (max: number) =>
  z
    .string()
    .trim()
    .max(max, `At most ${max} characters`)
    .transform((value) => value || null);

// Turns Zod issues into one message per input name.
function fieldErrors(error: z.ZodError): FormState {
  const fields: Record<string, string> = {};
  for (const issue of error.issues) {
    const name = String(issue.path[0] ?? "");
    if (name && !fields[name]) fields[name] = issue.message;
  }
  return { error: "Please fix the highlighted fields.", fields };
}

// What to tell the admin when the catalog refused a write.
function writeFailure(error: unknown, conflict: string): FormState {
  const status = httpStatusOf(error);
  if (status === 403) return NOT_ALLOWED;
  if (status === 400) return { error: "The catalog rejected those values. Check them and retry." };
  if (status === 404) return { error: "That item no longer exists." };
  if (status === 409) return { error: conflict };
  return { error: "The catalog is not responding. Please try again in a moment." };
}

const signIn = (returnTo: string) => `/signin?callbackUrl=${encodeURIComponent(returnTo)}`;

const movieSchema = z.object({
  title: text(LIMITS.title),
  description: optionalText(5000),
  durationMinutes: z.coerce.number("Enter a number").int("Whole minutes").positive("Above 0"),
  language: text(LIMITS.language),
  rating: optionalText(LIMITS.rating),
  releaseDate: z
    .string()
    .trim()
    .refine((value) => value === "" || /^\d{4}-\d{2}-\d{2}$/.test(value), "Use YYYY-MM-DD")
    .transform((value) => value || null),
  status: movieStatusSchema.catch("ACTIVE"),
});

// movieId is null for a new movie. Bound by the page with .bind(null, id).
export async function saveMovieAction(
  movieId: string | null,
  _previous: FormState,
  formData: FormData,
): Promise<FormState> {
  if (!(await isAdmin())) return NOT_ALLOWED;
  if (movieId !== null && !idSchema.safeParse(movieId).success) {
    return { error: "That movie no longer exists." };
  }
  const parsed = movieSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) return fieldErrors(parsed.error);

  let target: string;
  try {
    const movie = movieId
      ? await updateMovie(movieId, parsed.data)
      : await createMovie(parsed.data);
    target = `/admin/movies/${movie.id}?saved=1`;
  } catch (error) {
    if (!(error instanceof NotSignedInError)) {
      return writeFailure(error, "A movie with those details already exists.");
    }
    target = signIn("/admin/movies");
  }
  redirect(target);
}

const theaterSchema = z.object({
  name: text(LIMITS.theaterName),
  city: text(LIMITS.city),
  address: text(LIMITS.address),
});

export async function createTheaterAction(
  _previous: FormState,
  formData: FormData,
): Promise<FormState> {
  if (!(await isAdmin())) return NOT_ALLOWED;
  const parsed = theaterSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) return fieldErrors(parsed.error);

  let target: string;
  try {
    const theater = await createTheater(parsed.data);
    target = `/admin/theaters/${theater.id}`;
  } catch (error) {
    if (!(error instanceof NotSignedInError)) {
      return writeFailure(error, "A theater with that name already exists in that city.");
    }
    target = signIn("/admin/theaters");
  }
  redirect(target);
}

// A form with only a button: the id is bound by the page, so the form data is not read.
export async function deleteTheaterAction(theaterId: string): Promise<FormState> {
  if (!(await isAdmin())) return NOT_ALLOWED;
  if (!idSchema.safeParse(theaterId).success) return { error: "That item no longer exists." };

  let target = "/admin/theaters";
  try {
    await deleteTheater(theaterId);
  } catch (error) {
    if (!(error instanceof NotSignedInError)) {
      return writeFailure(
        error,
        "This theater still has screens or shows, so it cannot be deleted.",
      );
    }
    target = signIn("/admin/theaters");
  }
  redirect(target);
}

const screenSchema = z.object({
  name: text(LIMITS.screenName),
  seatRows: z.coerce
    .number("Enter a number")
    .int("Whole number")
    .min(1, "At least 1")
    .max(LIMITS.maxSeatRows, `At most ${LIMITS.maxSeatRows}`),
  seatsPerRow: z.coerce
    .number("Enter a number")
    .int("Whole number")
    .min(1, "At least 1")
    .max(LIMITS.maxSeatsPerRow, `At most ${LIMITS.maxSeatsPerRow}`),
});

export async function createScreenAction(
  theaterId: string,
  _previous: FormState,
  formData: FormData,
): Promise<FormState> {
  if (!(await isAdmin())) return NOT_ALLOWED;
  if (!idSchema.safeParse(theaterId).success) return { error: "That item no longer exists." };
  const parsed = screenSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) return fieldErrors(parsed.error);

  let target = `/admin/theaters/${theaterId}`;
  try {
    await createScreen(theaterId, parsed.data);
  } catch (error) {
    if (!(error instanceof NotSignedInError)) {
      return writeFailure(error, "This theater already has a screen with that name.");
    }
    target = signIn(target);
  }
  redirect(target);
}

const showSchema = z.object({
  screenId: idSchema,
  // <input type="datetime-local"> sends "2026-12-01T18:30" with no zone. The app shows every
  // time in UTC, so the admin enters UTC and the action adds the zone.
  startsAt: z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/, "Pick a date and time"),
  basePrice: z.coerce.number("Enter a number").min(0, "0 or more").max(99_999_999, "Too large"),
});

const MINUTE_MS = 60_000;

export async function createShowAction(
  movieId: string,
  _previous: FormState,
  formData: FormData,
): Promise<FormState> {
  if (!(await isAdmin())) return NOT_ALLOWED;
  if (!idSchema.safeParse(movieId).success) return { error: "That movie no longer exists." };
  const parsed = showSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) return fieldErrors(parsed.error);

  let target = `/admin/movies/${movieId}`;
  try {
    // The show ends when the movie does: start time plus the movie's running time.
    const movie = await getAdminMovie(movieId);
    if (!movie) return { error: "That movie no longer exists." };
    const startsAt = new Date(`${parsed.data.startsAt}:00Z`);
    if (Number.isNaN(startsAt.getTime())) {
      return {
        error: "Please fix the highlighted fields.",
        fields: { startsAt: "Not a valid date" },
      };
    }
    const endsAt = new Date(startsAt.getTime() + movie.durationMinutes * MINUTE_MS);
    await createShow({
      movieId,
      screenId: parsed.data.screenId,
      startsAt: startsAt.toISOString(),
      endsAt: endsAt.toISOString(),
      basePrice: parsed.data.basePrice,
    });
  } catch (error) {
    if (!(error instanceof NotSignedInError)) {
      return writeFailure(error, "That screen already has a show at that time.");
    }
    target = signIn(target);
  }
  redirect(target);
}

export async function cancelShowAction(movieId: string, showId: string): Promise<FormState> {
  if (!(await isAdmin())) return NOT_ALLOWED;
  if (!idSchema.safeParse(movieId).success || !idSchema.safeParse(showId).success) {
    return { error: "That item no longer exists." };
  }

  let target = `/admin/movies/${movieId}`;
  try {
    await cancelShow(showId);
  } catch (error) {
    if (!(error instanceof NotSignedInError)) {
      return writeFailure(error, "That show is already cancelled.");
    }
    target = signIn(target);
  }
  redirect(target);
}
