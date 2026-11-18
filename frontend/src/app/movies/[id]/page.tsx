import { Suspense } from "react";
import BackLink from "@/components/BackLink";
import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";
import MovieDetails from "./movie-details";

export default function MoviePage({ params }: PageProps<"/movies/[id]">) {
  return (
    <PageContainer>
      <BackLink href="/movies">All movies</BackLink>
      {/* params and the session are request data: read behind a boundary. */}
      <Suspense fallback={<LoadingMessage>Loading movie...</LoadingMessage>}>
        <MovieDetails params={params} />
      </Suspense>
    </PageContainer>
  );
}
