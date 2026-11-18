import Link from "next/link";
import { connection } from "next/server";
import { auth } from "@/auth";
import MovieList from "@/components/MovieList";
import { listNowShowing } from "@/lib/catalog/movies";

const FEATURED_COUNT = 4;

// A row of movies on the home page: only movies that have an upcoming show, so every card
// leads to something that can be booked. The home page is public and the catalog needs a signed-in
// user, so this shows nothing for a visitor, and nothing when the catalog cannot be reached:
// a decoration must never break the home page.
export default async function NowShowing() {
  await connection();
  const session = await auth();
  if (!session) return null;

  let movies;
  try {
    movies = await listNowShowing(FEATURED_COUNT, new Date());
  } catch {
    return null;
  }
  if (movies.length === 0) return null;

  return (
    <section aria-labelledby="now-showing" className="mt-6">
      <div className="mb-6 flex items-end justify-between gap-4">
        <h2 id="now-showing" className="font-display text-2xl font-bold">
          Now showing
        </h2>
        <Link href="/movies" className="text-brand-soft text-sm font-semibold hover:underline">
          See all movies
        </Link>
      </div>
      <MovieList movies={movies} />
    </section>
  );
}
