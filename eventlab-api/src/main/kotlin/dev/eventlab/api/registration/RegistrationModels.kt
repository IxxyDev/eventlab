package dev.eventlab.api.registration

data class RegistrationRequest(
  val name: String,
  val email: String,
)

data class RegistrationResponse(
  val id: String,
  val name: String,
  val email: String,
)

data class RegistrationCreatedResponse(
  val registration: RegistrationResponse,
  val accessToken: String,
)

data class RegistrationErrorResponse(
  val code: RegistrationErrorCode,
)

fun Registration.toResponse(): RegistrationResponse =
  RegistrationResponse(
    id = id.toString(),
    name = name,
    email = email,
  )

fun RegistrationWithAccessToken.toResponse(): RegistrationCreatedResponse =
  RegistrationCreatedResponse(
    registration = registration.toResponse(),
    accessToken = accessToken,
  )

fun RegistrationRequest.toParticipant(): Participant =
  Participant.of(name, email)
