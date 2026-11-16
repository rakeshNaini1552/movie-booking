// The catalog has no time zone per theater, so every start time is shown in UTC and says so.
const showTime = new Intl.DateTimeFormat("en-US", {
  weekday: "short",
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
  timeZone: "UTC",
});

// One place that formats show start times. Input is an ISO-8601 instant ("2026-12-01T18:30:00Z").
export function formatShowTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Time unavailable";
  return `${showTime.format(date)} UTC`;
}

// The catalog does not say which currency basePrice is in, so no symbol is added.
const price = new Intl.NumberFormat("en-US", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatPrice(amount: number): string {
  return price.format(amount);
}
