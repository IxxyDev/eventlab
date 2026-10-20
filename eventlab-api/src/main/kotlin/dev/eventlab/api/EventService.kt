package dev.eventlab.api

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

private const val EVENT_ID = "sound-workshop"

@Service
class EventService(
  private val jdbcClient: JdbcClient,
) {
  private val registrations = mutableListOf<StoredRegistration>()
  private val secureRandom = SecureRandom()

  @Synchronized
  fun register(request: RegistrationRequest): RegistrationCreatedResponse {
    val name = request.name.trim()
    val email = request.email.trim()
    val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    if (name.isEmpty() || email.isEmpty()) {
      throw RegistrationValidationException(
        RegistrationErrorCode.REQUIRED_FIELDS,
      )
    }

    if (name.length > 100 || email.length > 254) {
      throw RegistrationValidationException(
        RegistrationErrorCode.INPUT_TOO_LONG,
      )
    }

    if (!emailPattern.matches(email)) {
      throw RegistrationValidationException(
        RegistrationErrorCode.INVALID_EMAIL,
      )
    }

    if (registrations.any {
        it.registration.email.equals(email, ignoreCase = true)
      }
    ) {
      throw RegistrationConflictException(
        RegistrationErrorCode.EMAIL_ALREADY_REGISTERED,
      )
    }

    if (registrations.size >= findEvent().capacity) {
      throw RegistrationConflictException(
        RegistrationErrorCode.EVENT_FULL,
      )
    }

    val registration =
      RegistrationResponse(
        id = UUID.randomUUID().toString(),
        name = name,
        email = email,
      )

    val accessToken = generateAccessToken()

    registrations.add(
      StoredRegistration(
        registration = registration,
        accessToken = accessToken,
      ),
    )

    return RegistrationCreatedResponse(
      registration = registration,
      accessToken = accessToken,
    )
  }

  @Synchronized
  fun getEvent(): EventResponse {
    val event = findEvent()

    return EventResponse(
      id = event.id,
      title = event.title,
      description = event.description,
      dateLabel = event.dateLabel,
      timeLabel = event.timeLabel,
      venue = event.venue,
      capacity = event.capacity,
      availableSeats = event.capacity - registrations.size,
    )
  }

  @Synchronized
  fun getRegistration(
    id: String,
    token: String?,
  ): RegistrationResponse? {
    val stored =
      registrations.find {
        it.registration.id == id
      }

    if (stored == null || token.isNullOrEmpty()) {
      return null
    }

    val matches =
      MessageDigest.isEqual(
        stored.accessToken.toByteArray(Charsets.UTF_8),
        token.toByteArray(Charsets.UTF_8),
      )

    if (!matches) {
      return null
    }

    return stored.registration
  }

  @Synchronized
  fun cancelRegistration(
    id: String,
    token: String?,
  ): Boolean {
    val registration =
      getRegistration(id, token)
        ?: return false

    return registrations.removeAll {
      it.registration.id == registration.id
    }
  }

  private fun generateAccessToken(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)

    return Base64
      .getUrlEncoder()
      .withoutPadding()
      .encodeToString(bytes)
  }

  private fun findEvent(): EventRow =
    jdbcClient
      .sql(
        """
        SELECT id, title, description, date_label, time_label, venue, capacity
        FROM events
        WHERE id = :id
        """.trimIndent(),
      ).param("id", EVENT_ID)
      .query { rs, _ ->
        EventRow(
          id = rs.getString("id"),
          title = rs.getString("title"),
          description = rs.getString("description"),
          dateLabel = rs.getString("date_label"),
          timeLabel = rs.getString("time_label"),
          venue = rs.getString("venue"),
          capacity = rs.getInt("capacity"),
        )
      }.single()

  private data class EventRow(
    val id: String,
    val title: String,
    val description: String,
    val dateLabel: String,
    val timeLabel: String,
    val venue: String,
    val capacity: Int,
  )
}
