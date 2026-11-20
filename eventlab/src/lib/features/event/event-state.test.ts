import { describe, expect, it } from "vitest";
import { EventState } from "./event-state.svelte.ts";
import type { EventDetails } from "./types.ts";

const event: EventDetails = {
  id: "sound-workshop",
  title: "Звук в кино",
  description: "Лекция и мастер-класс.",
  startsAt: "2026-11-14T15:00:00Z",
  endsAt: "2026-11-14T17:00:00Z",
  timeZone: "Europe/Moscow",
  venue: "Москва, Лаборатория звука",
  capacity: 1,
  availableSeats: 1,
  registrationOpen: true,
};

describe("EventState", () => {
  it("should replace the event with the refreshed one", async () => {
    const state = new EventState(event, fetchSoldOutEvent);

    await state.refresh();

    expect(state.details.availableSeats).toEqual(0);
  });

  it("should keep the shown event when a refresh fails", async () => {
    const state = new EventState(event, loseEvent);

    await state.refresh();

    expect(state.details).toEqual(event);
  });

  it("should warn that seats are not refreshed when a refresh fails", async () => {
    const state = new EventState(event, loseEvent);

    await state.refresh();

    expect(state.error).toEqual(
      "Не удалось обновить количество мест. Показаны последние полученные данные.",
    );
  });

  it.each([
    { mark: "markRegistrationClosed", changed: { registrationOpen: false } },
    { mark: "markSoldOut", changed: { availableSeats: 0 } },
  ] as const)(
    "should change only its own field on $mark",
    ({ mark, changed }) => {
      const state = new EventState(event, async () => event);

      state[mark]();

      expect(state.details).toEqual({ ...event, ...changed });
    },
  );

  async function fetchSoldOutEvent(): Promise<EventDetails> {
    return { ...event, availableSeats: 0 };
  }

  async function loseEvent(): Promise<EventDetails> {
    throw new TypeError("Failed to fetch");
  }
});
