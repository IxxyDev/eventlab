import { describe, expect, it } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import type { EventDetails } from "#lib/features/event/types.ts";
import Page from "./+page.svelte";

describe("Catalog page", () => {
  it("should link the event to its page", async () => {
    renderComponent([
      createEvent({
        id: "sound-workshop",
        title: "Как устроен звук на концерте",
        organizationName: "Школа звукорежиссуры «Микшер»",
      }),
    ]);

    await expect
      .element(page.getByRole("link", { name: "Как устроен звук на концерте" }))
      .toHaveAttribute("href", "/events/sound-workshop");
  });

  it("should show the event organizer", async () => {
    renderComponent([
      createEvent({
        id: "stage-lighting",
        title: "Свет на сцене: от пульта до прожектора",
        organizationName: "Лаборатория света «Луч»",
      }),
    ]);

    await expect
      .element(page.getByText("Организатор: Лаборатория света «Луч»"))
      .toBeVisible();
  });

  it("should say that there are no events yet", async () => {
    renderComponent([]);

    await expect.element(page.getByText("Мероприятий пока нет.")).toBeVisible();
  });
});

function renderComponent(events: EventDetails[]) {
  render(Page, { params: {}, data: { events } });
}

function createEvent({
  id,
  title,
  organizationName,
}: Pick<EventDetails, "id" | "title" | "organizationName">): EventDetails {
  return {
    id,
    title,
    organizationName,
    description: "Разберём путь звука от микрофона до колонок.",
    startsAt: "2026-11-14T15:00:00Z",
    endsAt: "2026-11-14T17:00:00Z",
    timeZone: "Europe/Moscow",
    venue: "Пространство «Сцена», зал № 2",
    capacity: 20,
    availableSeats: 20,
    registrationOpen: true,
  };
}
