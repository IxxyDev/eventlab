package dev.eventlab.api.event

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

data class EventRow(
  val id: String,
  val title: String,
  val description: String,
  val startsAt: Instant,
  val endsAt: Instant,
  val timeZone: ZoneId,
  val venue: String,
  val capacity: Int,
  val registrationCount: Int,
)

data class RegistrationLimits(
  val capacity: Int,
  val startsAt: Instant,
)

@Repository
class EventRepository(
  private val jdbcClient: JdbcClient,
) {
  fun findById(id: String): EventRow =
    jdbcClient
      .sql(
        """
        SELECT
          id,
          title,
          description,
          starts_at,
          ends_at,
          time_zone,
          venue,
          capacity,
          (
            SELECT COUNT(*)
            FROM registrations
            WHERE registrations.event_id = events.id
          ) AS registration_count
        FROM events
        WHERE id = :id
        """.trimIndent(),
      ).param("id", id)
      .query { rs, _ ->
        EventRow(
          id = rs.getString("id"),
          title = rs.getString("title"),
          description = rs.getString("description"),
          startsAt = rs.getInstant("starts_at"),
          endsAt = rs.getInstant("ends_at"),
          timeZone = ZoneId.of(rs.getString("time_zone")),
          venue = rs.getString("venue"),
          capacity = rs.getInt("capacity"),
          registrationCount = rs.getInt("registration_count"),
        )
      }.single()

  fun lockRegistrationLimits(id: String): RegistrationLimits =
    jdbcClient
      .sql(
        """
        SELECT capacity, starts_at
        FROM events
        WHERE id = :id
        FOR UPDATE
        """.trimIndent(),
      ).param("id", id)
      .query { rs, _ ->
        RegistrationLimits(
          capacity = rs.getInt("capacity"),
          startsAt = rs.getInstant("starts_at"),
        )
      }.single()

  private fun ResultSet.getInstant(column: String): Instant =
    getObject(column, OffsetDateTime::class.java).toInstant()
}
