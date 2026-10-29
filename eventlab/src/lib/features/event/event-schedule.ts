import type { EventDetails } from "./types.ts";

type EventSchedule = Pick<EventDetails, "startsAt" | "endsAt" | "timeZone">;

export function formatEventSchedule(schedule: EventSchedule): string {
  const formatter = new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: schedule.timeZone,
  });

  return formatter.formatRange(
    new Date(schedule.startsAt),
    new Date(schedule.endsAt),
  );
}
