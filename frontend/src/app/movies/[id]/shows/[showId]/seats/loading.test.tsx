import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import Loading from "./loading";

describe("seats loading page", () => {
  it("renders its message", () => {
    render(<Loading />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading seats...");
  });
});
