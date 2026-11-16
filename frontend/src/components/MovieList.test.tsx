import { render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from "vitest";
import MovieList from "./MovieList";

const movies = [
  {
    id: "00000000-0000-0000-0000-000000000001",
    title: "Inception",
    durationMinutes: 148,
    language: "en",
    rating: "PG-13",
  },
  {
    id: "00000000-0000-0000-0000-000000000002",
    title: "Interstellar",
    durationMinutes: 169,
    language: "en",
    rating: null,
  },
];

describe("MovieList", () => {
  let consoleError: MockInstance<typeof console.error>;
  beforeEach(() => {
    consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
  });
  afterEach(() => {
    // Stable keys: React reports a missing or duplicate key through console.error.
    expect(consoleError).not.toHaveBeenCalled();
    vi.restoreAllMocks();
  });

  it("renders one linked item per movie", () => {
    render(<MovieList movies={movies} />);
    const items = within(screen.getByRole("list")).getAllByRole("listitem");
    expect(items).toHaveLength(2);
    expect(items[0]).toHaveTextContent("Inception148 min · en · PG-13");
    expect(screen.getByRole("link", { name: "Interstellar" })).toHaveAttribute(
      "href",
      "/movies/00000000-0000-0000-0000-000000000002",
    );
  });
});
