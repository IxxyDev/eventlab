import type { ComponentProps } from "svelte";
import { describe, expect, it } from "vitest";
import { page } from "vitest/browser";
import { render } from "vitest-browser-svelte";
import EventTimeAndPlace from "./EventTimeAndPlace.svelte";

describe("EventTimeAndPlace", () => {
  it("should show the time as local to the event", async () => {
    renderComponent({
      startsAt: "2026-11-14T15:00:00Z",
      endsAt: "2026-11-14T17:00:00Z",
      timeZone: "Europe/Moscow",
    });

    await expect
      .element(
        page.getByText("14 ноября 2026 г., 18:00–20:00 по местному времени."),
      )
      .toBeVisible();
  });

  it("should show the venue", async () => {
    renderComponent({ venue: "Пространство «Сцена», зал № 2" });

    await expect
      .element(page.getByText("Пространство «Сцена», зал № 2."))
      .toBeVisible();
  });

  function renderComponent(
    overrides: Partial<ComponentProps<typeof EventTimeAndPlace>["event"]>,
  ) {
    render(EventTimeAndPlace, {
      event: {
        startsAt: "2026-11-14T15:00:00Z",
        endsAt: "2026-11-14T17:00:00Z",
        timeZone: "Europe/Moscow",
        venue: "Пространство «Сцена», зал № 2",
        ...overrides,
      },
    });
  }
});
