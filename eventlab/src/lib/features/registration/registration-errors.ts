import { HttpError } from "#lib/shared/api/http.ts";

type RegistrationErrorCode =
  | "REQUIRED_FIELDS"
  | "INPUT_TOO_LONG"
  | "INVALID_EMAIL"
  | "INVALID_IDEMPOTENCY_KEY"
  | "EMAIL_ALREADY_REGISTERED"
  | "EVENT_FULL"
  | "REGISTRATION_CLOSED"
  | "IDEMPOTENCY_KEY_REUSED";

const retryAfterReloadMessage =
  "Не удалось отправить заявку. Обновите страницу и попробуйте снова.";

export const registrationErrorMessages: Record<RegistrationErrorCode, string> =
  {
    REQUIRED_FIELDS: "Укажите имя и email.",
    INPUT_TOO_LONG:
      "Имя должно быть не длиннее 100 символов, email — не длиннее 254.",
    INVALID_EMAIL: "Укажите email в формате name@example.com.",
    INVALID_IDEMPOTENCY_KEY: retryAfterReloadMessage,
    EMAIL_ALREADY_REGISTERED: "Этот email уже зарегистрирован на мероприятие.",
    EVENT_FULL: "Свободные места закончились.",
    REGISTRATION_CLOSED: "Регистрация закрыта: мероприятие уже началось.",
    IDEMPOTENCY_KEY_REUSED: retryAfterReloadMessage,
  };

const unknownConflictMessage =
  "Регистрация отклонена. Обновите данные и попробуйте снова.";
const unknownValidationMessage = "Проверьте имя и email.";
const unknownOutcomeMessage =
  "Не удалось получить подтверждение от сервера. " +
  "Нажмите «Записаться» ещё раз — вторая заявка не создастся.";

export function isRegistrationRejection(error: unknown): error is HttpError {
  return (
    error instanceof HttpError &&
    (error.status === 400 || error.status === 409 || error.status === 422)
  );
}

function isRegistrationErrorCode(code: string): code is RegistrationErrorCode {
  return Object.hasOwn(registrationErrorMessages, code);
}

export function describeRegistrationError(error: unknown): string {
  if (!isRegistrationRejection(error)) {
    return unknownOutcomeMessage;
  }

  if (error.code !== undefined && isRegistrationErrorCode(error.code)) {
    return registrationErrorMessages[error.code];
  }

  return error.status === 409
    ? unknownConflictMessage
    : unknownValidationMessage;
}
