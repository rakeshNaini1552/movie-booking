import { z } from "zod";

export const PAGE_SIZE = 12;

const MAX_PAGE = 10_000;
const MAX_QUERY_LENGTH = 100;

const first = (value: unknown) => (Array.isArray(value) ? value[0] : value);

// Query parameters come from the address bar, so they are validated, not trusted. Anything
// invalid falls back to a safe value (first page, no search) instead of failing the page.
const listParamsSchema = z.object({
  q: z.preprocess(
    first,
    z
      .string()
      .transform((s) => s.trim())
      .pipe(z.string().max(MAX_QUERY_LENGTH))
      .catch(""),
  ),
  page: z.preprocess(first, z.coerce.number().int().min(0).max(MAX_PAGE).catch(0)),
});

export type ListParams = { q: string; page: number };

export function parseListParams(raw: Record<string, unknown>): ListParams {
  return listParamsSchema.parse({ q: raw.q ?? "", page: raw.page ?? 0 });
}

// Link target for a list page, keeping the search text; page 0 is the default so it is left out.
export function listHref({ q, page }: ListParams): string {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (page > 0) params.set("page", String(page));
  const query = params.toString();
  return query ? `/movies?${query}` : "/movies";
}
