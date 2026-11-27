import { expect, test } from "@playwright/test";

test("should open the selected event from the catalog", async ({ page }) => {
  await page.goto("/");

  await page
    .getByRole("link", {
      name: "Свет на сцене: от пульта до прожектора",
      exact: true,
    })
    .click();

  await expect(page).toHaveURL("/events/stage-lighting");
  await expect(
    page.getByRole("heading", {
      name: "Свет на сцене: от пульта до прожектора",
      level: 1,
      exact: true,
    }),
  ).toBeVisible();
});

test.describe("server responses", () => {
  test.use({ javaScriptEnabled: false });

  test("should render the catalog without JavaScript", async ({ page }) => {
    await page.goto("/");

    await expect(
      page.getByRole("link", {
        name: "Свет на сцене: от пульта до прожектора",
        exact: true,
      }),
    ).toBeVisible();
  });

  test("should render the event without JavaScript", async ({ page }) => {
    await page.goto("events/stage-lighting");

    await expect(
      page.getByRole("heading", {
        name: "Свет на сцене: от пульта до прожектора",
        level: 1,
        exact: true,
      }),
    ).toBeVisible();
  });

  test("should return HTTP 404 for a missing event", async ({ request }) => {
    const response = await request.get("events/vinyl-mastering");

    expect(response.status()).toEqual(404);
  });
});
