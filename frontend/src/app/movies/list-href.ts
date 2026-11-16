// Kept apart from list-params.ts so the client search form does not pull Zod into the browser bundle.
// Link target for a list page, keeping the search text; page 0 is the default so it is left out.
export function listHref({ q, page }: { q: string; page: number }): string {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (page > 0) params.set("page", String(page));
  const query = params.toString();
  return query ? `/movies?${query}` : "/movies";
}
