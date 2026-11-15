import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import Header from "./Header";

describe("Header", () => {
  it("renders the title prop as a level-1 heading", () => {
    render(<Header title="All Movies" />);
    expect(screen.getByRole("heading", { level: 1, name: "All Movies" })).toBeInTheDocument();
  });
});
