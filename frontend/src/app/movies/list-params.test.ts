import { describe, expect, it } from "vitest";
import { listHref, parseListParams } from "./list-params";

describe("parseListParams", () => {
  it("defaults to no search and the first page", () => {
    expect(parseListParams({})).toEqual({ q: "", page: 0 });
  });

  it("reads and trims the search and the page", () => {
    expect(parseListParams({ q: "  dark  ", page: "3" })).toEqual({ q: "dark", page: 3 });
  });

  it("uses the first value when a parameter is repeated", () => {
    expect(parseListParams({ q: ["a", "b"], page: ["2", "5"] })).toEqual({ q: "a", page: 2 });
  });

  it.each(["-1", "abc", "1.5", "", "99999999", "Infinity"])(
    "falls back to page 0 for %j",
    (page) => {
      expect(parseListParams({ page }).page).toBe(0);
    },
  );

  it("drops an over-long search instead of sending it on", () => {
    expect(parseListParams({ q: "x".repeat(101) }).q).toBe("");
    expect(parseListParams({ q: "x".repeat(100) }).q).toHaveLength(100);
  });
});

describe("listHref", () => {
  it("leaves out defaults and encodes the search", () => {
    expect(listHref({ q: "", page: 0 })).toBe("/movies");
    expect(listHref({ q: "a&b", page: 0 })).toBe("/movies?q=a%26b");
    expect(listHref({ q: "dark", page: 2 })).toBe("/movies?q=dark&page=2");
  });
});
