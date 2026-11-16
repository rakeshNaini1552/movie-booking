import Link from "next/link";
import { Suspense } from "react";
import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";
import MovieDetails from "./movie-details";

export default function MoviePage({ params }: PageProps<"/movies/[id]">) {
  return (
    <PageContainer>
      <Link href="/movies" className="mb-4 inline-block text-sm font-medium underline">
        All movies
      </Link>
      {/* params and the session are request data: read behind a boundary. */}
      <Suspense fallback={<LoadingMessage>Loading movie...</LoadingMessage>}>
        <MovieDetails params={params} />
      </Suspense>
    </PageContainer>
  );
}
