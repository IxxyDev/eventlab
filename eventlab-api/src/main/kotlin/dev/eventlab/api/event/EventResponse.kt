package dev.eventlab.api.event

import java.time.Instant
import java.time.ZoneId

data class EventResponse(
  val id: String,
  val title: String,
  val description: String,
  val startsAt: Instant,
  val endsAt: Instant,
  val timeZone: ZoneId,
  val venue: String,
  val capacity: Int,
  val availableSeats: Int,
  val registrationOpen: Boolean,
)
