import MovieCard from "@/components/MovieCard";
import type { MovieSummary } from "@/lib/catalog/schemas";

type MovieListProps = {
  movies: readonly MovieSummary[];
};

export default function MovieList({ movies }: MovieListProps) {
  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {movies.map((movie) => (
        <li key={movie.id}>
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
