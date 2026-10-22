package dev.eventlab.api

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
}
