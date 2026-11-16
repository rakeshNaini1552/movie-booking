import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { listMovies, redirect } = vi.hoisted(() => ({
  listMovies: vi.fn(),
  redirect: vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  }),
}));
vi.mock("@/lib/catalog/movies", () => ({ listMovies }));
vi.mock("next/navigation", () => ({ redirect }));

import { CatalogError, NotSignedInError } from "@/lib/catalog/errors";
import MoviesContent from "./movies-content";

const movie = (n: number) => ({
  id: `00000000-0000-0000-0000-00000000000${n}`,
  title: `Movie ${n}`,
  durationMinutes: 100,
  language: "en",
  rating: null,
});
const render$ = async (query: Record<string, string | string[]> = {}) =>
  render(await MoviesContent({ searchParams: Promise.resolve(query) }));

describe("MoviesContent", () => {
  beforeEach(() => vi.clearAllMocks());

  it("lists the movies with links, and the first page has no Previous link", async () => {
    listMovies.mockResolvedValue({ movies: [movie(1), movie(2)], hasNext: true });
    await render$();

    expect(listMovies).toHaveBeenCalledWith({ titleContains: "", page: 0, size: 12 });
    expect(screen.getByRole("link", { name: "Movie 1" })).toHaveAttribute(
      "href",
      "/movies/00000000-0000-0000-0000-000000000001",
    );
    expect(screen.queryByRole("link", { name: "Previous" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Next" })).toHaveAttribute("href", "/movies?page=1");
  });

  it("passes the search and page from the URL and keeps the search in the page links", async () => {
    listMovies.mockResolvedValue({ movies: [movie(1)], hasNext: true });
    await render$({ q: "dark", page: "2" });

    expect(listMovies).toHaveBeenCalledWith({ titleContains: "dark", page: 2, size: 12 });
    expect(screen.getByLabelText("Search movies by title")).toHaveValue("dark");
    expect(screen.getByRole("link", { name: "Previous" })).toHaveAttribute(
      "href",
      "/movies?q=dark&page=1",
    );
    expect(screen.getByRole("link", { name: "Next" })).toHaveAttribute(
      "href",
      "/movies?q=dark&page=3",
    );
  });

  it("does not trust a bad page or an over-long search", async () => {
    listMovies.mockResolvedValue({ movies: [], hasNext: false });
    await render$({ q: "x".repeat(500), page: "-5" });
    expect(listMovies).toHaveBeenCalledWith({ titleContains: "", page: 0, size: 12 });
  });

  it("says nothing matched for an empty search result", async () => {
    listMovies.mockResolvedValue({ movies: [], hasNext: false });
    await render$({ q: "zzz" });
    expect(screen.getByText('No movies match "zzz".')).toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  it("says there are no movies for an empty catalog", async () => {
    listMovies.mockResolvedValue({ movies: [], hasNext: false });
    await render$();
    expect(screen.getByText("No movies to show yet.")).toBeInTheDocument();
  });

  it("redirects a signed-out user to sign-in, returning to the same list", async () => {
    listMovies.mockRejectedValue(new NotSignedInError());
    await expect(
      MoviesContent({ searchParams: Promise.resolve({ q: "dark", page: "1" }) }),
    ).rejects.toThrow("NEXT_REDIRECT /signin?callbackUrl=%2Fmovies%3Fq%3Ddark%26page%3D1");
  });

  it("lets a gateway failure reach the error boundary", async () => {
    listMovies.mockRejectedValue(new CatalogError("unreachable"));
    await expect(MoviesContent({ searchParams: Promise.resolve({}) })).rejects.toBeInstanceOf(
      CatalogError,
    );
    expect(redirect).not.toHaveBeenCalled();
  });
});
