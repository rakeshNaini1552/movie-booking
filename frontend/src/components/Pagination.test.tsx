import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import Pagination from "./Pagination";

describe("Pagination", () => {
  it("renders both links", () => {
    render(<Pagination previousHref="/movies" nextHref="/movies?page=2" />);
    expect(screen.getByRole("link", { name: "Previous" })).toHaveAttribute("href", "/movies");
    expect(screen.getByRole("link", { name: "Next" })).toHaveAttribute("href", "/movies?page=2");
  });

  it("renders only the link that applies", () => {
    render(<Pagination previousHref={null} nextHref="/movies?page=1" />);
    expect(screen.queryByRole("link", { name: "Previous" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Next" })).toBeInTheDocument();
  });

  it("renders nothing when there is nowhere to go", () => {
    const { container } = render(<Pagination previousHref={null} nextHref={null} />);
    expect(container).toBeEmptyDOMElement();
  });
});
