import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import Home from "./page";

describe("home page", () => {
  it("links to the movie list", () => {
    render(<Home />);
    expect(screen.getByRole("heading", { name: "Movie Booking" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Browse movies" })).toHaveAttribute("href", "/movies");
  });
});
