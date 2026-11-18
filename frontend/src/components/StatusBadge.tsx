import type { Tone } from "@/lib/bookings/status";

const tones: Record<Tone, string> = {
  amber: "border-amber-500/50 bg-amber-950/60 text-amber-200",
  green: "border-emerald-500/50 bg-emerald-950/60 text-emerald-200",
  red: "border-red-500/50 bg-red-950/60 text-red-200",
  zinc: "border-line bg-raised text-zinc-300",
};

// The status is always written out, the colour only supports it.
export default function StatusBadge({ label, tone }: { label: string; tone: Tone }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold ${tones[tone]}`}
    >
      {label}
    </span>
  );
}
