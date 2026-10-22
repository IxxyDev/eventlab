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
