import type { ComponentProps } from "svelte";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import * as registrationAccess from "../registration-access.ts";
import * as registrationApi from "../registration-api.ts";
import { RegistrationState } from "../registration-state.svelte.ts";
import RegistrationSection from "./RegistrationSection.svelte";

describe("RegistrationSection", () => {
  const access = {
    id: "9c55e2a7-3f6c-4b9e-8a41-0d7f2b1c6e30",
    accessToken: "q7Vd2LxN9tRb4KsY1mPz8WcF3hJg6TaE0uQoXi5nDwM",
  };

  beforeEach(() => {
    localStorage.clear();
  });

  it("should say that registration is closed once the event has started", async () => {
    renderComponent({ registrationOpen: false });

    await expect
      .element(page.getByText("Регистрация закрыта: мероприятие уже началось."))
      .toBeVisible();
  });

  it("should say that registration is closed when no seats are left", async () => {
    renderComponent({ availableSeats: 0 });

    await expect
      .element(page.getByText("Свободные места закончились. Запись закрыта."))
      .toBeVisible();
  });

  it("should offer to recheck the registration when the server cannot be reached", async () => {
    registrationAccess.saveRegistrationAccess("sound-workshop", access);
    vi.stubGlobal("fetch", loseResponse);

    renderComponent();

    await expect
      .element(
        page.getByRole("button", { name: "Повторить проверку регистрации" }),
      )
      .toBeVisible();
  });

  function renderComponent(
    overrides: Partial<
      ComponentProps<typeof RegistrationSection>["event"]
    > = {},
  ) {
    const registrationState = new RegistrationState("sound-workshop", {
      api: registrationApi,
      accessStore: registrationAccess,
      event: {
        refresh: vi.fn(),
        markRegistrationClosed: vi.fn(),
        markSoldOut: vi.fn(),
      },
    });

    render(RegistrationSection, {
      registrationState,
      event: { registrationOpen: true, availableSeats: 20, ...overrides },
    });
  }

  async function loseResponse(): Promise<Response> {
    throw new TypeError("Failed to fetch");
  }
});
