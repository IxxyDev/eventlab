package dev.eventlab.api

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class EventController(
  private val eventService: EventService,
) {
  @GetMapping("/api/event")
  fun getEvent(): EventResponse = eventService.getEvent()
}
