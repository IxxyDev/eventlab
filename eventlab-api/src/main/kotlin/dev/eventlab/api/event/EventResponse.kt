package dev.eventlab.api.event

import java.time.Instant
import java.time.ZoneId

data class EventResponse(
  val id: String,
  val organizationName: String,
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

fun EventOverview.toResponse(): EventResponse =
  EventResponse(
    id = event.id,
    organizationName = event.organizationName,
    title = event.title,
    description = event.description,
    startsAt = event.startsAt,
    endsAt = event.endsAt,
    timeZone = event.timeZone,
    venue = event.venue,
    capacity = event.capacity,
    availableSeats = event.availableSeats,
    registrationOpen = registrationOpen,
  )
