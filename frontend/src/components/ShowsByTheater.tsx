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
    return (
      <p className="border-line bg-panel rounded-2xl border p-6 text-zinc-300">
        No shows are scheduled for this movie yet.
      </p>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      {groups.map((group) => (
        <section
          key={group.theaterId}
          aria-labelledby={`theater-${group.theaterId}`}
          className="border-line bg-panel rounded-2xl border p-5"
        >
          <h3 id={`theater-${group.theaterId}`} className="text-lg font-semibold">
            {group.theaterName}
            <span className="ml-2 text-sm font-normal text-zinc-400">{group.city}</span>
          </h3>
          <ul className="mt-4 flex flex-wrap gap-3">
            {group.shows.map((show) => (
              <li key={show.id}>
                {/* One show time is one link: time, screen and price together. */}
                <Link
                  href={`/movies/${movieId}/shows/${show.id}/seats`}
                  aria-label={`Select seats for ${formatShowTime(show.startsAt)} at ${group.theaterName}`}
                  className="border-line bg-raised hover:border-brand-soft hover:bg-brand/15 flex flex-col rounded-xl border px-4 py-2.5 transition-colors"
                >
                  <time dateTime={show.startsAt} className="font-semibold">
                    {formatShowTime(show.startsAt)}
                  </time>
                  <span className="text-xs text-zinc-400">
                    {show.screen.name} · from {formatPrice(show.basePrice)}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
