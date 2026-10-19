package dev.eventlab.api

import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

@Service
class EventService {
    private val registrations = mutableListOf<StoredRegistration>()
    private val capacity = 1
    private val secureRandom = SecureRandom()

    @Synchronized
    fun register(
        request: RegistrationRequest
    ): RegistrationCreatedResponse {
        val name = request.name.trim()
        val email = request.email.trim()
        val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

        if (name.isEmpty() || email.isEmpty()) {
            throw RegistrationValidationException(
                RegistrationErrorCode.REQUIRED_FIELDS
            )
        }

        if (name.length > 100 || email.length > 254) {
            throw RegistrationValidationException(
                RegistrationErrorCode.INPUT_TOO_LONG
            )
        }

        if (!emailPattern.matches(email)) {
            throw RegistrationValidationException(
                RegistrationErrorCode.INVALID_EMAIL
            )
        }

        if (registrations.any { it.registration.email.equals(email, ignoreCase = true) }) {
            throw RegistrationConflictException(
                RegistrationErrorCode.EMAIL_ALREADY_REGISTERED
            )
        }

        if (registrations.size >= capacity) {
            throw RegistrationConflictException(
                RegistrationErrorCode.EVENT_FULL
            )
        }

        val registration = RegistrationResponse(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email
        )

        val accessToken = generateAccessToken()

        registrations.add(
            StoredRegistration(
                registration = registration,
                accessToken = accessToken
            )
        )

        return RegistrationCreatedResponse(
            registration = registration,
            accessToken = accessToken
        )
    }

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

    @Synchronized
    fun getRegistration(
        id: String,
        token: String?
    ): RegistrationResponse? {
        val stored = registrations.find {
            it.registration.id == id
        }

        if (stored == null || token.isNullOrEmpty()) {
            return null
        }

        val matches = MessageDigest.isEqual(
            stored.accessToken.toByteArray(Charsets.UTF_8),
            token.toByteArray(Charsets.UTF_8)
        )

        if (!matches) {
            return null
        }

        return stored.registration
    }

    @Synchronized
    fun cancelRegistration(
        id: String,
        token: String?
    ): Boolean {
        val registration = getRegistration(id, token)
            ?: return false

        return registrations.removeAll {
            it.registration.id == registration.id
        }
    }

    private fun generateAccessToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }
}
