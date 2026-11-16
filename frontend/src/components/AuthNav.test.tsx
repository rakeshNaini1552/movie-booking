import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AuthNav from "./AuthNav";

describe("AuthNav", () => {
  it("signed out: shows Sign in and no Sign out", async () => {
    const user = userEvent.setup();
    const signInAction = vi.fn();
    render(<AuthNav user={null} signInAction={signInAction} signOutAction={vi.fn()} />);

    expect(screen.queryByRole("button", { name: "Sign out" })).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    expect(signInAction).toHaveBeenCalledTimes(1);
  });

  it("signed in: shows the user's name and Sign out, no Sign in", async () => {
    const user = userEvent.setup();
    const signOutAction = vi.fn();
    render(
      <AuthNav user={{ name: "Test User" }} signInAction={vi.fn()} signOutAction={signOutAction} />,
    );

    expect(screen.getByText("Test User")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Sign in" })).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Sign out" }));
    expect(signOutAction).toHaveBeenCalledTimes(1);
  });
});
