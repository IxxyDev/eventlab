package dev.eventlab.api.web

import dev.eventlab.api.registration.IdempotencyKeyReusedException
import dev.eventlab.api.registration.RegistrationConflictException
import dev.eventlab.api.registration.RegistrationErrorCode
import dev.eventlab.api.registration.RegistrationErrorResponse
import dev.eventlab.api.registration.RegistrationValidationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler(RegistrationConflictException::class)
  @ResponseStatus(HttpStatus.CONFLICT)
  fun handleRegistrationConflict(
    exception: RegistrationConflictException,
  ): RegistrationErrorResponse =
    RegistrationErrorResponse(code = exception.code)

  @ExceptionHandler(RegistrationValidationException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun handleRegistrationValidation(
    exception: RegistrationValidationException,
  ): RegistrationErrorResponse =
    RegistrationErrorResponse(code = exception.code)

  @ExceptionHandler(IdempotencyKeyReusedException::class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
  fun handleIdempotencyKeyReused(): RegistrationErrorResponse =
    RegistrationErrorResponse(
      code = RegistrationErrorCode.IDEMPOTENCY_KEY_REUSED,
    )
}
