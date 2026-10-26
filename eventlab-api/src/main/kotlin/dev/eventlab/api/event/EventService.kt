package dev.eventlab.api.event

import org.springframework.stereotype.Service
import java.time.Clock

const val EVENT_ID = "sound-workshop"

@Service
class EventService(
  private val eventRepository: EventRepository,
  private val clock: Clock,
) {
  fun getEvent(): EventOverview {
    val event = eventRepository.findById(EVENT_ID)

    return EventOverview(
      event = event,
      registrationOpen = event.isRegistrationOpen(clock.instant()),
    )
  }
}
