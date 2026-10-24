import AxeBuilder from "@axe-core/playwright";
import { expect, type Page, test } from "@playwright/test";
import type { RegistrationCreatedResponse } from "../src/lib/api/types.ts";

const storageKey = "eventlab:registration-access:sound-workshop";

async function fillRegistration(page: Page): Promise<string> {
  const email = `e2e-${crypto.randomUUID()}@example.com`;
  await page.getByRole("textbox", { name: "Имя" }).fill("Анна");
  await page.getByRole("textbox", { name: "Email" }).fill(email);

  return email;
}

async function register(page: Page): Promise<string> {
  await page.goto("/");
  const email = await fillRegistration(page);
  await page.getByRole("button", { name: "Записаться" }).click();
  await expect(
    page.getByRole("heading", { name: "Вы зарегистрированы" }),
  ).toBeVisible();

  return email;
}

test.afterEach(async ({ page, request }) => {
  const saved = await page.evaluate(
    (key) => localStorage.getItem(key),
    storageKey,
  );

  if (saved === null) {
    return;
  }

  const access: { id: string; accessToken: string } = JSON.parse(saved);
  await request.delete(`http://localhost:8080/api/registrations/${access.id}`, {
    headers: { Authorization: `Bearer ${access.accessToken}` },
  });
});

test("should restore the registration after reload", async ({ page }) => {
  const email = await register(page);

  await page.reload();

  await expect(
    page.getByRole("heading", { name: "Вы зарегистрированы" }),
  ).toBeVisible();
  await expect(page.getByText(`Email: ${email}`)).toBeVisible();
});

test("should show the form again after cancelling", async ({ page }) => {
  await register(page);

  await page.getByRole("button", { name: "Отменить регистрацию" }).click();

  await expect(page.getByText("Регистрация отменена.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Записаться" })).toBeVisible();
});

test("should create one registration when the response is lost", async ({
  page,
}) => {
  let lostRegistrationId: string | null = null;
  await page.route("**/api/event/registrations", async (route) => {
    if (route.request().method() !== "POST" || lostRegistrationId !== null) {
      await route.fallback();
      return;
    }

    const response = await route.fetch();
    const created: RegistrationCreatedResponse = await response.json();
    lostRegistrationId = created.registration.id;
    await route.abort("connectionreset");
  });
  await page.goto("/");
  await fillRegistration(page);
  await page.getByRole("button", { name: "Записаться" }).click();
  await expect(page.getByRole("alert")).toHaveText(
    "Не удалось получить подтверждение от сервера. " +
      "Нажмите «Записаться» ещё раз — вторая заявка не создастся.",
  );

  await page.getByRole("button", { name: "Записаться" }).click();

  await expect(
    page.getByText(`Номер заявки: ${lostRegistrationId}`),
  ).toBeVisible();
});

test("should have no accessibility violations on the registration form", async ({
  page,
}) => {
  await page.goto("/");
  await expect(page.getByRole("button", { name: "Записаться" })).toBeVisible();

  const results = await new AxeBuilder({ page }).analyze();

  expect(results.violations).toEqual([]);
});
