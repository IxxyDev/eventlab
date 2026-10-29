package dev.eventlab.api.event

import dev.eventlab.api.loadSql
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

@Repository
class EventRepository(
  private val jdbcClient: JdbcClient,
) {
  private val findByIdSql = loadSql("event/find_by_id.sql")
  private val lockByIdSql = loadSql("event/lock_by_id.sql")

  fun findById(id: String): Event =
    jdbcClient
      .sql(findByIdSql)
      .param("id", id)
      .query { rs, _ ->
        Event(
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

  fun lockById(id: String): Event {
    jdbcClient
      .sql(lockByIdSql)
      .param("id", id)
      .query(String::class.java)
      .single()

    return findById(id)
  }

  private fun ResultSet.getInstant(column: String): Instant =
    getObject(column, OffsetDateTime::class.java).toInstant()
}
