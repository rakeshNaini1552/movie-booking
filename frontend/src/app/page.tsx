import Link from "next/link";
import Header from "@/components/Header";
import PageContainer from "@/components/PageContainer";

export default function Home() {
  return (
    <PageContainer>
      <Header title="Movie Booking" />
      <p className="mb-4">Browse what is showing and pick a show.</p>
      <Link
        href="/movies"
        className="rounded-md font-medium text-indigo-700 underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 dark:text-indigo-300 dark:focus-visible:outline-indigo-400"
      >
        Browse movies
      </Link>
    </PageContainer>
  );
}
