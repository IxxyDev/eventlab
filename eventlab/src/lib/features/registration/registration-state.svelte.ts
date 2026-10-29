import { HttpError } from "#lib/shared/api/http.ts";
import type { RegistrationAccess } from "./registration-access.ts";
import {
  type RegistrationAttempt,
  reuseOrStartAttempt,
} from "./registration-attempt.ts";
import {
  describeRegistrationError,
  isRegistrationRejection,
  registrationErrorMessages,
} from "./registration-errors.ts";
import type {
  Registration,
  RegistrationCreatedResponse,
  RegistrationRequest,
} from "./types.ts";

export type RegistrationApi = {
  createRegistration(
    request: RegistrationRequest,
    idempotencyKey: string,
  ): Promise<RegistrationCreatedResponse>;
  getRegistration(id: string, accessToken: string): Promise<Registration>;
  cancelRegistration(id: string, accessToken: string): Promise<void>;
};

export type RegistrationAccessStore = {
  readRegistrationAccess(): RegistrationAccess | null;
  saveRegistrationAccess(access: RegistrationAccess): void;
  clearRegistrationAccess(): void;
};

export type DisplayedEvent = {
  load(): Promise<void>;
  markRegistrationClosed(): void;
  markSoldOut(): void;
};

type RestoreState =
  | { status: "loading" }
  | { status: "ready" }
  | { status: "error"; message: string };

export class RegistrationState {
  registration = $state<Registration | null>(null);
  submitting = $state(false);
  registrationError = $state("");
  cancelling = $state(false);
  cancelError = $state("");
  cancelNotice = $state("");
  restoreState = $state<RestoreState>({ status: "loading" });
  storageWarning = $state("");

  #access: RegistrationAccess | null = null;
  #pendingAttempt: RegistrationAttempt | null = null;

  readonly #api: RegistrationApi;
  readonly #accessStore: RegistrationAccessStore;
  readonly #event: DisplayedEvent;

  constructor(dependencies: {
    api: RegistrationApi;
    accessStore: RegistrationAccessStore;
    event: DisplayedEvent;
  }) {
    this.#api = dependencies.api;
    this.#accessStore = dependencies.accessStore;
    this.#event = dependencies.event;
  }

  async register(request: RegistrationRequest): Promise<void> {
    if (this.submitting || this.registration) {
      return;
    }

    const name = request.name.trim();
    const email = request.email.trim();

    if (!name || !email) {
      this.registrationError = registrationErrorMessages.REQUIRED_FIELDS;
      return;
    }

    const attempt = reuseOrStartAttempt(this.#pendingAttempt, { name, email });
    this.#pendingAttempt = attempt;

    this.submitting = true;
    this.registrationError = "";
    this.cancelNotice = "";

    try {
      const result = await this.#api.createRegistration(
        attempt.request,
        attempt.idempotencyKey,
      );
      const access: RegistrationAccess = {
        id: result.registration.id,
        accessToken: result.accessToken,
      };

      this.registration = result.registration;
      this.#access = access;
      this.#pendingAttempt = null;
      this.storageWarning = "";

      try {
        this.#accessStore.saveRegistrationAccess(access);
      } catch {
        this.storageWarning =
          "Регистрация создана, но браузер не сохранил ключ доступа. " +
          "После обновления страницы автоматическое восстановление не сработает.";
      }

      await this.#event.load();
    } catch (error) {
      this.registrationError = describeRegistrationError(error);

      if (!isRegistrationRejection(error)) {
        console.error("Ошибка регистрации:", error);
        return;
      }

      if (error.status === 409) {
        if (error.code === "REGISTRATION_CLOSED") {
          this.#event.markRegistrationClosed();
        } else if (error.code === "EVENT_FULL") {
          this.#event.markSoldOut();
        }

        await this.#event.load();
      }
    } finally {
      this.submitting = false;
    }
  }

  async cancel(): Promise<void> {
    if (!this.#access || this.cancelling) {
      return;
    }

    const access = this.#access;
    this.cancelling = true;
    this.cancelError = "";
    let notice = "Регистрация отменена.";

    try {
      await this.#api.cancelRegistration(access.id, access.accessToken);
    } catch (error) {
      if (!(error instanceof HttpError && error.status === 404)) {
        console.error("Ошибка отмены регистрации", error);
        this.cancelError =
          "Не удалось получить ответ сервера, поэтому неизвестно, отменена ли заявка. " +
          "Нажмите «Отменить регистрацию» ещё раз.";
        return;
      }
      notice =
        "Заявка уже недоступна: возможно, ее отменили раньше. " +
        "Сохраненный ключ удален.";
    } finally {
      this.cancelling = false;
    }

    this.registration = null;
    this.#access = null;
    this.storageWarning = "";
    this.cancelNotice = notice;

    try {
      this.#accessStore.clearRegistrationAccess();
    } catch {
      this.storageWarning =
        "Регистрация отменена, но браузер не удалил сохранённый ключ. " +
        "Он больше не действует.";
    }

    await this.#event.load();
  }

  async restore(): Promise<void> {
    this.restoreState = { status: "loading" };

    try {
      const access = this.#accessStore.readRegistrationAccess();

      if (access === null) {
        this.restoreState = { status: "ready" };
        return;
      }

      try {
        this.registration = await this.#api.getRegistration(
          access.id,
          access.accessToken,
        );
        this.#access = access;
      } catch (error) {
        if (error instanceof HttpError && error.status === 404) {
          this.#accessStore.clearRegistrationAccess();
          this.registration = null;
          this.#access = null;
          this.storageWarning =
            "Сохранённая заявка недоступна: " +
            "она отсутствует или ключ больше не подходит.";
          this.restoreState = { status: "ready" };
          return;
        }

        throw error;
      }

      this.restoreState = { status: "ready" };
    } catch {
      this.restoreState = {
        status: "error",
        message:
          "Не удалось восстановить регистрацию. Сохраненный ключ не удален.",
      };
    }
  }
}
