package dev.eventlab.api.event

import dev.eventlab.api.loadSql
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
  private val findByIdSql = loadSql("event/find_by_id.sql")
  private val lockRegistrationLimitsSql =
    loadSql("event/lock_registration_limits.sql")

  fun findById(id: String): EventRow =
    jdbcClient
      .sql(findByIdSql)
      .param("id", id)
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
      .sql(lockRegistrationLimitsSql)
      .param("id", id)
      .query { rs, _ ->
        RegistrationLimits(
          capacity = rs.getInt("capacity"),
          startsAt = rs.getInstant("starts_at"),
        )
      }.single()

  private fun ResultSet.getInstant(column: String): Instant =
    getObject(column, OffsetDateTime::class.java).toInstant()
}
