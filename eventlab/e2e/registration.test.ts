import AxeBuilder from "@axe-core/playwright";
import { expect, type Page, test } from "@playwright/test";
import type { RegistrationCreatedResponse } from "../src/lib/features/registration/types.ts";
import { resetDatabase } from "./reset-database.ts";

async function fillRegistration(page: Page): Promise<string> {
  const email = `e2e-${crypto.randomUUID()}@example.com`;

  await page.getByRole("textbox", { name: "Имя" }).fill("Анна");
  await page.getByRole("textbox", { name: "Email" }).fill(email);

  return email;
}

async function register(page: Page, eventId: string): Promise<string> {
  await page.goto(`/events/${eventId}`);
  const email = await fillRegistration(page);
  await page.getByRole("button", { name: "Записаться" }).click();
  await page.getByRole("heading", { name: "Вы зарегистрированы" }).waitFor();

  return email;
}

test.beforeEach(resetDatabase);

test("should restore the registration after reload", async ({ page }) => {
  const email = await register(page, "sound-workshop");

  await page.reload();

  await expect(
    page.getByRole("heading", { name: "Вы зарегистрированы" }),
  ).toBeVisible();
  await expect(page.getByText(`Email: ${email}`)).toBeVisible();
});

test("should show the form again after cancelling", async ({ page }) => {
  await register(page, "sound-workshop");

  await page.getByRole("button", { name: "Отменить регистрацию" }).click();

  await expect(page.getByText("Регистрация отменена.")).toBeVisible();
  await expect(page.getByRole("button", { name: "Записаться" })).toBeVisible();
});

test("should create one registration when the response is lost", async ({
  page,
}) => {
  let lostRegistrationId: string | null = null;
  await page.route(
    "**/api/events/sound-workshop/registrations",
    async (route) => {
      if (route.request().method() !== "POST" || lostRegistrationId !== null) {
        await route.fallback();
        return;
      }

      const response = await route.fetch();
      const created: RegistrationCreatedResponse = await response.json();
      lostRegistrationId = created.registration.id;
      await route.abort("connectionreset");
    },
  );
  await page.goto("/events/sound-workshop");
  await fillRegistration(page);
  await page.getByRole("button", { name: "Записаться" }).click();
  await page.getByRole("alert").waitFor();

  await page.getByRole("button", { name: "Записаться" }).click();

  await expect(
    page.getByText(`Номер заявки: ${lostRegistrationId}`),
  ).toBeVisible();
});

test("should have no accessibility violations on the registration form", async ({
  page,
}) => {
  await page.goto("/events/sound-workshop");
  await page.getByRole("button", { name: "Записаться" }).waitFor();

  const results = await new AxeBuilder({ page }).analyze();

  expect(results.violations).toEqual([]);
});

test("should register for another event with the same email", async ({
  page,
}) => {
  const email = await register(page, "sound-workshop");

  await page.goto("/events/stage-lighting");
  await page.getByRole("textbox", { name: "Имя" }).fill("Анна");
  await page.getByRole("textbox", { name: "Email" }).fill(email);
  await page.getByRole("button", { name: "Записаться" }).click();

  await expect(
    page.getByRole("heading", { name: "Вы зарегистрированы" }),
  ).toBeVisible();
});

test("should restore the first registration after registering for another event", async ({
  page,
}) => {
  const email = await register(page, "sound-workshop");
  await register(page, "stage-lighting");

  await page.goto("/events/sound-workshop");

  await expect(
    page.getByText(`Email: ${email}`, { exact: true }),
  ).toBeVisible();
});

test("should restore the second registration after reload", async ({
  page,
}) => {
  await register(page, "sound-workshop");
  const email = await register(page, "stage-lighting");

  await page.reload();

  await expect(
    page.getByText(`Email: ${email}`, { exact: true }),
  ).toBeVisible();
});

test("should keep another event registration after cancelling", async ({
  page,
}) => {
  const email = await register(page, "sound-workshop");
  await register(page, "stage-lighting");

  await page.getByRole("button", { name: "Отменить регистрацию" }).click();
  await page.getByText("Регистрация отменена.", { exact: true }).waitFor();
  await page.goto("/events/sound-workshop");

  await expect(
    page.getByText(`Email: ${email}`, { exact: true }),
  ).toBeVisible();
});
