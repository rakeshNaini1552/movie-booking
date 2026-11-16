import { notFound } from "next/navigation";
import Header from "@/components/Header";
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

  const details = [
    `${movie.durationMinutes} min`,
    movie.language,
    movie.rating,
    movie.releaseDate && `Released ${movie.releaseDate}`,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <>
      <Header title={movie.title} />
      <p className="text-sm text-zinc-700 dark:text-zinc-300">{details}</p>
      {movie.description && <p className="mt-4 max-w-prose">{movie.description}</p>}
      <h2 className="mt-8 mb-4 text-2xl font-semibold">Shows</h2>
      <ShowsByTheater shows={movie.shows} />
    </>
  );
}
