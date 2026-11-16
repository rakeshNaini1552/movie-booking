import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { auth, redirect } = vi.hoisted(() => ({
  auth: vi.fn(),
  // Next's redirect() throws to stop rendering; mimic that.
  redirect: vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  }),
}));
vi.mock("@/auth", () => ({ auth }));
vi.mock("next/navigation", () => ({ redirect }));

import AccountDetails from "./account-details";

describe("AccountDetails", () => {
  beforeEach(() => vi.clearAllMocks());

  it("redirects to sign-in, returning to /account, when there is no session", async () => {
    auth.mockResolvedValue(null);

    await expect(AccountDetails()).rejects.toThrow("NEXT_REDIRECT");
    expect(redirect).toHaveBeenCalledWith("/signin?callbackUrl=%2Faccount");
  });

  it("shows name, email and roles for a signed-in user", async () => {
    auth.mockResolvedValue({
      expires: "2099-01-01",
      user: { name: "Test User", email: "test@example.com", roles: ["customer", "staff"] },
    });

    render(await AccountDetails());

    expect(screen.getByText("Test User")).toBeInTheDocument();
    expect(screen.getByText("test@example.com")).toBeInTheDocument();
    expect(screen.getByText("customer, staff")).toBeInTheDocument();
    expect(redirect).not.toHaveBeenCalled();
  });
});
