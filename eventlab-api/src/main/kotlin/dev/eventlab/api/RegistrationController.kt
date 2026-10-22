package dev.eventlab.api

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

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class RegistrationController(
  private val registrationService: RegistrationService,
) {
  @PostMapping("/api/event/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  fun register(
    @RequestBody request: RegistrationRequest,
  ): RegistrationCreatedResponse = registrationService.register(request)

  @GetMapping("/api/registrations/{id}")
  fun getRegistration(
    @PathVariable("id") id: String,
    @RequestHeader(
      value = "Authorization",
      required = false,
    ) authorization: String?,
  ): RegistrationResponse =
    registrationService.getRegistration(id, bearerToken(authorization))
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  @DeleteMapping("/api/registrations/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun cancelRegistration(
    @PathVariable("id") id: String,
    @RequestHeader(
      value = "Authorization",
      required = false,
    ) authorization: String?,
  ) {
    val cancelled =
      registrationService.cancelRegistration(
        id,
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
}
