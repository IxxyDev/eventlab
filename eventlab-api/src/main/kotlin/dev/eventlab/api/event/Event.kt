package dev.eventlab.api.event

import java.time.Instant
import java.time.ZoneId

data class Event(
  val id: String,
  val title: String,
  val description: String,
  val startsAt: Instant,
  val endsAt: Instant,
  val timeZone: ZoneId,
  val venue: String,
  val capacity: Int,
  val registrationCount: Int,
) {
  val availableSeats: Int
    get() = capacity - registrationCount

  val hasAvailableSeats: Boolean
    get() = availableSeats > 0

  fun isRegistrationOpen(now: Instant): Boolean = now < startsAt
}

data class EventOverview(
  val event: Event,
  val registrationOpen: Boolean,
)
