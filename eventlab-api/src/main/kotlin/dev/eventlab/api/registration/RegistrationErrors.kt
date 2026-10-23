package dev.eventlab.api.registration

enum class RegistrationErrorCode {
  REQUIRED_FIELDS,
  INPUT_TOO_LONG,
  INVALID_EMAIL,
  INVALID_IDEMPOTENCY_KEY,
  EMAIL_ALREADY_REGISTERED,
  EVENT_FULL,
  REGISTRATION_CLOSED,
  IDEMPOTENCY_KEY_REUSED,
}

class RegistrationConflictException(
  val code: RegistrationErrorCode,
) : RuntimeException(code.name)

class RegistrationValidationException(
  val code: RegistrationErrorCode,
) : RuntimeException(code.name)

class IdempotencyKeyReusedException :
  RuntimeException(RegistrationErrorCode.IDEMPOTENCY_KEY_REUSED.name)
