import Header from "@/components/Header";
import MovieList from "@/components/MovieList";
import PageContainer from "@/components/PageContainer";

export default function MoviesPage() {
  return (
    <PageContainer>
      <Header title="All Movies" />
      <MovieList />
    </PageContainer>
  );
}
