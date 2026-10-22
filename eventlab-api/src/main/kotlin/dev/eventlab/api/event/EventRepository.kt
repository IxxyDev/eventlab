package dev.eventlab.api.event

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

data class EventRow(
  val id: String,
  val title: String,
  val description: String,
  val dateLabel: String,
  val timeLabel: String,
  val venue: String,
  val capacity: Int,
  val registrationCount: Int,
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
          date_label,
          time_label,
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
          dateLabel = rs.getString("date_label"),
          timeLabel = rs.getString("time_label"),
          venue = rs.getString("venue"),
          capacity = rs.getInt("capacity"),
          registrationCount = rs.getInt("registration_count"),
        )
      }.single()

  fun lockCapacity(id: String): Int =
    jdbcClient
      .sql(
        """
        SELECT capacity
        FROM events
        WHERE id = :id
        FOR UPDATE
        """.trimIndent(),
      ).param("id", id)
      .query(Int::class.java)
      .single()
}
