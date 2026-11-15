import { render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from "vitest";
import MovieList from "./MovieList";

describe("MovieList", () => {
  // React logs the missing-key warning only once per component, so the spy must be active
  // for every test in this file, whichever one renders first.
  let consoleError: MockInstance<typeof console.error>;
  beforeEach(() => {
    consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
  });
  afterEach(() => {
    // Stable keys: React reports a missing or duplicate key through console.error.
    expect(consoleError).not.toHaveBeenCalled();
    vi.restoreAllMocks();
  });

  it("renders one item per movie with title and duration", () => {
    render(<MovieList />);
    const items = within(screen.getByRole("list")).getAllByRole("listitem");
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent("Inception (148 min)");
    expect(items[1]).toHaveTextContent("Interstellar (169 min)");
    expect(items[2]).toHaveTextContent("The Dark Knight (152 min)");
  });

  it("logs no React key warning when rendering", () => {
    render(<MovieList />);
  });
});
