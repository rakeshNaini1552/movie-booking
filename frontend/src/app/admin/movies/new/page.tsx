import { Suspense } from "react";
import ActionForm from "@/components/admin/ActionForm";
import AdminSection from "@/components/admin/AdminSection";
import MovieFields from "@/components/admin/MovieFields";
import BackLink from "@/components/BackLink";
import LoadingMessage from "@/components/LoadingMessage";
import { saveMovieAction } from "@/lib/admin/actions";
import { requireAdmin } from "@/lib/admin/guard";

export default function NewMoviePage() {
  return (
    <>
      <BackLink href="/admin/movies">All movies</BackLink>
      <Suspense fallback={<LoadingMessage>Loading...</LoadingMessage>}>
        <NewMovie />
      </Suspense>
    </>
  );
}

async function NewMovie() {
  await requireAdmin("/admin/movies/new");
  return (
    <AdminSection id="new-movie" title="Add a movie">
      {/* bind(null, null): the action's first argument is the movie id, and a new movie has none. */}
      <ActionForm
        action={saveMovieAction.bind(null, null)}
        submitLabel="Add movie"
        pendingLabel="Adding..."
      >
        <MovieFields />
      </ActionForm>
    </AdminSection>
  );
}
