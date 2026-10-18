package dev.eventlab.eventlab_api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
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

data class RegistrationCreatedResponse(
    val registration: RegistrationResponse,
    val accessToken: String
)

data class StoredRegistration(
    val registration: RegistrationResponse,
    val accessToken: String,
)

@RestController
@CrossOrigin(origins = ["http://localhost:5173"])
class EventController {
    private val registrations = mutableListOf<StoredRegistration>()
    private val capacity = 1
    private val secureRandom = SecureRandom()

    @PostMapping("/api/event/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    @Synchronized
    fun register(
        @RequestBody request: RegistrationRequest
    ): RegistrationCreatedResponse {
        val name = request.name.trim()
        val email = request.email.trim()

        if (name.isEmpty() || email.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Name and email required"
            )
        }

        if (registrations.any { it.registration.email.equals(email, ignoreCase = true) }) {
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

        val accessToken = generateAcessToken()

        registrations.add(StoredRegistration(
            registration = registration,
            accessToken = accessToken
        ))

        return RegistrationCreatedResponse(
            registration = registration,
            accessToken = accessToken
        )
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

    @GetMapping("/api/registrations/{id}")
    @Synchronized
    fun getRegistration(
        @PathVariable("id") id: String,
        @RequestHeader(
            value = "Authorization",
            required = false
        ) authorization: String?
    ): RegistrationResponse {
        val stored = registrations.find {
            it.registration.id == id
        }

        val token = authorization
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substring(7)
            ?.trim()

        if (stored == null || token.isNullOrEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        val matches = MessageDigest.isEqual(
            stored.accessToken.toByteArray(Charsets.UTF_8),
            token.toByteArray(Charsets.UTF_8)
        )

        if (!matches) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        return stored.registration
    }

    private fun generateAcessToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }
}
