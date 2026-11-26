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
