package dev.eventlab.api

import org.springframework.stereotype.Service

const val EVENT_ID = "sound-workshop"

@Service
class EventService(
  private val eventRepository: EventRepository,
  private val registrationRepository: RegistrationRepository,
) {
  fun getEvent(): EventResponse {
    val event = eventRepository.findById(EVENT_ID)

    return EventResponse(
      id = event.id,
      title = event.title,
      description = event.description,
      dateLabel = event.dateLabel,
      timeLabel = event.timeLabel,
      venue = event.venue,
      capacity = event.capacity,
      availableSeats =
        event.capacity - registrationRepository.countByEvent(EVENT_ID),
    )
  }
}
