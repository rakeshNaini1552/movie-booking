import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { GatewayError } from "@/lib/gateway/errors";
import SeatsError from "./error";

const withDigest = (digest?: string) =>
  Object.assign(new Error("An error occurred in the Server Components render."), { digest });

describe("seats error page", () => {
  it("calls reset on retry", async () => {
    const reset = vi.fn();
    render(<SeatsError error={withDigest()} reset={reset} />);
    await userEvent.setup().click(screen.getByRole("button", { name: "Try again" }));
    expect(reset).toHaveBeenCalledTimes(1);
  });

  it.each([
    ["timeout", "took too long"],
    ["unreachable", "could not reach"],
    ["http", "loading the seat map"],
  ] as const)("shows a short message for a %s failure, no internals", (kind, text) => {
    render(<SeatsError error={withDigest(new GatewayError(kind).digest)} reset={() => {}} />);
    expect(screen.getByRole("alert")).toHaveTextContent(text);
    expect(screen.queryByText(/Reference|GATEWAY/)).not.toBeInTheDocument();
  });

  it("shows the digest as a reference for any other error", () => {
    render(<SeatsError error={withDigest("12345")} reset={() => {}} />);
    expect(screen.getByText("Reference: 12345")).toBeInTheDocument();
  });
});
