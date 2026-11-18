import MovieCard from "@/components/MovieCard";
import type { MovieSummary } from "@/lib/catalog/schemas";

type MovieListProps = {
  movies: readonly MovieSummary[];
};

export default function MovieList({ movies }: MovieListProps) {
  return (
    <ul className="grid grid-cols-2 gap-x-5 gap-y-8 sm:grid-cols-3 lg:grid-cols-4">
      {movies.map((movie) => (
        <li key={movie.id} className="animate-rise">
          <MovieCard
            title={movie.title}
            durationMinutes={movie.durationMinutes}
            language={movie.language}
            rating={movie.rating}
            href={`/movies/${movie.id}`}
          />
        </li>
      ))}
    </ul>
  );
}
