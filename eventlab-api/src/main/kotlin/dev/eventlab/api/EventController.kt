package dev.eventlab.api

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

data class EventResponse(
  val id: String,
  val title: String,
  val description: String,
  val dateLabel: String,
  val timeLabel: String,
  val venue: String,
  val capacity: Int,
  val availableSeats: Int,
)

data class RegistrationRequest(val name: String, val email: String)

data class RegistrationResponse(
  val id: String,
  val name: String,
  val email: String,
)

data class RegistrationCreatedResponse(
  val registration: RegistrationResponse,
  val accessToken: String,
)

data class StoredRegistration(
  val registration: RegistrationResponse,
  val accessToken: String,
)

enum class RegistrationErrorCode {
  REQUIRED_FIELDS,
  INPUT_TOO_LONG,
  INVALID_EMAIL,
  EMAIL_ALREADY_REGISTERED,
  EVENT_FULL,
}

data class RegistrationErrorResponse(val code: RegistrationErrorCode)

class RegistrationConflictException(val code: RegistrationErrorCode) :
  RuntimeException(code.name)

class RegistrationValidationException(val code: RegistrationErrorCode) :
  RuntimeException(code.name)

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class EventController(private val eventService: EventService) {
  @PostMapping("/api/event/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  fun register(
    @RequestBody request: RegistrationRequest,
  ): RegistrationCreatedResponse = eventService.register(request)

  @GetMapping("/api/event")
  fun getEvent(): EventResponse = eventService.getEvent()

  @GetMapping("/api/registrations/{id}")
  fun getRegistration(
    @PathVariable("id") id: String,
    @RequestHeader(
      value = "Authorization",
      required = false,
    ) authorization: String?,
  ): RegistrationResponse {
    val token =
      authorization
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substring(7)
        ?.trim()

    return eventService.getRegistration(id, token)
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  }

  @DeleteMapping("/api/registrations/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun cancelRegistration(
    @PathVariable("id") id: String,
    @RequestHeader(
      value = "Authorization",
      required = false,
    ) authorization: String?,
  ) {
    val token =
      authorization
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.substring(7)
        ?.trim()
    val cancelled = eventService.cancelRegistration(id, token)

    if (!cancelled) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
  }

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
