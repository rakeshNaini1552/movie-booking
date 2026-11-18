import MovieGridSkeleton from "@/components/MovieGridSkeleton";
import PageContainer from "@/components/PageContainer";

// Covers the /movies list and /movies/[id] while a segment loads.
export default function Loading() {
  return (
    <PageContainer>
      <MovieGridSkeleton label="Loading movies..." />
    </PageContainer>
  );
}
