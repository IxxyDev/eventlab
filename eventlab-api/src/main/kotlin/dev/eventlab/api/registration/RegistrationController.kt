package dev.eventlab.api.registration

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class RegistrationController(
  private val registrationService: RegistrationService,
) {
  @PostMapping("/api/events/{eventId}/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  fun register(
    @PathVariable("eventId") eventId: String,
    @RequestBody request: RegistrationRequest,
    @RequestHeader(
      value = "Idempotency-Key",
      required = false,
    ) idempotencyKeyHeader: String?,
  ): RegistrationCreatedResponse {
    val participant = request.toParticipant()
    val idempotencyKey = parseIdempotencyKey(idempotencyKeyHeader)

    return registrationService
      .register(eventId, participant, idempotencyKey)
      ?.toResponse()
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  }

  @GetMapping("/api/registrations/{id}")
  fun getRegistration(
    @PathVariable("id") id: String,
    @RequestHeader(
      value = "Authorization",
      required = false,
    ) authorization: String?,
  ): RegistrationResponse {
    val registrationId = parseRegistrationId(id)

    return registrationService
      .getRegistration(registrationId, bearerToken(authorization))
      ?.toResponse()
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
    val registrationId = parseRegistrationId(id)
    val cancelled =
      registrationService.cancelRegistration(
        registrationId,
        bearerToken(authorization),
      )

    if (!cancelled) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
  }

  private fun bearerToken(authorization: String?): String? =
    authorization
      ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
      ?.substring(7)
      ?.trim()

  private fun parseIdempotencyKey(header: String?): UUID? {
    if (header == null) {
      return null
    }

    return header.toUuidOrNull()
      ?: throw RegistrationValidationException(
        RegistrationErrorCode.INVALID_IDEMPOTENCY_KEY,
      )
  }

  private fun parseRegistrationId(id: String): UUID =
    id.toUuidOrNull()
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}

private fun String.toUuidOrNull(): UUID? =
  try {
    UUID.fromString(this)
  } catch (e: IllegalArgumentException) {
    null
  }
