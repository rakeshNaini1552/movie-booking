import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

// AuthStatus is an async Server Component that reads the session; Navbar's own job is the links.
vi.mock("@/components/AuthStatus", () => ({ default: () => <div>auth area</div> }));

import Navbar from "./Navbar";

describe("Navbar", () => {
  it("shows the site name and links to Home and Movies", () => {
    render(<Navbar />);
    const nav = screen.getByRole("navigation", { name: "Main" });
    expect(nav).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Movie Booking" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "Movies" })).toHaveAttribute("href", "/movies");
  });

  it("renders the auth area inside the navigation", () => {
    render(<Navbar />);
    expect(screen.getByRole("navigation", { name: "Main" })).toHaveTextContent("auth area");
  });
});
