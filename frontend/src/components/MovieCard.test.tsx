import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import MovieCard from "./MovieCard";

describe("MovieCard", () => {
  it("shows the title as a heading and the duration", () => {
    render(<MovieCard title="Inception" durationMinutes={148} />);
    expect(screen.getByRole("heading", { name: "Inception" })).toBeInTheDocument();
    expect(screen.getByText("148 min")).toBeInTheDocument();
  });
});
