import type { ComponentProps } from "svelte";
import { describe, expect, it, vi } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import type { RegistrationRequest } from "#lib/api/types.ts";
import RegistrationForm from "./RegistrationForm.svelte";

function renderForm(
  overrides: Partial<ComponentProps<typeof RegistrationForm>> = {},
) {
  const onRegister = vi.fn<(request: RegistrationRequest) => Promise<void>>();

  render(RegistrationForm, {
    submitting: false,
    registrationError: "",
    onRegister,
    ...overrides,
  });

  return onRegister;
}

describe("RegistrationForm", () => {
  it("should send the entered name and email", async () => {
    const onRegister = renderForm();
    await page.getByRole("textbox", { name: "Имя" }).fill("Анна");
    await page.getByRole("textbox", { name: "Email" }).fill("anna@example.com");

    await page.getByRole("button", { name: "Записаться" }).click();

    expect(onRegister.mock.calls).toEqual([
      [{ name: "Анна", email: "anna@example.com" }],
    ]);
  });

  it.each([
    {
      reason: "the name has only spaces",
      name: "   ",
      email: "anna@example.com",
    },
    { reason: "the email has no domain", name: "Анна", email: "anna" },
  ])("should not send the form when $reason", async ({ name, email }) => {
    const onRegister = renderForm();
    await page.getByRole("textbox", { name: "Имя" }).fill(name);
    await page.getByRole("textbox", { name: "Email" }).fill(email);

    await page.getByRole("button", { name: "Записаться" }).click();

    expect(onRegister.mock.calls).toEqual([]);
  });

  it("should lock the form while the request is being sent", async () => {
    renderForm({ submitting: true });

    const button = page.getByRole("button", { name: "Отправляем заявку…" });

    await expect.element(button).toBeDisabled();
    await expect
      .element(page.getByRole("textbox", { name: "Имя" }))
      .toBeDisabled();
  });

  it("should announce the registration error", async () => {
    renderForm({ registrationError: "Свободные места закончились." });

    const form = page.getByRole("form", { name: "Запись на мероприятие" });

    await expect
      .element(page.getByRole("alert"))
      .toHaveTextContent("Свободные места закончились.");
    await expect
      .element(form)
      .toHaveAccessibleDescription("Свободные места закончились.");
  });
});
