import Link from "next/link";

type MovieCardProps = {
  title: string;
  durationMinutes: number;
  language?: string;
  rating?: string | null;
  href?: string;
};

// Poster is added later.
export default function MovieCard({
  title,
  durationMinutes,
  language,
  rating,
  href,
}: MovieCardProps) {
  const details = [`${durationMinutes} min`, language, rating].filter(Boolean).join(" · ");
  return (
    <article className="rounded-lg border border-zinc-300 p-4 dark:border-zinc-700">
      <h2 className="text-lg font-semibold">
        {href ? (
          <Link
            href={href}
            className="rounded-md hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 dark:focus-visible:outline-indigo-400"
          >
            {title}
          </Link>
        ) : (
          title
        )}
      </h2>
      <p className="mt-1 text-sm text-zinc-700 dark:text-zinc-300">{details}</p>
    </article>
  );
}
