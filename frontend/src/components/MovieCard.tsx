import Link from "next/link";
import MoviePoster from "@/components/MoviePoster";

type MovieCardProps = {
  title: string;
  durationMinutes: number;
  language?: string;
  rating?: string | null;
  href?: string;
};

export default function MovieCard({
  title,
  durationMinutes,
  language,
  rating,
  href,
}: MovieCardProps) {
  // The rating is also a tag on the poster, but the poster is decorative, so it is repeated here as text.
  const details = [`${durationMinutes} min`, language, rating].filter(Boolean).join(" · ");
  return (
    // "group" lets the poster react when any part of the card is hovered or focused.
    <article className="group relative">
      <MoviePoster
        title={title}
        tag={rating}
        className="group-focus-within:ring-brand-soft transition duration-300 group-hover:-translate-y-1.5 group-hover:shadow-2xl group-hover:shadow-black/60 group-hover:ring-white/30"
      />
      <h2 className="mt-3 leading-snug font-semibold">
        {href ? (
          // The link covers the whole card (after:absolute after:inset-0), so the poster is clickable too.
          <Link href={href} className="rounded-md after:absolute after:inset-0">
            {title}
          </Link>
        ) : (
          title
        )}
      </h2>
      <p className="mt-0.5 text-sm text-zinc-400">{details}</p>
    </article>
  );
}
