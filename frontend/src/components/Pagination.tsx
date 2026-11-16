import Link from "next/link";

type PaginationProps = {
  previousHref: string | null;
  nextHref: string | null;
};

const linkClass =
  "rounded-md border border-zinc-400 px-4 py-2 text-sm font-medium hover:bg-zinc-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 dark:border-zinc-600 dark:hover:bg-zinc-800 dark:focus-visible:outline-indigo-400";

export default function Pagination({ previousHref, nextHref }: PaginationProps) {
  if (!previousHref && !nextHref) return null;
  return (
    <nav aria-label="Pagination" className="mt-6 flex gap-4">
      {previousHref && (
        <Link href={previousHref} rel="prev" className={linkClass}>
          Previous
        </Link>
      )}
      {nextHref && (
        <Link href={nextHref} rel="next" className={linkClass}>
          Next
        </Link>
      )}
    </nav>
  );
}
