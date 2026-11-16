import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";

export default function Loading() {
  return (
    <PageContainer>
      <LoadingMessage>Loading seats...</LoadingMessage>
    </PageContainer>
  );
}
