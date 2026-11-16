import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import LoadingMessage from "./LoadingMessage";

describe("LoadingMessage", () => {
  it("is announced as a status", () => {
    render(<LoadingMessage>Loading...</LoadingMessage>);
    expect(screen.getByRole("status")).toHaveTextContent("Loading...");
  });
});
