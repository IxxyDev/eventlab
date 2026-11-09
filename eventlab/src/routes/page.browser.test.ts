import { describe, expect, it } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import type { EventDetails } from "#lib/features/event/types.ts";
import Page from "./+page.svelte";

describe("Catalog page", () => {
  it("should link the event to its page", async () => {
    renderComponent();

    await expect
      .element(page.getByRole("link", { name: "Как устроен звук на концерте" }))
      .toHaveAttribute("href", "/events/sound-workshop");
  });
});

function renderComponent() {
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
  };

  render(Page, { params: {}, data: { events: [event] } });
}
