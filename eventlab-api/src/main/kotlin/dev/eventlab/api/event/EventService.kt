package dev.eventlab.api.event

import org.springframework.stereotype.Service

const val EVENT_ID = "sound-workshop"

@Service
class EventService(
  private val eventRepository: EventRepository,
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
      availableSeats = event.capacity - event.registrationCount,
    )
  }
}
