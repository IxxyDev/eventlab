import { describe, expect, it } from "vitest";
import { formatEventSchedule } from "./event-schedule.ts";

describe("formatEventSchedule", () => {
  it("should show the time in the event time zone", () => {
    const schedule = {
      startsAt: "2026-11-14T15:00:00Z",
      endsAt: "2026-11-14T17:00:00Z",
      timeZone: "Asia/Yekaterinburg",
    };

    const formatted = formatEventSchedule(schedule);

    expect(formatted).toEqual("14 ноября 2026 г., 20:00–22:00");
  });

  it("should show both dates when the event ends on another day", () => {
    const schedule = {
      startsAt: "2026-11-14T15:00:00Z",
      endsAt: "2026-11-15T09:00:00Z",
      timeZone: "Europe/Moscow",
    };

    const formatted = formatEventSchedule(schedule);

    expect(formatted).toEqual(
      "14 ноября 2026 г. в 18:00 – 15 ноября 2026 г. в 12:00",
    );
  });
});
