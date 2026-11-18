import { posterBackground } from "@/lib/poster";

type MoviePosterProps = {
  title: string;
  // Shown as a small tag in the corner, for example the rating.
  tag?: string | null;
  // A small poster (booking list): only the colour and the first letter, no title text.
  compact?: boolean;
  className?: string;
};

// A generated poster (see lib/poster.ts). Decorative: the movie title is always next to it as
// real text, so the poster is hidden from screen readers.
export default function MoviePoster({ title, tag, compact, className = "" }: MoviePosterProps) {
  const initial = title.charAt(0).toUpperCase();
  if (compact) {
    return (
      <div
        aria-hidden
        style={{ backgroundImage: posterBackground(title) }}
        className={`flex aspect-[2/3] items-center justify-center overflow-hidden rounded-xl ring-1 ring-white/10 ${className}`}
      >
        <span className="font-display text-5xl font-extrabold text-white/30 select-none">
          {initial}
        </span>
      </div>
    );
  }
  return (
    <div
      aria-hidden
      style={{ backgroundImage: posterBackground(title) }}
      className={`relative isolate flex aspect-[2/3] flex-col justify-end overflow-hidden rounded-2xl p-4 ring-1 ring-white/10 ${className}`}
    >
      <span className="font-display pointer-events-none absolute -top-6 -right-3 -z-10 text-[11rem] leading-none font-extrabold text-white/10 select-none">
        {initial}
      </span>
      {tag && (
        <span className="absolute top-3 left-3 rounded-full bg-black/55 px-2.5 py-1 text-[0.7rem] font-semibold tracking-wide text-white backdrop-blur">
          {tag}
        </span>
      )}
      <span className="absolute inset-x-0 bottom-0 -z-10 h-2/3 bg-gradient-to-t from-black/75 to-transparent" />
      <span className="font-display line-clamp-3 text-xl leading-tight font-bold text-balance text-white drop-shadow">
        {title}
      </span>
    </div>
  );
}
