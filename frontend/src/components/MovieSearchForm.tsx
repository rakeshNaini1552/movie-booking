import Button from "@/components/Button";

type MovieSearchFormProps = {
  defaultValue: string;
};

// A plain GET form: the search text lands in the URL (?q=...), so it works without JavaScript
// and a result page can be bookmarked. Submitting always starts on the first page.
export default function MovieSearchForm({ defaultValue }: MovieSearchFormProps) {
  return (
    <form action="/movies" method="get" role="search" className="mb-6 flex flex-wrap gap-2">
      <label htmlFor="movie-search" className="sr-only">
        Search movies by title
      </label>
      <input
        id="movie-search"
        name="q"
        type="search"
        defaultValue={defaultValue}
        maxLength={100}
        placeholder="Search by title"
        className="min-w-0 flex-1 rounded-md border border-zinc-400 bg-transparent px-3 py-2 text-sm dark:border-zinc-600"
      />
      <Button type="submit">Search</Button>
    </form>
  );
}
