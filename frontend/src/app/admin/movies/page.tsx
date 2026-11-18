import Link from "next/link";
import { Suspense } from "react";
import Button from "@/components/Button";
import ButtonLink from "@/components/ButtonLink";
import Header from "@/components/Header";
import LoadingMessage from "@/components/LoadingMessage";
import Pagination from "@/components/Pagination";
import StatusBadge from "@/components/StatusBadge";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { listAdminMovies } from "@/lib/admin/api";
import { requireAdmin } from "@/lib/admin/guard";
import { parseListParams } from "@/app/movies/list-params";

export default function AdminMoviesPage({ searchParams }: PageProps<"/admin/movies">) {
  return (
    <>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <Header title="Movies" />
        <ButtonLink href="/admin/movies/new">Add movie</ButtonLink>
      </div>
      <Suspense fallback={<LoadingMessage>Loading movies...</LoadingMessage>}>
        <AdminMovies searchParams={searchParams} />
      </Suspense>
    </>
  );
}

function adminListHref({ q, page }: { q: string; page: number }): string {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (page > 0) params.set("page", String(page));
  const query = params.toString();
  return query ? `/admin/movies?${query}` : "/admin/movies";
}

async function AdminMovies({ searchParams }: Pick<PageProps<"/admin/movies">, "searchParams">) {
  // Same validation of ?q= and ?page= as the customer movie list.
  const params = parseListParams(await searchParams);
  const here = adminListHref(params);
  await requireAdmin(here);
  const movies = await requireSignIn(here, () => listAdminMovies(params.q, params.page));

  return (
    <>
      {/* A plain GET form: submitting it puts ?q=... in the address and the page renders again. */}
      <form action="/admin/movies" method="get" role="search" className="mb-6 flex max-w-xl gap-2">
        <label htmlFor="admin-movie-search" className="sr-only">
          Search movies by title
        </label>
        <input
          id="admin-movie-search"
          name="q"
          type="search"
          defaultValue={params.q}
          maxLength={100}
          placeholder="Search by title"
          className="border-line bg-panel focus:border-brand-soft min-w-0 flex-1 rounded-full border px-5 py-2.5 text-sm placeholder:text-zinc-500"
        />
        <Button type="submit" variant="secondary">
          Search
        </Button>
      </form>

      {movies.content.length === 0 ? (
        <p className="border-line bg-panel rounded-2xl border p-6 text-zinc-300">
          {params.q ? `No movies match "${params.q}".` : "There are no movies yet."}
        </p>
      ) : (
        <div className="border-line bg-panel overflow-x-auto rounded-2xl border">
          <table className="w-full text-left text-sm">
            <caption className="sr-only">Movies in the catalog</caption>
            <thead className="border-line border-b text-xs tracking-wider text-zinc-400 uppercase">
              <tr>
                <th scope="col" className="px-5 py-3 font-semibold">
                  Title
                </th>
                <th scope="col" className="px-5 py-3 font-semibold">
                  Length
                </th>
                <th scope="col" className="px-5 py-3 font-semibold">
                  Language
                </th>
                <th scope="col" className="px-5 py-3 font-semibold">
                  Status
                </th>
              </tr>
            </thead>
            <tbody className="divide-line divide-y">
              {movies.content.map((movie) => (
                <tr key={movie.id} className="hover:bg-raised/60">
                  <th scope="row" className="px-5 py-3 font-medium">
                    <Link href={`/admin/movies/${movie.id}`} className="rounded-md hover:underline">
                      {movie.title}
                    </Link>
                  </th>
                  <td className="px-5 py-3 text-zinc-300">{movie.durationMinutes} min</td>
                  <td className="px-5 py-3 text-zinc-300">{movie.language}</td>
                  <td className="px-5 py-3">
                    <StatusBadge
                      label={movie.status === "ACTIVE" ? "Active" : "Archived"}
                      tone={movie.status === "ACTIVE" ? "green" : "zinc"}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pagination
        previousHref={params.page > 0 ? adminListHref({ ...params, page: params.page - 1 }) : null}
        nextHref={
          params.page + 1 < movies.totalPages
            ? adminListHref({ ...params, page: params.page + 1 })
            : null
        }
      />
    </>
  );
}
