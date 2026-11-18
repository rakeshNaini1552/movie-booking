import { expect, test } from "./fixtures";
import { ADMIN, CUSTOMER, signInAs } from "./session";

test("a visitor is sent to sign-in and the home page stays public", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Your seat is waiting");

  await page.goto("/bookings");
  await expect(page).toHaveURL(/\/signin\?callbackUrl=%2Fbookings/);
  await expect(page.getByRole("button", { name: "Sign in with Keycloak" })).toBeVisible();
});

test("a customer cannot see the admin area", async ({ page, context }) => {
  await signInAs(context, CUSTOMER);
  await page.goto("/");
  await expect(page.getByRole("link", { name: "My bookings" }).first()).toBeVisible();
  await expect(page.getByRole("link", { name: "Admin", exact: true })).toHaveCount(0);

  await page.goto("/admin/movies");
  await expect(page.getByText("Page not found.")).toBeVisible();
  await expect(page.getByRole("link", { name: "Add movie" })).toHaveCount(0);
});

test("responses carry the security headers", async ({ page }) => {
  const response = await page.goto("/");
  const headers = response!.headers();
  expect(headers["content-security-policy"]).toContain("frame-ancestors 'none'");
  expect(headers["x-content-type-options"]).toBe("nosniff");
  expect(headers["x-frame-options"]).toBe("DENY");
});

test("an admin adds a movie, a theater with a screen, and a show", async ({ page, context }) => {
  // Queries by role skip hidden elements. Next.js keeps the previous page in the document
  // (hidden) for instant back navigation, so a plain label query could match a field there.
  const text = (label: string) => page.getByRole("textbox", { name: new RegExp(`^${label}`) });
  const number = (label: string) => page.getByRole("spinbutton", { name: new RegExp(`^${label}`) });

  await signInAs(context, ADMIN);
  await page.goto("/");
  await page.getByRole("link", { name: "Admin", exact: true }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Catalog admin" })).toBeVisible();
  await page.getByRole("link", { name: /Add movies, edit their details/ }).click();
  await page.getByRole("link", { name: "Add movie" }).click();

  // An empty form comes back with messages next to the fields.
  await page.getByRole("button", { name: "Add movie" }).click();
  await expect(
    page.getByRole("alert").filter({ hasText: "Please fix the highlighted fields" }),
  ).toBeVisible();

  await text("Title").fill("Test Feature");
  await number("Length in minutes").fill("100");
  await text("Language").fill("English");
  await page.getByRole("button", { name: "Add movie" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Test Feature" })).toBeVisible();
  const movieUrl = page.url().split("?")[0];

  await page.goto("/admin/theaters");
  await text("Name").fill("Test Theater");
  await text("City").fill("Austin");
  await text("Address").fill("1 Test Road");
  await page.getByRole("button", { name: "Add theater" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Test Theater" })).toBeVisible();
  await text("Name").fill("Screen 9");
  await number("Rows").fill("5");
  await number("Seats per row").fill("8");
  await page.getByRole("button", { name: "Add screen" }).click();
  await expect(page.getByText("5 rows of 8 · 40 seats")).toBeVisible();

  await page.goto(movieUrl);
  await page.getByRole("link", { name: "Add show" }).click();
  await page.getByRole("link", { name: /Test Theater/ }).click();
  await page.locator("#startsAt:visible").fill("2026-12-24T19:00");
  await number("Base price").fill("300");
  await page.getByRole("button", { name: "Add show" }).click();
  await expect(page.getByText("Thu, Dec 24, 19:00 UTC")).toBeVisible();

  await page.getByRole("button", { name: "Cancel show" }).click();
  await expect(page.getByText("Cancelled", { exact: true })).toBeVisible();
});
