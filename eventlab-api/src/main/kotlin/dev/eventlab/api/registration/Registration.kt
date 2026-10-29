package dev.eventlab.api.registration

import java.util.UUID

data class Registration(
  val id: UUID,
  val eventId: String,
  val name: String,
  val email: String,
  val accessTokenHash: ByteArray,
  val idempotencyKeyHash: ByteArray?,
)

data class RegistrationWithAccessToken(
  val registration: Registration,
  val accessToken: String,
)
