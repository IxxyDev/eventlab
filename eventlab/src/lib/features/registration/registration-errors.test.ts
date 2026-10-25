import { describe, expect, it } from "vitest";
import { HttpError } from "#lib/shared/api/http.ts";
import { describeRegistrationError } from "./registration-errors.ts";

describe("describeRegistrationError", () => {
  it.each([
    {
      status: 409,
      code: "EMAIL_ALREADY_REGISTERED",
      expected: "Этот email уже зарегистрирован на мероприятие.",
    },
    {
      status: 400,
      code: "INVALID_IDEMPOTENCY_KEY",
      expected:
        "Не удалось отправить заявку. Обновите страницу и попробуйте снова.",
    },
    {
      status: 422,
      code: "IDEMPOTENCY_KEY_REUSED",
      expected:
        "Не удалось отправить заявку. Обновите страницу и попробуйте снова.",
    },
  ])(
    "should describe $code with its own message",
    ({ status, code, expected }) => {
      const message = describeRegistrationError(new HttpError(status, code));

      expect(message).toEqual(expected);
    },
  );

  it.each([
    {
      status: 409,
      code: "SEATS_ON_HOLD",
      expected: "Регистрация отклонена. Обновите данные и попробуйте снова.",
    },
    {
      status: 409,
      code: "toString",
      expected: "Регистрация отклонена. Обновите данные и попробуйте снова.",
    },
    { status: 400, code: undefined, expected: "Проверьте имя и email." },
  ])(
    "should fall back to a general message for $status with code $code",
    ({ status, code, expected }) => {
      const message = describeRegistrationError(new HttpError(status, code));

      expect(message).toEqual(expected);
    },
  );

  it.each([
    { cause: "server error", error: new HttpError(500) },
    { cause: "lost connection", error: new TypeError("Failed to fetch") },
  ])("should ask to retry after a $cause", ({ error }) => {
    const message = describeRegistrationError(error);

    expect(message).toEqual(
      "Не удалось получить подтверждение от сервера. " +
        "Нажмите «Записаться» ещё раз — вторая заявка не создастся.",
    );
  });
});
