import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { getMovie, notFound, redirect } = vi.hoisted(() => ({
  getMovie: vi.fn(),
  notFound: vi.fn(() => {
    throw new Error("NEXT_NOT_FOUND");
  }),
  redirect: vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  }),
}));
vi.mock("@/lib/catalog/movies", () => ({ getMovie }));
vi.mock("next/navigation", () => ({ notFound, redirect }));

import { NotSignedInError } from "@/lib/gateway/errors";
import MovieDetails from "./movie-details";

const ID = "3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b";
const movie = {
  id: ID,
  title: "Inception",
  description: "A thief steals secrets through dreams.",
  durationMinutes: 148,
  language: "en",
  rating: "PG-13",
  releaseDate: "2010-07-16",
  shows: [
    {
      id: "00000000-0000-0000-0000-000000000001",
      startsAt: "2026-12-01T18:30:00Z",
      basePrice: 12.5,
      status: "SCHEDULED",
      screen: { name: "Screen 1", theater: { id: "t1", name: "Alpha Cinema", city: "Austin" } },
    },
  ],
};
const render$ = async (id: string) =>
  render(await MovieDetails({ params: Promise.resolve({ id }) }));

describe("MovieDetails", () => {
  beforeEach(() => vi.clearAllMocks());

  it("shows the movie and its shows grouped by theater", async () => {
    getMovie.mockResolvedValue(movie);
    await render$(ID);

    expect(screen.getByRole("heading", { level: 1, name: "Inception" })).toBeInTheDocument();
    expect(screen.getByRole("list", { name: "Movie facts" })).toHaveTextContent(
      "148 minenPG-13Released 2010-07-16",
    );
    expect(screen.getByText("A thief steals secrets through dreams.")).toBeInTheDocument();
    expect(screen.getByRole("region", { name: /Alpha Cinema/ })).toHaveTextContent(
      "Tue, Dec 1, 18:30 UTC",
    );
  });

  it("shows a clear message for a movie with no shows", async () => {
    getMovie.mockResolvedValue({ ...movie, shows: [] });
    await render$(ID);
    expect(screen.getByText("No shows are scheduled for this movie yet.")).toBeInTheDocument();
  });

  it("copes with a movie that has no description, rating or release date", async () => {
    getMovie.mockResolvedValue({ ...movie, description: null, rating: null, releaseDate: null });
    await render$(ID);
    expect(screen.getByRole("list", { name: "Movie facts" })).toHaveTextContent(/^148 minen$/);
  });

  it("returns 404 when the movie does not exist", async () => {
    getMovie.mockResolvedValue(null);
    await expect(MovieDetails({ params: Promise.resolve({ id: ID }) })).rejects.toThrow(
      "NEXT_NOT_FOUND",
    );
  });

  it("returns 404 for an invalid id without calling the catalog", async () => {
    await expect(MovieDetails({ params: Promise.resolve({ id: "1; drop" }) })).rejects.toThrow(
      "NEXT_NOT_FOUND",
    );
    expect(getMovie).not.toHaveBeenCalled();
  });

  it("redirects a signed-out user to sign-in, returning to this movie", async () => {
    getMovie.mockRejectedValue(new NotSignedInError());
    await expect(MovieDetails({ params: Promise.resolve({ id: ID }) })).rejects.toThrow(
      `NEXT_REDIRECT /signin?callbackUrl=%2Fmovies%2F${ID}`,
    );
  });
});
