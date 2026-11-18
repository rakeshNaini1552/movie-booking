"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Button from "@/components/Button";
import { listHref } from "@/app/movies/list-href";

export const DEBOUNCE_MS = 300;

type MovieSearchFormProps = {
  defaultValue: string;
};

// A plain GET form first: without JavaScript, Search submits ?q=... to /movies. With JavaScript the
// effect below also updates the URL while typing. It only changes the address; the server page
// re-renders and makes the gateway call, so no token or gateway URL is ever in this bundle.
export default function MovieSearchForm({ defaultValue }: MovieSearchFormProps) {
  const router = useRouter();
  const [text, setText] = useState(defaultValue);

  useEffect(() => {
    const trimmed = text.trim();
    // Nothing to do when the text already matches the URL: no navigation on first render, and
    // none after the navigation this effect caused (the page re-renders with the new default).
    if (trimmed === defaultValue) return;
    const timer = setTimeout(() => {
      router.replace(listHref({ q: trimmed, page: 0 }));
    }, DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [text, defaultValue, router]);

  return (
    <form action="/movies" method="get" role="search" className="mb-10 flex max-w-xl gap-2">
      <label htmlFor="movie-search" className="sr-only">
        Search movies by title
      </label>
      <input
        id="movie-search"
        name="q"
        type="search"
        value={text}
        onChange={(event) => setText(event.target.value)}
        maxLength={100}
        placeholder="Search by title"
        className="border-line bg-panel focus:border-brand-soft min-w-0 flex-1 rounded-full border px-5 py-2.5 text-sm placeholder:text-zinc-500"
      />
      <Button type="submit">Search</Button>
    </form>
  );
}
