import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import MoviesError from "./error";

describe("movies error page", () => {
  it("shows a useful message without the error details, and calls reset on retry", async () => {
    const user = userEvent.setup();
    const reset = vi.fn();
    render(<MoviesError error={new Error("connect ECONNREFUSED 10.0.0.5:8090")} reset={reset} />);

    expect(screen.getByRole("alert")).toHaveTextContent("Something went wrong");
    expect(screen.getByRole("alert")).toHaveTextContent("try again in a moment");
    expect(screen.queryByText(/ECONNREFUSED/)).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Try again" }));
    expect(reset).toHaveBeenCalledTimes(1);
  });

  it("shows the digest as a reference when there is one", () => {
    const error = Object.assign(new Error("x"), { digest: "abc123" });
    render(<MoviesError error={error} reset={() => {}} />);
    expect(screen.getByText("Reference: abc123")).toBeInTheDocument();
  });
});
