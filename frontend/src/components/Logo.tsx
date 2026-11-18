// The mark is a ticket stub: a rounded rectangle with a notch on each side.
export default function Logo() {
  return (
    <span className="flex items-center gap-2.5">
      <svg aria-hidden viewBox="0 0 32 32" className="h-8 w-8">
        <path
          d="M5 7h22a2 2 0 0 1 2 2v4a3 3 0 0 0 0 6v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4a3 3 0 0 0 0-6V9a2 2 0 0 1 2-2Z"
          className="fill-brand"
        />
        <path d="m13 11 8 5-8 5V11Z" className="fill-white" />
      </svg>
      <span className="font-display text-lg font-bold tracking-tight">Movie Booking</span>
    </span>
  );
}
