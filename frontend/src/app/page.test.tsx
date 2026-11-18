import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

// NowShowing reads the session and calls the catalog; the page test covers the static part.
vi.mock("./now-showing", () => ({ default: () => null }));

import Home from "./page";

describe("home page", () => {
  it("links to the movie list", () => {
    render(<Home />);
    expect(
      screen.getByRole("heading", { level: 1, name: "Your seat is waiting." }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Browse movies" })).toHaveAttribute("href", "/movies");
  });
});
