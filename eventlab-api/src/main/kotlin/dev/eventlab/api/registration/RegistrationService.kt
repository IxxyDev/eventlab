package dev.eventlab.api.registration

import dev.eventlab.api.event.EVENT_ID
import dev.eventlab.api.event.EventRepository
import dev.eventlab.api.event.RegistrationLimits
import dev.eventlab.api.event.isRegistrationOpen
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.UUID

@Service
class RegistrationService(
  private val eventRepository: EventRepository,
  private val registrationRepository: RegistrationRepository,
  private val transactionTemplate: TransactionTemplate,
  private val clock: Clock,
) {
  private val secureRandom = SecureRandom()

  fun register(
    request: RegistrationRequest,
    idempotencyKey: String?,
  ): RegistrationCreatedResponse {
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

    if (idempotencyKey != null && parseUuid(idempotencyKey) == null) {
      throw RegistrationValidationException(
        RegistrationErrorCode.INVALID_IDEMPOTENCY_KEY,
      )
    }

    val idempotencyKeyHash = idempotencyKey?.let { sha256(it) }

    return transactionTemplate.execute {
      val limits = eventRepository.lockRegistrationLimits(EVENT_ID)
      val previous =
        idempotencyKeyHash?.let {
          registrationRepository.findByIdempotencyKeyHash(it)
        }

      if (previous != null) {
        retryRegistration(previous, name, email)
      } else {
        createRegistration(limits, name, email, idempotencyKeyHash)
      }
    }
  }

  fun getRegistration(
    id: String,
    token: String?,
  ): RegistrationResponse? = findAccessibleRegistration(id, token)?.toResponse()

  fun cancelRegistration(
    id: String,
    token: String?,
  ): Boolean {
    val registration =
      findAccessibleRegistration(id, token)
        ?: return false

    return registrationRepository.deleteById(registration.id)
  }

  private fun retryRegistration(
    previous: RegistrationRow,
    name: String,
    email: String,
  ): RegistrationCreatedResponse {
    if (previous.name != name || previous.email != email) {
      throw IdempotencyKeyReusedException()
    }

    val accessToken = generateAccessToken()

    registrationRepository.replaceAccessTokenHash(
      previous.id,
      sha256(accessToken),
    )

    return RegistrationCreatedResponse(
      registration = previous.toResponse(),
      accessToken = accessToken,
    )
  }

  private fun createRegistration(
    limits: RegistrationLimits,
    name: String,
    email: String,
    idempotencyKeyHash: ByteArray?,
  ): RegistrationCreatedResponse {
    if (!isRegistrationOpen(limits.startsAt, clock.instant())) {
      throw RegistrationConflictException(
        RegistrationErrorCode.REGISTRATION_CLOSED,
      )
    }

    if (registrationRepository.existsByEventAndEmail(EVENT_ID, email)) {
      throw RegistrationConflictException(
        RegistrationErrorCode.EMAIL_ALREADY_REGISTERED,
      )
    }

    if (registrationRepository.countByEvent(EVENT_ID) >= limits.capacity) {
      throw RegistrationConflictException(
        RegistrationErrorCode.EVENT_FULL,
      )
    }

    val accessToken = generateAccessToken()
    val registration =
      RegistrationRow(
        id = UUID.randomUUID(),
        eventId = EVENT_ID,
        name = name,
        email = email,
        accessTokenHash = sha256(accessToken),
        idempotencyKeyHash = idempotencyKeyHash,
      )

    registrationRepository.insert(registration)

    return RegistrationCreatedResponse(
      registration = registration.toResponse(),
      accessToken = accessToken,
    )
  }

  private fun findAccessibleRegistration(
    id: String,
    token: String?,
  ): RegistrationRow? {
    val registrationId = parseUuid(id) ?: return null
    val registration = registrationRepository.findById(registrationId)

    if (registration == null || token.isNullOrEmpty()) {
      return null
    }

    val matches =
      MessageDigest.isEqual(
        registration.accessTokenHash,
        sha256(token),
      )

    if (!matches) {
      return null
    }

    return registration
  }

  private fun generateAccessToken(): String {
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)

    return Base64
      .getUrlEncoder()
      .withoutPadding()
      .encodeToString(bytes)
  }

  private fun sha256(value: String): ByteArray =
    MessageDigest
      .getInstance("SHA-256")
      .digest(value.toByteArray(Charsets.UTF_8))

  private fun parseUuid(value: String): UUID? =
    try {
      UUID.fromString(value)
    } catch (e: IllegalArgumentException) {
      null
    }

  private fun RegistrationRow.toResponse(): RegistrationResponse =
    RegistrationResponse(
      id = id.toString(),
      name = name,
      email = email,
    )
}
