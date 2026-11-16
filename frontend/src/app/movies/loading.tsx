import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";

// Covers the /movies list and /movies/[id] while a segment loads.
export default function Loading() {
  return (
    <PageContainer>
      <LoadingMessage>Loading movies...</LoadingMessage>
    </PageContainer>
  );
}
