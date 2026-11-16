import Link from "next/link";
import PageContainer from "@/components/PageContainer";

export default function NotFound() {
  return (
    <PageContainer>
      <p className="mb-4">Page not found.</p>
      <Link
        href="/"
        className="rounded-md font-medium text-indigo-700 underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 dark:text-indigo-300 dark:focus-visible:outline-indigo-400"
      >
        Return home
      </Link>
    </PageContainer>
  );
}
