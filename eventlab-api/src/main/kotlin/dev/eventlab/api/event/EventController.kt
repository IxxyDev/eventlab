package dev.eventlab.api.event

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class EventController(
  private val eventService: EventService,
) {
  @GetMapping("/api/event")
  fun getEvent(): EventResponse = getEventById(EVENT_ID)

  @GetMapping("/api/events/{id}")
  fun getEventById(
    @PathVariable("id") id: String,
  ): EventResponse =
    eventService
      .getEvent(id)
      ?.toResponse()
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  @GetMapping("/api/events")
  fun getEvents(): List<EventResponse> =
    eventService.getEvents().map {
      it.toResponse()
    }
}
