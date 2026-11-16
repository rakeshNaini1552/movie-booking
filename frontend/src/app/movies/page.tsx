import { Suspense } from "react";
import Header from "@/components/Header";
import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";
import MoviesContent from "./movies-content";

export default function MoviesPage({ searchParams }: PageProps<"/movies">) {
  return (
    <PageContainer>
      <Header title="All Movies" />
      {/* searchParams and the session are request data, so everything that uses them sits behind a boundary. */}
      <Suspense fallback={<LoadingMessage>Loading movies...</LoadingMessage>}>
        <MoviesContent searchParams={searchParams} />
      </Suspense>
    </PageContainer>
  );
}
