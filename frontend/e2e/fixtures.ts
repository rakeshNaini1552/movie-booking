import AxeBuilder from "@axe-core/playwright";
import { expect, test as base, type Page } from "@playwright/test";
import { MOCK_GATEWAY_URL } from "./settings";

// Ids of the data the mock gateway starts with (see reset() in mock-gateway.mjs).
export const MOVIE_ID = "00000000-0000-4000-8000-000000000001";
export const MOVIE_TITLE = "The Lighthouse Keeper";
export const SHOW_ID = "00000000-0000-4000-8000-000000000300";
export const SEATS_URL = `/movies/${MOVIE_ID}/shows/${SHOW_ID}/seats`;

// Every test starts from the same data.
export const test = base.extend({
  page: async ({ page }, run) => {
    await fetch(`${MOCK_GATEWAY_URL}/__reset`, { method: "POST" });
    await run(page);
  },
});

// Fails the test when the page has an accessibility problem axe can detect automatically
// (missing labels, low contrast, wrong roles). It does not replace checking with a keyboard.
export async function expectAccessible(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();
  // Each problem is reported with the elements it was found on, so the failure says where to look.
  const problems = results.violations.flatMap((violation) =>
    violation.nodes.map((node) => `${violation.id}: ${node.target.join(" ")}`),
  );
  expect(problems).toEqual([]);
}

export { expect };
