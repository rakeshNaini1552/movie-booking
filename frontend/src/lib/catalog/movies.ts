import "server-only";
import { gatewayQuery } from "./client";
import { CatalogError } from "./errors";
import {
  idSchema,
  movieDataSchema,
  moviesDataSchema,
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
    if (error instanceof CatalogError && error.isNotFound) return null;
    throw error;
  }
}
