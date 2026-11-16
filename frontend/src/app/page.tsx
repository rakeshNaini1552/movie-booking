import Header from "@/components/Header";
import MovieList from "@/components/MovieList";
import PageContainer from "@/components/PageContainer";

export default function Home() {
  return (
    <PageContainer>
      <Header title="Movie Booking" />
      <MovieList />
    </PageContainer>
  );
}
