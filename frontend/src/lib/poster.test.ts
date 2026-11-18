import { describe, expect, it } from "vitest";
import { posterBackground, posterHue } from "./poster";

describe("poster", () => {
  it("gives the same movie the same colour every time, within the hue range", () => {
    expect(posterHue("Inception")).toBe(posterHue("Inception"));
    for (const title of ["Inception", "Interstellar", "12 Angry Men", ""]) {
      expect(posterHue(title)).toBeGreaterThanOrEqual(0);
      expect(posterHue(title)).toBeLessThan(360);
    }
  });

  it("builds a CSS background from the title", () => {
    expect(posterBackground("Inception")).toContain("linear-gradient");
    expect(posterBackground("Inception")).not.toBe(posterBackground("Interstellar"));
  });
});
