package dev.eventlab.api

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
