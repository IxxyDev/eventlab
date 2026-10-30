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
  private val findAllSql = loadSql("event/find_all.sql")

  fun findById(id: String): Event? =
    jdbcClient
      .sql(findByIdSql)
      .param("id", id)
      .query { rs, _ -> rs.toEvent() }
      .list()
      .firstOrNull()

  fun findAll(): List<Event> =
    jdbcClient
      .sql(findAllSql)
      .query { rs, _ -> rs.toEvent() }
      .list()

  fun lockById(id: String): Event {
    jdbcClient
      .sql(lockByIdSql)
      .param("id", id)
      .query(String::class.java)
      .single()

    return checkNotNull(findById(id))
  }

  private fun ResultSet.getInstant(column: String): Instant =
    getObject(column, OffsetDateTime::class.java).toInstant()

  private fun ResultSet.toEvent(): Event =
    Event(
      id = getString("id"),
      organizationName = getString("organization_name"),
      title = getString("title"),
      description = getString("description"),
      startsAt = getInstant("starts_at"),
      endsAt = getInstant("ends_at"),
      timeZone = ZoneId.of(getString("time_zone")),
      venue = getString("venue"),
      capacity = getInt("capacity"),
      registrationCount = getInt("registration_count"),
    )
}
