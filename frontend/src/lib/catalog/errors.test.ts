import { describe, expect, it } from "vitest";
import { CatalogError, catalogErrorMessage, isCatalogDigest } from "./errors";

describe("CatalogError digest", () => {
  it("carries the failure kind, so the error page can pick a message", () => {
    expect(new CatalogError("timeout").digest).toBe("CATALOG_TIMEOUT");
    expect(new CatalogError("invalid-response").digest).toBe("CATALOG_INVALID_RESPONSE");
  });
});

describe("catalogErrorMessage", () => {
  it("has a different message for timeout, unreachable and everything else", () => {
    const messages = new Set([
      catalogErrorMessage("CATALOG_TIMEOUT"),
      catalogErrorMessage("CATALOG_UNREACHABLE"),
      catalogErrorMessage("CATALOG_HTTP"),
    ]);
    expect(messages.size).toBe(3);
    expect(catalogErrorMessage(undefined)).toBe(catalogErrorMessage("CATALOG_GRAPHQL"));
    expect(catalogErrorMessage("12345")).toBe(catalogErrorMessage("CATALOG_HTTP"));
  });

  it("recognises catalog digests", () => {
    expect(isCatalogDigest("CATALOG_HTTP")).toBe(true);
    expect(isCatalogDigest("1234567")).toBe(false);
    expect(isCatalogDigest(undefined)).toBe(false);
  });
});
