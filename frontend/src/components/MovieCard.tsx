type MovieCardProps = {
  title: string;
  durationMinutes: number;
};

// Poster and showtimes are added later.
export default function MovieCard({ title, durationMinutes }: MovieCardProps) {
  return (
    <article className="rounded-lg border border-zinc-300 p-4 dark:border-zinc-700">
      <h2 className="text-lg font-semibold">{title}</h2>
      <p className="mt-1 text-sm text-zinc-700 dark:text-zinc-300">{durationMinutes} min</p>
    </article>
  );
}
