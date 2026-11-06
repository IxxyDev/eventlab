import { describe, expect, it, vi } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import type { EventDetails } from "#lib/features/event/types.ts";
import Page from "./+page.svelte";

describe("Event page", () => {
  it("should say that registration is closed once the event has started", async () => {
    mockEvent({ registrationOpen: false });

    render(Page);

    await expect
      .element(page.getByText("Регистрация закрыта: мероприятие уже началось."))
      .toBeVisible();
  });
});

function mockEvent(overrides: Partial<EventDetails>) {
  const event: EventDetails = {
    id: "sound-workshop",
    title: "Как устроен звук на концерте",
    description: "Разберём путь звука от микрофона до колонок.",
    startsAt: "2026-11-14T15:00:00Z",
    endsAt: "2026-11-14T17:00:00Z",
    timeZone: "Europe/Moscow",
    venue: "Пространство «Сцена», зал № 2",
    capacity: 20,
    availableSeats: 20,
    registrationOpen: true,
    ...overrides,
  };

  vi.stubGlobal(
    "fetch",
    vi.fn(async () => Response.json(event)),
  );
}
