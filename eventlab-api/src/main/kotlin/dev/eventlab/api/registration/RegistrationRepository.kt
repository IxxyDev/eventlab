package dev.eventlab.api.registration

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

data class RegistrationRow(
  val id: UUID,
  val eventId: String,
  val name: String,
  val email: String,
  val accessTokenHash: ByteArray,
  val idempotencyKeyHash: ByteArray?,
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
        SELECT id, event_id, name, email, access_token_hash, idempotency_key_hash
        FROM registrations
        WHERE id = :id
        """.trimIndent(),
      ).param("id", id)
      .query { rs, _ -> rs.toRegistrationRow() }
      .list()
      .firstOrNull()

  fun findByIdempotencyKeyHash(
    idempotencyKeyHash: ByteArray,
  ): RegistrationRow? =
    jdbcClient
      .sql(
        """
        SELECT id, event_id, name, email, access_token_hash, idempotency_key_hash
        FROM registrations
        WHERE idempotency_key_hash = :idempotencyKeyHash
        """.trimIndent(),
      ).param("idempotencyKeyHash", idempotencyKeyHash)
      .query { rs, _ -> rs.toRegistrationRow() }
      .list()
      .firstOrNull()

  fun insert(registration: RegistrationRow) {
    jdbcClient
      .sql(
        """
        INSERT INTO registrations (
          id, event_id, name, email, access_token_hash, idempotency_key_hash
        )
        VALUES (
          :id, :eventId, :name, :email, :accessTokenHash, :idempotencyKeyHash
        )
        """.trimIndent(),
      ).param("id", registration.id)
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
      .sql(
        """
        UPDATE registrations
        SET access_token_hash = :accessTokenHash
        WHERE id = :id
        """.trimIndent(),
      ).param("id", id)
      .param("accessTokenHash", accessTokenHash)
      .update()
  }

  fun deleteById(id: UUID): Boolean =
    jdbcClient
      .sql("DELETE FROM registrations WHERE id = :id")
      .param("id", id)
      .update() > 0

  private fun ResultSet.toRegistrationRow(): RegistrationRow =
    RegistrationRow(
      id = getObject("id", UUID::class.java),
      eventId = getString("event_id"),
      name = getString("name"),
      email = getString("email"),
      accessTokenHash = getBytes("access_token_hash"),
      idempotencyKeyHash = getBytes("idempotency_key_hash"),
    )
}
