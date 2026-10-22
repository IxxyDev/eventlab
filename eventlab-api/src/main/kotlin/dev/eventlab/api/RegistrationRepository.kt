package dev.eventlab.api

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

data class RegistrationRow(
  val id: UUID,
  val eventId: String,
  val name: String,
  val email: String,
  val accessToken: String,
)

@Repository
class RegistrationRepository(
  private val jdbcClient: JdbcClient,
) {
  fun countByEvent(eventId: String): Int =
    jdbcClient
      .sql(
        """
        SELECT COUNT(*)
        FROM registrations
        WHERE event_id = :eventId
        """.trimIndent(),
      ).param("eventId", eventId)
      .query(Int::class.java)
      .single()

  fun existsByEventAndEmail(
    eventId: String,
    email: String,
  ): Boolean =
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
      ).param("eventId", eventId)
      .param("email", email)
      .query(Boolean::class.java)
      .single()

  fun findById(id: UUID): RegistrationRow? =
    jdbcClient
      .sql(
        """
        SELECT id, event_id, name, email, access_token
        FROM registrations
        WHERE id = :id
        """.trimIndent(),
      ).param("id", id)
      .query { rs, _ ->
        RegistrationRow(
          id = rs.getObject("id", UUID::class.java),
          eventId = rs.getString("event_id"),
          name = rs.getString("name"),
          email = rs.getString("email"),
          accessToken = rs.getString("access_token"),
        )
      }.list()
      .firstOrNull()

  fun insert(registration: RegistrationRow) {
    jdbcClient
      .sql(
        """
        INSERT INTO registrations (id, event_id, name, email, access_token)
        VALUES (:id, :eventId, :name, :email, :accessToken)
        """.trimIndent(),
      ).param("id", registration.id)
      .param("eventId", registration.eventId)
      .param("name", registration.name)
      .param("email", registration.email)
      .param("accessToken", registration.accessToken)
      .update()
  }

  fun deleteById(id: UUID): Boolean =
    jdbcClient
      .sql("DELETE FROM registrations WHERE id = :id")
      .param("id", id)
      .update() > 0
}
