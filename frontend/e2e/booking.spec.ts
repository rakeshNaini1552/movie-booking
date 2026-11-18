import { expect, expectAccessible, MOVIE_TITLE, SEATS_URL, test } from "./fixtures";
import { CUSTOMER, signInAs } from "./session";

test.beforeEach(async ({ context }) => {
  await signInAs(context, CUSTOMER);
});

test("a customer books seats, pays and gets a ticket", async ({ page }) => {
  await page.goto("/movies");
  await page.getByRole("link", { name: MOVIE_TITLE }).click();
  await expect(page.getByRole("heading", { level: 1, name: MOVIE_TITLE })).toBeVisible();

  await page.getByRole("link", { name: /Select seats for .* at Aurora Cinemas/ }).click();
  await page.getByRole("button", { name: "Seat E5, Platinum, available" }).click();
  await page.getByRole("button", { name: "Seat E6, Platinum, available" }).click();
  await expect(page.getByText("Selected 2 of 6: E5, E6")).toBeVisible();
  await page.getByRole("button", { name: "Continue to booking" }).click();

  await expect(page).toHaveURL(/\/bookings\/[0-9a-f-]{36}$/);
  await expect(page.getByText("Awaiting payment")).toBeVisible();
  await expect(page.getByText("500.00").first()).toBeVisible();

  // A declined card leaves the booking payable.
  await page.getByLabel(/Test card that declines/).check();
  await page.getByRole("button", { name: "Pay 500.00" }).click();
  await expect(page.getByRole("alert").filter({ hasText: "declined" })).toBeVisible();

  await page.getByLabel(/Test card that approves/).check();
  await page.getByRole("button", { name: "Pay 500.00" }).click();
  await expect(page.getByText("Confirmed", { exact: true })).toBeVisible();
  await expect(page.getByRole("img", { name: "Ticket QR code" })).toBeVisible();

  await page.goto("/bookings");
  await expect(page.getByRole("link", { name: MOVIE_TITLE })).toBeVisible();
  await expect(page.getByText("E5, E6")).toBeVisible();
});

test("seats taken by a booking are no longer selectable", async ({ page }) => {
  await page.goto(SEATS_URL);
  await page.getByRole("button", { name: "Seat A1, Silver, available" }).click();
  await page.getByRole("button", { name: "Continue to booking" }).click();
  await expect(page).toHaveURL(/\/bookings\//);

  await page.goto(SEATS_URL);
  await expect(page.getByRole("button", { name: "Seat A1, Silver, held" })).toBeDisabled();
});

test("a customer cancels a booking", async ({ page }) => {
  await page.goto(SEATS_URL);
  await page.getByRole("button", { name: "Seat B2, Silver, available" }).click();
  await page.getByRole("button", { name: "Continue to booking" }).click();
  await expect(page).toHaveURL(/\/bookings\//);

  await page.getByRole("button", { name: "Cancel booking" }).click();
  await page.getByRole("button", { name: "Yes, cancel booking" }).click();
  await expect(page.getByText("Cancelled", { exact: true })).toBeVisible();
  await expect(page.getByRole("link", { name: "Book again" })).toBeVisible();

  await page.goto(SEATS_URL);
  await expect(page.getByRole("button", { name: "Seat B2, Silver, available" })).toBeEnabled();
});

test("search narrows the movie list while typing", async ({ page }) => {
  await page.goto("/movies");
  await page.getByRole("searchbox", { name: "Search movies by title" }).fill("monsoon");
  await expect(page).toHaveURL(/q=monsoon/);
  await expect(page.getByRole("link", { name: "The Last Monsoon" })).toBeVisible();
  await expect(page.getByRole("link", { name: MOVIE_TITLE })).toHaveCount(0);
});

test("the customer pages have no detectable accessibility problems", async ({ page }) => {
  for (const path of ["/", "/movies", SEATS_URL, "/bookings", "/account"]) {
    await page.goto(path);
    await expect(page.getByRole("main")).toBeVisible();
    await page.waitForLoadState("networkidle");
    await expectAccessible(page);
  }
});
