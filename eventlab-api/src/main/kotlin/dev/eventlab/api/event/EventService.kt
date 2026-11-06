package dev.eventlab.api.event

import org.springframework.stereotype.Service
import java.time.Clock

const val EVENT_ID = "sound-workshop"

@Service
class EventService(
  private val eventRepository: EventRepository,
  private val clock: Clock,
) {
  fun getEvent(id: String): EventOverview? {
    val event = eventRepository.findById(id) ?: return null
    val now = clock.instant()

    return EventOverview(
      event = event,
      registrationOpen = event.isRegistrationOpen(now),
    )
  }

  fun getEvents(): List<EventOverview> {
    val now = clock.instant()

    return eventRepository.findAll().map { event ->
      EventOverview(
        event = event,
        registrationOpen = event.isRegistrationOpen(now),
      )
    }
  }
}
