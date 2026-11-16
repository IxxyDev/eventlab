import type { EventDetails } from "./types.ts";

type LoadEvent = () => Promise<EventDetails>;

export class EventState {
  details: EventDetails;
  refreshing = $state(false);
  error = $state("");

  readonly #loadEvent: LoadEvent;

  constructor(details: EventDetails, loadEvent: LoadEvent) {
    this.details = $state(details);
    this.#loadEvent = loadEvent;
  }

  async load(): Promise<void> {
    if (this.refreshing) {
      return;
    }
    this.refreshing = true;
    this.error = "";

    try {
      this.details = await this.#loadEvent();
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
