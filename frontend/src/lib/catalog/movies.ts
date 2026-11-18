import "server-only";
import { gatewayQuery } from "./client";
import { GatewayError } from "@/lib/gateway/errors";
import {
  idSchema,
  movieDataSchema,
  moviesDataSchema,
  nowShowingDataSchema,
  type MovieDetail,
  type MovieSummary,
} from "./schemas";

const MOVIES_QUERY = /* GraphQL */ `
  query Movies($titleContains: String, $page: Int!, $size: Int!) {
    movies(titleContains: $titleContains, page: $page, size: $size) {
      id
      title
      durationMinutes
      language
      rating
    }
  }
`;

const MOVIE_QUERY = /* GraphQL */ `
  query Movie($id: ID!) {
    movie(id: $id) {
      id
      title
      description
      durationMinutes
      language
      rating
      releaseDate
      shows {
        id
        startsAt
        basePrice
        status
        screen {
          name
          theater {
            id
            name
            city
          }
        }
      }
    }
  }
`;

const NOW_SHOWING_QUERY = /* GraphQL */ `
  query NowShowing($size: Int!) {
    movies(page: 0, size: $size) {
      id
      title
      durationMinutes
      language
      rating
      shows {
        startsAt
        status
      }
    }
  }
`;

// How many movies are looked at to find the ones with a show. One gateway call: the catalog
// loads the shows of all these movies in a single batch.
const NOW_SHOWING_SCAN = 60;

// Movies that can actually be booked: at least one show that is not cancelled and has not
// started yet. Returns at most `limit` of them, soonest show first.
export async function listNowShowing(limit: number, now: Date): Promise<MovieSummary[]> {
  const data = await gatewayQuery(
    NOW_SHOWING_QUERY,
    { size: NOW_SHOWING_SCAN },
    nowShowingDataSchema,
  );
  const cutoff = now.toISOString();
  return data.movies
    .map(({ shows, ...movie }) => ({
      movie,
      // ISO-8601 UTC strings sort in time order, so plain string comparison is enough.
      next: shows
        .filter((show) => show.status !== "CANCELLED" && show.startsAt > cutoff)
        .map((show) => show.startsAt)
        .sort()[0],
    }))
    .filter((entry): entry is { movie: MovieSummary; next: string } => entry.next !== undefined)
    .sort((a, b) => a.next.localeCompare(b.next))
    .slice(0, limit)
    .map((entry) => entry.movie);
}

export type MoviePage = { movies: MovieSummary[]; hasNext: boolean };

// The catalog returns a plain list with no total, so this asks for one extra row to learn
// whether a next page exists. `size` must stay below the catalog's page-size cap (100).
export async function listMovies({
  titleContains,
  page,
  size,
}: {
  titleContains?: string;
  page: number;
  size: number;
}): Promise<MoviePage> {
  if (!Number.isInteger(page) || page < 0) throw new RangeError("page must be an integer >= 0");
  if (!Number.isInteger(size) || size < 1 || size > 99) {
    throw new RangeError("size must be an integer between 1 and 99");
  }
  const data = await gatewayQuery(
    MOVIES_QUERY,
    { titleContains: titleContains || undefined, page, size: size + 1 },
    moviesDataSchema,
  );
  return { movies: data.movies.slice(0, size), hasNext: data.movies.length > size };
}

// Returns null when the movie does not exist (or the id is not an id at all).
export async function getMovie(id: string): Promise<MovieDetail | null> {
  if (!idSchema.safeParse(id).success) return null;
  try {
    const data = await gatewayQuery(MOVIE_QUERY, { id }, movieDataSchema);
    return data.movie;
  } catch (error) {
    if (error instanceof GatewayError && error.isNotFound) return null;
    throw error;
  }
}
