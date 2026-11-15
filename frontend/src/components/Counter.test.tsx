import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import Counter from "./Counter";

describe("Counter", () => {
  it("starts at 0 and goes up by one per click", async () => {
    const user = userEvent.setup();
    render(<Counter />);

    const button = screen.getByRole("button", { name: "Clicked 0 times" });
    await user.click(button);
    expect(button).toHaveTextContent("Clicked 1 times");
    await user.click(button);
    expect(button).toHaveTextContent("Clicked 2 times");
  });
});
