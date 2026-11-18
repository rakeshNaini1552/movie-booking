import type { Metadata } from "next";
import { Suspense } from "react";
import Header from "@/components/Header";
import MovieGridSkeleton from "@/components/MovieGridSkeleton";
import PageContainer from "@/components/PageContainer";
import MoviesContent from "./movies-content";

export const metadata: Metadata = { title: "Movies" };

export default function MoviesPage({ searchParams }: PageProps<"/movies">) {
  return (
    <PageContainer>
      <Header title="All Movies" eyebrow="Now showing" />
      {/* searchParams and the session are request data, so everything that uses them sits behind a boundary. */}
      <Suspense fallback={<MovieGridSkeleton label="Loading movies..." />}>
        <MoviesContent searchParams={searchParams} />
      </Suspense>
    </PageContainer>
  );
}
