import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import MoviesError from "./error";

describe("movies error page", () => {
  it("shows its message and calls reset when the button is clicked", async () => {
    const user = userEvent.setup();
    const reset = vi.fn();
    render(<MoviesError error={new Error("boom")} reset={reset} />);

    expect(screen.getByRole("alert")).toHaveTextContent("Something went wrong");
    await user.click(screen.getByRole("button", { name: "Try again" }));
    expect(reset).toHaveBeenCalledTimes(1);
  });
});
