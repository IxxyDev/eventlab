import type { EventDetails } from "./types.ts";

export class EventState {
  details: EventDetails;
  refreshing = $state(false);
  error = $state("");

  readonly #fetchEvent: FetchEventFn;

  constructor(details: EventDetails, fetchEvent: FetchEventFn) {
    this.details = $state(details);
    this.#fetchEvent = fetchEvent;
  }

  async refresh(): Promise<void> {
    if (this.refreshing) {
      return;
    }
    this.refreshing = true;
    this.error = "";

    try {
      this.details = await this.#fetchEvent();
    } catch (error) {
      console.error("Не удалось обновить мероприятие", error);
      this.error =
        "Не удалось обновить количество мест. Показаны последние полученные данные.";
    } finally {
      this.refreshing = false;
    }
  }

  markRegistrationClosed(): void {
    this.details = { ...this.details, registrationOpen: false };
  }

  markSoldOut(): void {
    this.details = { ...this.details, availableSeats: 0 };
  }
}

type FetchEventFn = () => Promise<EventDetails>;
