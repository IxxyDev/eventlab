package dev.eventlab.api

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

private const val EVENT_ID = "sound-workshop"

@Service
class EventService(
  private val jdbcClient: JdbcClient,
  private val transactionTemplate: TransactionTemplate,
) {
  private val secureRandom = SecureRandom()

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

    return transactionTemplate.execute {
      val capacity = lockEventCapacity()

      if (isEmailRegistered(email)) {
        throw RegistrationConflictException(
          RegistrationErrorCode.EMAIL_ALREADY_REGISTERED,
        )
      }

      if (countRegistrations() >= capacity) {
        throw RegistrationConflictException(
          RegistrationErrorCode.EVENT_FULL,
        )
      }

      val id = UUID.randomUUID()
      val registration =
        RegistrationResponse(
          id = id.toString(),
          name = name,
          email = email,
        )

      val accessToken = generateAccessToken()

      jdbcClient
        .sql(
          """
          INSERT INTO registrations (id, event_id, name, email, access_token)
          VALUES (:id, :eventId, :name, :email, :accessToken)
          """.trimIndent(),
        ).param("id", id)
        .param("eventId", EVENT_ID)
        .param("name", name)
        .param("email", email)
        .param("accessToken", accessToken)
        .update()

      RegistrationCreatedResponse(
        registration = registration,
        accessToken = accessToken,
      )
    }
  }

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
      availableSeats = event.capacity - countRegistrations(),
    )
  }

  fun getRegistration(
    id: String,
    token: String?,
  ): RegistrationResponse? {
    val registrationId = parseUuid(id) ?: return null
    val stored = findRegistration(registrationId)

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

  fun cancelRegistration(
    id: String,
    token: String?,
  ): Boolean {
    val registration =
      getRegistration(id, token)
        ?: return false

    val deletedCount =
      jdbcClient
        .sql("DELETE FROM registrations WHERE id = :id")
        .param("id", UUID.fromString(registration.id))
        .update()

    return deletedCount > 0
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

  private fun countRegistrations(): Int =
    jdbcClient
      .sql(
        """
        SELECT COUNT(*)
        FROM registrations
        WHERE event_id = :eventId
        """.trimIndent(),
      ).param("eventId", EVENT_ID)
      .query(Int::class.java)
      .single()

  private fun isEmailRegistered(email: String): Boolean =
    jdbcClient
      .sql(
        """
        SELECT EXISTS (
          SELECT 1
          FROM registrations
          WHERE event_id = :eventId
            AND LOWER(email) = LOWER(:email)
        )
        """.trimIndent(),
      ).param("eventId", EVENT_ID)
      .param("email", email)
      .query(Boolean::class.java)
      .single()

  private fun findRegistration(id: UUID): StoredRegistration? =
    jdbcClient
      .sql(
        """
        SELECT id, name, email, access_token
        FROM registrations
        WHERE id = :id
        """.trimIndent(),
      ).param("id", id)
      .query { rs, _ ->
        StoredRegistration(
          registration =
            RegistrationResponse(
              id = rs.getString("id"),
              name = rs.getString("name"),
              email = rs.getString("email"),
            ),
          accessToken = rs.getString("access_token"),
        )
      }.list()
      .firstOrNull()

  private fun parseUuid(value: String): UUID? =
    try {
      UUID.fromString(value)
    } catch (e: IllegalArgumentException) {
      null
    }

  private fun lockEventCapacity(): Int =
    jdbcClient
      .sql(
        """
        SELECT capacity
        FROM events
        WHERE id = :id
        FOR UPDATE
        """.trimIndent(),
      ).param("id", EVENT_ID)
      .query(Int::class.java)
      .single()

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
