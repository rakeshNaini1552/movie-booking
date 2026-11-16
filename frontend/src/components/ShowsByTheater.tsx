import Link from "next/link";
import { groupShowsByTheater } from "@/lib/catalog/group-shows";
import type { Show } from "@/lib/catalog/schemas";
import { formatPrice, formatShowTime } from "@/lib/format";

type ShowsByTheaterProps = {
  movieId: string;
  shows: readonly Show[];
};

export default function ShowsByTheater({ movieId, shows }: ShowsByTheaterProps) {
  const groups = groupShowsByTheater(shows);
  if (groups.length === 0) {
    return <p>No shows are scheduled for this movie yet.</p>;
  }
  return (
    <div className="flex flex-col gap-6">
      {groups.map((group) => (
        <section key={group.theaterId} aria-labelledby={`theater-${group.theaterId}`}>
          <h3 id={`theater-${group.theaterId}`} className="text-lg font-semibold">
            {group.theaterName}
            <span className="ml-2 text-sm font-normal text-zinc-700 dark:text-zinc-300">
              {group.city}
            </span>
          </h3>
          <ul className="mt-2 divide-y divide-zinc-300 rounded-lg border border-zinc-300 dark:divide-zinc-700 dark:border-zinc-700">
            {group.shows.map((show) => (
              <li key={show.id} className="flex flex-wrap justify-between gap-x-6 gap-y-1 p-3">
                <time dateTime={show.startsAt}>{formatShowTime(show.startsAt)}</time>
                <span className="text-zinc-700 dark:text-zinc-300">{show.screen.name}</span>
                <span>{formatPrice(show.basePrice)}</span>
                <Link
                  href={`/movies/${movieId}/shows/${show.id}/seats`}
                  aria-label={`Select seats for ${formatShowTime(show.startsAt)} at ${group.theaterName}`}
                  className="rounded-md font-medium text-indigo-700 underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 dark:text-indigo-300 dark:focus-visible:outline-indigo-400"
                >
                  Select seats
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
