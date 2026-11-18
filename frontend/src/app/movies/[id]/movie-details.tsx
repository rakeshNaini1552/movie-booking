import { notFound } from "next/navigation";
import MoviePoster from "@/components/MoviePoster";
import ShowsByTheater from "@/components/ShowsByTheater";
import { requireSignIn } from "@/lib/auth/require-sign-in";
import { getMovie } from "@/lib/catalog/movies";
import { idSchema } from "@/lib/catalog/schemas";

export default async function MovieDetails({ params }: Pick<PageProps<"/movies/[id]">, "params">) {
  const { id } = await params;
  // The id comes from the address bar: check its shape before it goes anywhere near the gateway.
  if (!idSchema.safeParse(id).success) notFound();

  const movie = await requireSignIn(`/movies/${id}`, () => getMovie(id));
  if (!movie) notFound();

  const facts = [
    `${movie.durationMinutes} min`,
    movie.language,
    movie.rating,
    movie.releaseDate && `Released ${movie.releaseDate}`,
  ].filter((fact): fact is string => Boolean(fact));

  return (
    <>
      <div className="animate-rise grid gap-8 sm:grid-cols-[minmax(0,16rem)_1fr] sm:items-end">
        <MoviePoster title={movie.title} className="mx-auto w-48 sm:mx-0 sm:w-full" />
        <div>
          <h1 className="font-display text-4xl font-extrabold tracking-tight text-balance sm:text-6xl">
            {movie.title}
          </h1>
          <ul aria-label="Movie facts" className="mt-5 flex flex-wrap gap-2">
            {facts.map((fact) => (
              <li
                key={fact}
                className="border-line bg-panel rounded-full border px-3 py-1 text-sm text-zinc-200"
              >
                {fact}
              </li>
            ))}
          </ul>
          {movie.description && (
            <p className="mt-6 max-w-prose leading-relaxed text-zinc-300">{movie.description}</p>
          )}
        </div>
      </div>
      <h2 className="font-display mt-14 mb-5 text-2xl font-bold">Shows</h2>
      <ShowsByTheater movieId={movie.id} shows={movie.shows} />
    </>
  );
}
