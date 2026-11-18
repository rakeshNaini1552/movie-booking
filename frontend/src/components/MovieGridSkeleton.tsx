// Grey placeholder cards shown while the movie list loads, so the page does not jump when it arrives.
export default function MovieGridSkeleton({ label }: { label: string }) {
  return (
    <div role="status">
      <span className="sr-only">{label}</span>
      <ul aria-hidden className="grid grid-cols-2 gap-x-5 gap-y-8 sm:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 8 }, (_, index) => (
          <li key={index}>
            <div className="from-panel via-raised to-panel animate-shimmer aspect-[2/3] rounded-2xl bg-gradient-to-r bg-[length:200%_100%]" />
            <div className="bg-panel mt-3 h-4 w-3/4 rounded" />
            <div className="bg-panel mt-2 h-3 w-1/2 rounded" />
          </li>
        ))}
      </ul>
    </div>
  );
}
