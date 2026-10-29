package dev.eventlab.api.registration

import dev.eventlab.api.loadSql
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class RegistrationRepository(
  private val jdbcClient: JdbcClient,
) {
  private val existsByEventAndEmailSql =
    loadSql("registration/exists_by_event_and_email.sql")
  private val findByIdSql = loadSql("registration/find_by_id.sql")
  private val findByIdempotencyKeyHashSql =
    loadSql("registration/find_by_idempotency_key_hash.sql")
  private val insertSql = loadSql("registration/insert.sql")
  private val replaceAccessTokenHashSql =
    loadSql("registration/replace_access_token_hash.sql")
  private val deleteByIdSql = loadSql("registration/delete_by_id.sql")

  fun existsByEventAndEmail(
    eventId: String,
    email: String,
  ): Boolean =
    jdbcClient
      .sql(existsByEventAndEmailSql)
      .param("eventId", eventId)
      .param("email", email)
      .query(Boolean::class.java)
      .single()

  fun findById(id: UUID): Registration? =
    jdbcClient
      .sql(findByIdSql)
      .param("id", id)
      .query { rs, _ -> rs.toRegistration() }
      .list()
      .firstOrNull()

  fun findByIdempotencyKeyHash(idempotencyKeyHash: ByteArray): Registration? =
    jdbcClient
      .sql(findByIdempotencyKeyHashSql)
      .param("idempotencyKeyHash", idempotencyKeyHash)
      .query { rs, _ -> rs.toRegistration() }
      .list()
      .firstOrNull()

  fun insert(registration: Registration) {
    jdbcClient
      .sql(insertSql)
      .param("id", registration.id)
      .param("eventId", registration.eventId)
      .param("name", registration.name)
      .param("email", registration.email)
      .param("accessTokenHash", registration.accessTokenHash)
      .param("idempotencyKeyHash", registration.idempotencyKeyHash)
      .update()
  }

  fun replaceAccessTokenHash(
    id: UUID,
    accessTokenHash: ByteArray,
  ) {
    jdbcClient
      .sql(replaceAccessTokenHashSql)
      .param("id", id)
      .param("accessTokenHash", accessTokenHash)
      .update()
  }

  fun deleteById(id: UUID): Boolean =
    jdbcClient
      .sql(deleteByIdSql)
      .param("id", id)
      .update() > 0

  private fun ResultSet.toRegistration(): Registration =
    Registration(
      id = getObject("id", UUID::class.java),
      eventId = getString("event_id"),
      name = getString("name"),
      email = getString("email"),
      accessTokenHash = getBytes("access_token_hash"),
      idempotencyKeyHash = getBytes("idempotency_key_hash"),
    )
}
