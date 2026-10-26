import { describe, expect, it, vi } from "vitest";
import { HttpError } from "#lib/shared/api/http.ts";
import type { RegistrationAccess } from "./registration-access.ts";
import {
  type RegistrationApi,
  RegistrationState,
} from "./registration-state.svelte.ts";

const request = { name: "Анна", email: "anna@example.com" };
const registration = { id: "registration-1", ...request };
const access = { id: "registration-1", accessToken: "token-1" };

describe("RegistrationState", () => {
  describe("register", () => {
    it("should remember the registration the server accepted", async () => {
      const { state, api, accessStore, event } = createState();

      await state.register({
        name: "  Анна ",
        email: " anna@example.com  ",
      });

      expect(api.createRegistration.mock.calls[0][0]).toEqual(request);
      expect(state.registration).toEqual(registration);
      expect(accessStore.saveRegistrationAccess.mock.calls).toEqual([[access]]);
      expect(event.load.mock.calls).toEqual([[]]);
    });

    it("should ask to press again when the response is lost", async () => {
      const { state, api } = createState();
      api.createRegistration.mockRejectedValueOnce(
        new TypeError("Failed to fetch"),
      );

      await state.register(request);

      expect(state.registrationError).toEqual(
        "Не удалось получить подтверждение от сервера. " +
          "Нажмите «Записаться» ещё раз — вторая заявка не создастся.",
      );
      expect(state.submitting).toEqual(false);
    });

    it("should reuse the idempotency key after a lost response", async () => {
      const { state, api } = createState();
      api.createRegistration.mockRejectedValueOnce(
        new TypeError("Failed to fetch"),
      );
      await state.register(request);

      await state.register(request);

      const [first, second] = api.createRegistration.mock.calls;
      expect(second[1]).toEqual(first[1]);
      expect(state.registration).toEqual(registration);
    });

    it.each([
      { code: "REGISTRATION_CLOSED", mark: "markRegistrationClosed" },
      { code: "EVENT_FULL", mark: "markSoldOut" },
    ] as const)(
      "should call $mark when the server answers $code",
      async ({ code, mark }) => {
        const { state, api, event } = createState();
        api.createRegistration.mockRejectedValueOnce(new HttpError(409, code));

        await state.register(request);

        expect(event[mark].mock.calls).toEqual([[]]);
        expect(event.load.mock.calls).toEqual([[]]);
        expect(state.registration).toEqual(null);
      },
    );

    it("should warn when the browser does not save the access", async () => {
      const { state, accessStore } = createState();
      accessStore.saveRegistrationAccess.mockImplementation(() => {
        throw new DOMException("Storage is full", "QuotaExceededError");
      });

      await state.register(request);

      expect(state.registration).toEqual(registration);
      expect(state.storageWarning).toEqual(
        "Регистрация создана, но браузер не сохранил ключ доступа. " +
          "После обновления страницы автоматическое восстановление не сработает.",
      );
    });
  });

  describe("cancel", () => {
    it("should forget the registration after cancelling", async () => {
      const { state, api, accessStore, event } = createState(access);
      await state.restore();

      await state.cancel();

      expect(api.cancelRegistration.mock.calls).toEqual([
        ["registration-1", "token-1"],
      ]);
      expect(state.registration).toEqual(null);
      expect(state.cancelNotice).toEqual("Регистрация отменена.");
      expect(accessStore.clearRegistrationAccess.mock.calls).toEqual([[]]);
      expect(event.load.mock.calls).toEqual([[]]);
    });

    it("should forget a registration the server no longer has", async () => {
      const { state, api, accessStore } = createState(access);
      api.cancelRegistration.mockRejectedValueOnce(new HttpError(404));
      await state.restore();

      await state.cancel();

      expect(state.registration).toEqual(null);
      expect(state.cancelNotice).toEqual(
        "Заявка уже недоступна: возможно, ее отменили раньше. " +
          "Сохраненный ключ удален.",
      );
      expect(accessStore.clearRegistrationAccess.mock.calls).toEqual([[]]);
    });

    it("should keep the registration when the response is lost", async () => {
      const { state, api, accessStore } = createState(access);
      api.cancelRegistration.mockRejectedValueOnce(
        new TypeError("Failed to fetch"),
      );
      await state.restore();

      await state.cancel();

      expect(state.registration).toEqual(registration);
      expect(state.cancelError).toEqual(
        "Не удалось получить ответ сервера, поэтому неизвестно, отменена ли заявка. " +
          "Нажмите «Отменить регистрацию» ещё раз.",
      );
      expect(accessStore.clearRegistrationAccess.mock.calls).toEqual([]);
    });
  });

  describe("restore", () => {
    it("should restore the saved registration", async () => {
      const { state, api } = createState(access);

      await state.restore();

      expect(api.getRegistration.mock.calls).toEqual([
        ["registration-1", "token-1"],
      ]);
      expect(state.registration).toEqual(registration);
      expect(state.restoreState).toEqual({ status: "ready" });
    });

    it("should drop a saved registration the server no longer has", async () => {
      const { state, api, accessStore } = createState(access);
      api.getRegistration.mockRejectedValueOnce(new HttpError(404));

      await state.restore();

      expect(state.registration).toEqual(null);
      expect(accessStore.clearRegistrationAccess.mock.calls).toEqual([[]]);
      expect(state.storageWarning).toEqual(
        "Сохранённая заявка недоступна: " +
          "она отсутствует или ключ больше не подходит.",
      );
      expect(state.restoreState).toEqual({ status: "ready" });
    });

    it("should keep the saved key when the server cannot be reached", async () => {
      const { state, api, accessStore } = createState(access);
      api.getRegistration.mockRejectedValueOnce(
        new TypeError("Failed to fetch"),
      );

      await state.restore();

      expect(state.restoreState).toEqual({
        status: "error",
        message:
          "Не удалось восстановить регистрацию. Сохраненный ключ не удален.",
      });
      expect(accessStore.clearRegistrationAccess.mock.calls).toEqual([]);
    });
  });
});

function createState(saved: RegistrationAccess | null = null) {
  const api = {
    createRegistration: vi.fn<RegistrationApi["createRegistration"]>(
      async () => ({ registration, accessToken: access.accessToken }),
    ),
    getRegistration: vi.fn<RegistrationApi["getRegistration"]>(
      async () => registration,
    ),
    cancelRegistration: vi.fn<RegistrationApi["cancelRegistration"]>(
      async () => {},
    ),
  };
  const accessStore = {
    readRegistrationAccess: vi.fn(() => saved),
    saveRegistrationAccess: vi.fn<(access: RegistrationAccess) => void>(),
    clearRegistrationAccess: vi.fn(),
  };
  const event = {
    load: vi.fn(async () => {}),
    markRegistrationClosed: vi.fn(),
    markSoldOut: vi.fn(),
  };
  const state = new RegistrationState({ api, accessStore, event });

  return { state, api, accessStore, event };
}
