import { Suspense } from "react";
import LoadingMessage from "@/components/LoadingMessage";
import PageContainer from "@/components/PageContainer";
import SeatMapContent from "./seat-map-content";

export default function SeatsPage({ params }: PageProps<"/movies/[id]/shows/[showId]/seats">) {
  return (
    <PageContainer>
      {/* params and the session are request data: read behind a boundary. */}
      <Suspense fallback={<LoadingMessage>Loading seats...</LoadingMessage>}>
        <SeatMapContent params={params} />
      </Suspense>
    </PageContainer>
  );
}
