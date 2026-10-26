import type { EventDetails } from "./types.ts";

type LoadEvent = () => Promise<EventDetails>;

export class EventState {
  details = $state<EventDetails | null>(null);
  loading = $state(true);
  refreshing = $state(false);
  error = $state("");

  readonly #loadEvent: LoadEvent;

  constructor(loadEvent: LoadEvent) {
    this.#loadEvent = loadEvent;
  }

  async load(): Promise<void> {
    if (this.refreshing) {
      return;
    }
    this.loading = this.details === null;
    this.refreshing = this.details !== null;
    this.error = "";

    try {
      this.details = await this.#loadEvent();
    } catch (error) {
      console.error("Не удалось загрузить мероприятие", error);
      this.error =
        this.details === null
          ? "Не удалось загрузить мероприятие. Попробуйте позже."
          : "Не удалось обновить количество мест. Показаны последние полученные данные.";
    } finally {
      this.loading = false;
      this.refreshing = false;
    }
  }

  markRegistrationClosed(): void {
    if (this.details) {
      this.details = { ...this.details, registrationOpen: false };
    }
  }

  markSoldOut(): void {
    if (this.details) {
      this.details = { ...this.details, availableSeats: 0 };
    }
  }
}
