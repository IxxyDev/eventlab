import { describe, expect, it, vi } from "vitest";
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
  it("should show an error when the first load fails", async () => {
    const state = new EventState(async () => {
      throw new TypeError("Failed to fetch");
    });

    await state.load();

    expect(state.details).toEqual(null);
    expect(state.error).toEqual(
      "Не удалось загрузить мероприятие. Попробуйте позже.",
    );
  });

  it("should keep the last event when a refresh fails", async () => {
    const loadEvent = vi
      .fn<() => Promise<EventDetails>>()
      .mockResolvedValueOnce(event)
      .mockRejectedValueOnce(new TypeError("Failed to fetch"));
    const state = new EventState(loadEvent);
    await state.load();

    await state.load();

    expect(state.details).toEqual(event);
    expect(state.error).toEqual(
      "Не удалось обновить количество мест. Показаны последние полученные данные.",
    );
  });

  it.each([
    { mark: "markRegistrationClosed", changed: { registrationOpen: false } },
    { mark: "markSoldOut", changed: { availableSeats: 0 } },
  ] as const)(
    "should change only its own field on $mark",
    async ({ mark, changed }) => {
      const state = new EventState(async () => event);
      await state.load();

      state[mark]();

      expect(state.details).toEqual({ ...event, ...changed });
    },
  );
});
