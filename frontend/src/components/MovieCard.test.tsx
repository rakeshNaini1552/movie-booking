import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import MovieCard from "./MovieCard";

describe("MovieCard", () => {
  it("shows the title as a heading and the duration", () => {
    render(<MovieCard title="Inception" durationMinutes={148} />);
    expect(screen.getByRole("heading", { name: "Inception" })).toBeInTheDocument();
    expect(screen.getByText("148 min")).toBeInTheDocument();
  });

  it("adds language and rating, and skips a missing rating", () => {
    const { rerender } = render(
      <MovieCard title="Inception" durationMinutes={148} language="en" rating="PG-13" />,
    );
    expect(screen.getByText("148 min · en · PG-13")).toBeInTheDocument();
    rerender(<MovieCard title="Inception" durationMinutes={148} language="en" rating={null} />);
    expect(screen.getByText("148 min · en")).toBeInTheDocument();
  });

  it("links the title when given an href", () => {
    render(<MovieCard title="Inception" durationMinutes={148} href="/movies/abc" />);
    expect(screen.getByRole("link", { name: "Inception" })).toHaveAttribute("href", "/movies/abc");
  });
});
