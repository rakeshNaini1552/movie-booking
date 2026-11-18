import { describe, expect, it } from "vitest";
import { qrMatrix } from "./qr";

describe("qrMatrix", () => {
  it("returns a square grid and one path made of unit squares", () => {
    const { size, path } = qrMatrix("3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b.signature");
    expect(size).toBeGreaterThanOrEqual(21);
    expect(path).toMatch(/^(M\d+ \d+h1v1h-1z)+$/);
    // The top-left corner of every QR code is a dark module (part of the finder pattern).
    expect(path.startsWith("M0 0h1v1h-1z")).toBe(true);
  });

  it("gives a different code for a different payload", () => {
    expect(qrMatrix("ticket-a").path).not.toBe(qrMatrix("ticket-b").path);
  });
});
