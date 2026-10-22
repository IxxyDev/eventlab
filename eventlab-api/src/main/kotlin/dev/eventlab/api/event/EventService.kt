package dev.eventlab.api.event

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

const val EVENT_ID = "sound-workshop"

fun isRegistrationOpen(
  startsAt: Instant,
  now: Instant,
): Boolean = now < startsAt

@Service
class EventService(
  private val eventRepository: EventRepository,
  private val clock: Clock,
) {
  fun getEvent(): EventResponse {
    val event = eventRepository.findById(EVENT_ID)

    return EventResponse(
      id = event.id,
      title = event.title,
      description = event.description,
      startsAt = event.startsAt,
      endsAt = event.endsAt,
      timeZone = event.timeZone,
      venue = event.venue,
      capacity = event.capacity,
      availableSeats = event.capacity - event.registrationCount,
      registrationOpen = isRegistrationOpen(event.startsAt, clock.instant()),
    )
  }
}
