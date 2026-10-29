package dev.eventlab.api.registration

import dev.eventlab.api.event.EVENT_ID
import dev.eventlab.api.event.Event
import dev.eventlab.api.event.EventRepository
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
    participant: Participant,
    idempotencyKey: String?,
  ): RegistrationWithAccessToken {
    if (idempotencyKey != null && parseUuid(idempotencyKey) == null) {
      throw RegistrationValidationException(
        RegistrationErrorCode.INVALID_IDEMPOTENCY_KEY,
      )
    }

    val idempotencyKeyHash = idempotencyKey?.let { sha256(it) }

    return transactionTemplate.execute {
      val event = eventRepository.lockById(EVENT_ID)
      val previous =
        idempotencyKeyHash?.let {
          registrationRepository.findByIdempotencyKeyHash(it)
        }

      if (previous != null) {
        retryRegistration(previous, participant)
      } else {
        createRegistration(event, participant, idempotencyKeyHash)
      }
    }
  }

  fun getRegistration(
    id: String,
    token: String?,
  ): Registration? = findAccessibleRegistration(id, token)

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
    previous: Registration,
    participant: Participant,
  ): RegistrationWithAccessToken {
    if (previous.name != participant.name ||
      previous.email != participant.email
    ) {
      throw IdempotencyKeyReusedException()
    }

    val accessToken = generateAccessToken()

    registrationRepository.replaceAccessTokenHash(
      previous.id,
      sha256(accessToken),
    )

    return RegistrationWithAccessToken(
      registration = previous,
      accessToken = accessToken,
    )
  }

  private fun createRegistration(
    event: Event,
    participant: Participant,
    idempotencyKeyHash: ByteArray?,
  ): RegistrationWithAccessToken {
    if (!event.isRegistrationOpen(clock.instant())) {
      throw RegistrationConflictException(
        RegistrationErrorCode.REGISTRATION_CLOSED,
      )
    }

    if (registrationRepository.existsByEventAndEmail(
        event.id,
        participant.email,
      )
    ) {
      throw RegistrationConflictException(
        RegistrationErrorCode.EMAIL_ALREADY_REGISTERED,
      )
    }

    if (!event.hasAvailableSeats) {
      throw RegistrationConflictException(
        RegistrationErrorCode.EVENT_FULL,
      )
    }

    val accessToken = generateAccessToken()
    val registration =
      Registration(
        id = UUID.randomUUID(),
        eventId = event.id,
        name = participant.name,
        email = participant.email,
        accessTokenHash = sha256(accessToken),
        idempotencyKeyHash = idempotencyKeyHash,
      )

    registrationRepository.insert(registration)

    return RegistrationWithAccessToken(
      registration = registration,
      accessToken = accessToken,
    )
  }

  private fun findAccessibleRegistration(
    id: String,
    token: String?,
  ): Registration? {
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
}
