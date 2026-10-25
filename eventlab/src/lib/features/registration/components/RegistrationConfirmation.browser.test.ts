import type { ComponentProps } from "svelte";
import { describe, expect, it, vi } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import RegistrationConfirmation from "./RegistrationConfirmation.svelte";

const registration = {
  id: "registration-1",
  name: "Анна",
  email: "anna@example.com",
};

function renderConfirmation(
  overrides: Partial<ComponentProps<typeof RegistrationConfirmation>> = {},
) {
  const onCancel = vi.fn<() => Promise<void>>();

  render(RegistrationConfirmation, {
    registration,
    cancelling: false,
    cancelError: "",
    onCancel,
    ...overrides,
  });

  return onCancel;
}

describe("RegistrationConfirmation", () => {
  it("should show the confirmed registration with its number", async () => {
    renderConfirmation();

    const confirmation = page.getByRole("status");

    await expect
      .element(confirmation)
      .toHaveTextContent(
        "Вы зарегистрированы " +
          "Анна, сервер подтвердил вашу регистрацию. " +
          "Email: anna@example.com " +
          "Номер заявки: registration-1",
      );
  });

  it("should ask to cancel once per click", async () => {
    const onCancel = renderConfirmation();

    await page.getByRole("button", { name: "Отменить регистрацию" }).click();

    expect(onCancel).toHaveBeenCalledOnce();
  });

  it("should lock the cancel button while cancelling", async () => {
    renderConfirmation({ cancelling: true });

    const button = page.getByRole("button", { name: "Отменяем регистрацию…" });

    await expect.element(button).toBeDisabled();
  });

  it("should announce the cancel error", async () => {
    renderConfirmation({ cancelError: "Не удалось получить ответ сервера." });

    const alert = page.getByRole("alert");

    await expect
      .element(alert)
      .toHaveTextContent("Не удалось получить ответ сервера.");
  });
});
