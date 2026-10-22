package dev.eventlab.api

enum class RegistrationErrorCode {
  REQUIRED_FIELDS,
  INPUT_TOO_LONG,
  INVALID_EMAIL,
  EMAIL_ALREADY_REGISTERED,
  EVENT_FULL,
}

class RegistrationConflictException(
  val code: RegistrationErrorCode,
) : RuntimeException(code.name)

class RegistrationValidationException(
  val code: RegistrationErrorCode,
) : RuntimeException(code.name)
