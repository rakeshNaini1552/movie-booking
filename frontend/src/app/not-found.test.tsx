import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import NotFound from "./not-found";

describe("not-found page", () => {
  it("renders its message and a link home", () => {
    render(<NotFound />);
    expect(screen.getByText("Page not found.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Return home" })).toHaveAttribute("href", "/");
  });
});
