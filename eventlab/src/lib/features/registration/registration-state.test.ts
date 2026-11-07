import { describe, expect, it, vi } from "vitest";
import { HttpError } from "#lib/shared/api/http.ts";
import type { RegistrationAccess } from "./registration-access.ts";
import {
  type RegistrationAccessStore,
  type RegistrationApi,
  RegistrationState,
} from "./registration-state.svelte.ts";

const request = { name: "Анна", email: "anna@example.com" };
const registration = { id: "registration-1", ...request };
const access = { id: "registration-1", accessToken: "token-1" };

describe("RegistrationState", () => {
  describe("register", () => {
    it("should trim the name and email before sending", async () => {
      const { state, api } = createState();

      await state.register({
        name: "  Анна ",
        email: " anna@example.com  ",
      });

      const [[, sentRequest]] = api.createRegistration.mock.calls;
      expect(sentRequest).toEqual({ name: "Анна", email: "anna@example.com" });
    });

    it("should register for the event it was created with", async () => {
      const { state, api } = createStateWithEventId("stage-lighting");

      await state.register(request);

      const [[eventId]] = api.createRegistration.mock.calls;
      expect(eventId).toEqual("stage-lighting");
    });

    it("should remember the registration the server accepted", async () => {
      const { state } = createState();

      await state.register(request);

      expect(state.registration).toEqual(registration);
    });

    it("should save the issued access for the event it was created with", async () => {
      const { state, accessStore } = createStateWithEventId("stage-lighting");

      await state.register(request);

      expect(accessStore.saveRegistrationAccess.mock.calls).toEqual([
        ["stage-lighting", { id: "registration-1", accessToken: "token-1" }],
      ]);
    });

    it("should reload the event after registration", async () => {
      const { state, event } = createState();

      await state.register(request);

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

      const [[, , firstKey], [, , secondKey]] =
        api.createRegistration.mock.calls;
      expect(secondKey).toEqual(firstKey);
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
    it("should cancel with the saved access", async () => {
      const { state, api } = createState(access);
      await state.restore();

      await state.cancel();

      expect(api.cancelRegistration.mock.calls).toEqual([
        ["registration-1", "token-1"],
      ]);
    });

    it("should forget the registration after cancelling", async () => {
      const { state } = createState(access);
      await state.restore();

      await state.cancel();

      expect(state.registration).toEqual(null);
    });

    it("should notify about the cancellation", async () => {
      const { state } = createState(access);
      await state.restore();

      await state.cancel();

      expect(state.cancelNotice).toEqual("Регистрация отменена.");
    });

    it("should clear the saved access of the event it was created with", async () => {
      const { state, accessStore } = createState(access, "stage-lighting");
      await state.restore();

      await state.cancel();

      expect(accessStore.clearRegistrationAccess.mock.calls).toEqual([
        ["stage-lighting"],
      ]);
    });

    it("should reload the event after cancelling", async () => {
      const { state, event } = createState(access);
      await state.restore();

      await state.cancel();

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
      expect(accessStore.clearRegistrationAccess.mock.calls).toHaveLength(1);
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
    it("should read the saved access of the event it was created with", async () => {
      const { state, accessStore } = createStateWithEventId("stage-lighting");

      await state.restore();

      expect(accessStore.readRegistrationAccess.mock.calls).toEqual([
        ["stage-lighting"],
      ]);
    });

    it("should restore the saved registration", async () => {
      const { state, api } = createState(access);

      await state.restore();

      expect(api.getRegistration.mock.calls).toEqual([
        ["registration-1", "token-1"],
      ]);
      expect(state.registration).toEqual(registration);
      expect(state.restoreState).toEqual({ status: "ready" });
    });

    it("should clear a saved key the server no longer accepts", async () => {
      const { state, api, accessStore } = createState(access, "stage-lighting");
      api.getRegistration.mockRejectedValueOnce(new HttpError(404));

      await state.restore();

      expect(accessStore.clearRegistrationAccess.mock.calls).toEqual([
        ["stage-lighting"],
      ]);
    });

    it("should warn about a saved registration the server no longer has", async () => {
      const { state, api } = createState(access);
      api.getRegistration.mockRejectedValueOnce(new HttpError(404));

      await state.restore();

      expect(state.storageWarning).toEqual(
        "Сохранённая заявка недоступна: " +
          "она отсутствует или ключ больше не подходит.",
      );
    });

    it("should finish restoring when the server no longer has the registration", async () => {
      const { state, api } = createState(access);
      api.getRegistration.mockRejectedValueOnce(new HttpError(404));

      await state.restore();

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

    it("should finish restoring when the saved access cannot be read", async () => {
      const { state } = createStateWithUnreadableAccess();

      await state.restore();

      expect(state.restoreState).toEqual({ status: "ready" });
    });

    it("should warn when the saved access cannot be read", async () => {
      const { state } = createStateWithUnreadableAccess();

      await state.restore();

      expect(state.storageWarning).toEqual(
        "Не удалось прочитать сохранённую заявку. " +
          "Если вы не записывались, это предупреждение можно не учитывать.",
      );
    });

    it("should clear the saved access that cannot be read", async () => {
      const { state, accessStore } =
        createStateWithUnreadableAccess("stage-lighting");

      await state.restore();

      expect(accessStore.clearRegistrationAccess.mock.calls).toEqual([
        ["stage-lighting"],
      ]);
    });

    it("should finish restoring when the storage is blocked", async () => {
      const { state } = createStateWithBlockedStorage();

      await state.restore();

      expect(state.restoreState).toEqual({ status: "ready" });
    });
  });
});

function createState(
  saved: RegistrationAccess | null = null,
  eventId = "sound-workshop",
) {
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
    readRegistrationAccess: vi.fn<
      RegistrationAccessStore["readRegistrationAccess"]
    >(() => saved),
    saveRegistrationAccess:
      vi.fn<RegistrationAccessStore["saveRegistrationAccess"]>(),
    clearRegistrationAccess:
      vi.fn<RegistrationAccessStore["clearRegistrationAccess"]>(),
  };
  const event = {
    load: vi.fn(async () => {}),
    markRegistrationClosed: vi.fn(),
    markSoldOut: vi.fn(),
  };
  const state = new RegistrationState(eventId, { api, accessStore, event });

  return { state, api, accessStore, event };
}

function createStateWithEventId(eventId: string) {
  return createState(null, eventId);
}

function createStateWithUnreadableAccess(eventId = "sound-workshop") {
  const created = createState(null, eventId);
  created.accessStore.readRegistrationAccess.mockImplementation(() => {
    throw new Error("Saved registration access cannot be read");
  });

  return created;
}

function createStateWithBlockedStorage() {
  const created = createStateWithUnreadableAccess();
  created.accessStore.clearRegistrationAccess.mockImplementation(() => {
    throw new DOMException("Storage is blocked", "SecurityError");
  });

  return created;
}
