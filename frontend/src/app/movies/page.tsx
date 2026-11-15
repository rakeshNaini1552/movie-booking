import Counter from "@/components/Counter";
import Header from "@/components/Header";
import MovieList from "@/components/MovieList";

export default function MoviesPage() {
  return (
    <main>
      <Header title="All Movies" />
      <MovieList />
      {/* Learning exercise (client state), to be removed in F2. */}
      <Counter />
    </main>
  );
}
