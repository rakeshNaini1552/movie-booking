import { describe, expect, it } from "vitest";
import { formatPrice, formatShowTime } from "./format";

describe("formatShowTime", () => {
  it("formats an instant in UTC and says so", () => {
    expect(formatShowTime("2026-12-01T18:30:00Z")).toBe("Tue, Dec 1, 18:30 UTC");
  });

  it("uses UTC whatever the offset in the input", () => {
    expect(formatShowTime("2026-12-01T20:30:00+02:00")).toBe("Tue, Dec 1, 18:30 UTC");
  });

  it("does not print Invalid Date for garbage", () => {
    expect(formatShowTime("soon")).toBe("Time unavailable");
  });
});

describe("formatPrice", () => {
  it("always shows two decimals and groups thousands", () => {
    expect(formatPrice(12.5)).toBe("12.50");
    expect(formatPrice(1234)).toBe("1,234.00");
  });
});
