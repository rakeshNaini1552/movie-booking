import { beforeEach, describe, expect, it, vi } from "vitest";

const { redirect } = vi.hoisted(() => ({
  redirect: vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  }),
}));
vi.mock("next/navigation", () => ({ redirect }));

import { CatalogError, NotSignedInError } from "@/lib/catalog/errors";
import { requireSignIn } from "./require-sign-in";

describe("requireSignIn", () => {
  beforeEach(() => vi.clearAllMocks());

  it("returns the result of the call", async () => {
    await expect(requireSignIn("/movies", async () => 42)).resolves.toBe(42);
    expect(redirect).not.toHaveBeenCalled();
  });

  it("redirects to sign-in with the encoded return path when signed out", async () => {
    await expect(
      requireSignIn("/movies?q=a&page=2", () => Promise.reject(new NotSignedInError())),
    ).rejects.toThrow("NEXT_REDIRECT /signin?callbackUrl=%2Fmovies%3Fq%3Da%26page%3D2");
  });

  it("lets other errors through to the error boundary", async () => {
    const failure = new CatalogError("timeout");
    await expect(requireSignIn("/movies", () => Promise.reject(failure))).rejects.toBe(failure);
    expect(redirect).not.toHaveBeenCalled();
  });
});
