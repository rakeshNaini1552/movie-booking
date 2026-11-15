import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import Loading from "./loading";

describe("movies loading page", () => {
  it("renders its message", () => {
    render(<Loading />);
    expect(screen.getByText("Loading movies...")).toBeInTheDocument();
  });
});
