import MovieList from "@/components/MovieList";
import MovieSearchForm from "@/components/MovieSearchForm";
import Pagination from "@/components/Pagination";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { listMovies } from "@/lib/catalog/movies";
import { listHref } from "./list-href";
import { PAGE_SIZE, parseListParams } from "./list-params";

export default async function MoviesContent({
  searchParams,
}: Pick<PageProps<"/movies">, "searchParams">) {
  const params = parseListParams(await searchParams);
  const { movies, hasNext } = await requireSignIn(listHref(params), () =>
    listMovies({ titleContains: params.q, page: params.page, size: PAGE_SIZE }),
  );

  return (
    <>
      <MovieSearchForm defaultValue={params.q} />
      {movies.length === 0 ? (
        <p>
          {params.q
            ? `No movies match "${params.q}".`
            : params.page > 0
              ? "There are no more movies."
              : "No movies to show yet."}
        </p>
      ) : (
        <MovieList movies={movies} />
      )}
      <Pagination
        previousHref={params.page > 0 ? listHref({ ...params, page: params.page - 1 }) : null}
        nextHref={hasNext ? listHref({ ...params, page: params.page + 1 }) : null}
      />
    </>
  );
}
