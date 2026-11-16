import MovieCard from "@/components/MovieCard";

type Movie = {
  id: number;
  title: string;
  durationMinutes: number;
};

const movies: Movie[] = [
  { id: 1, title: "Inception", durationMinutes: 148 },
  { id: 2, title: "Interstellar", durationMinutes: 169 },
  { id: 3, title: "The Dark Knight", durationMinutes: 152 },
];

export default function MovieList() {
  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {movies.map((movie) => (
        <li key={movie.id}>
          <MovieCard title={movie.title} durationMinutes={movie.durationMinutes} />
        </li>
      ))}
    </ul>
  );
}
