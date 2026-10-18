package dev.eventlab.eventlab_api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class EventResponse(
    val id: String,
    val title: String,
    val description: String,
    val dateLabel: String,
    val timeLabel: String,
    val venue: String,
    val capacity: Int,
    val availableSeats: Int
)

data class RegistrationRequest(
    val name: String,
    val email: String
)

data class RegistrationResponse(
    val id: String,
    val name: String,
    val email: String
)

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class EventController {
    private val registrations = mutableListOf<RegistrationResponse>()
    private val capacity = 1

    @PostMapping("/api/event/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    @Synchronized
    fun register(
        @RequestBody request: RegistrationRequest
    ): RegistrationResponse {
        val name = request.name.trim()
        val email = request.email.trim()

        if (name.isEmpty() || email.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Name and email required"
            )
        }

        if (registrations.any { it.email.equals(email, ignoreCase = true) }) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "This email is already registered"
            )
        }

        if (registrations.size >= capacity) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "No seats available"
            )
        }

        val registration = RegistrationResponse(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email
        )

        registrations.add(registration)

        return registration
    }
    
    @GetMapping("/api/event")
    @Synchronized
    fun getEvent(): EventResponse {
        return EventResponse(
            id = "sound-workshop",
            title = "Как устроен звук на концерте",
            description = "Разберём путь звука от микрофона до колонок " +
                "и попробуем настроить небольшой концертный микс.",
            dateLabel = "14 ноября 2026 года",
            timeLabel = "18:00–20:00, по Москве",
            venue = "Пространство «Сцена», зал № 2",
            capacity = capacity,
            availableSeats = capacity - registrations.size
        )
    }
}
