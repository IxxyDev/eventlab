package dev.eventlab.api.registration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParticipantTest {
  @Test
  fun `should trim spaces around name and email`() {
    val participant = Participant("  Anna ", " anna@example.com  ")

    assertEquals("Anna", participant.name)
    assertEquals("anna@example.com", participant.email)
  }

  @Test
  fun `should reject name made only of spaces`() {
    val error =
      assertFailsWith<RegistrationValidationException> {
        Participant("   ", "anna@example.com")
      }

    assertEquals(RegistrationErrorCode.REQUIRED_FIELDS, error.code)
  }

  @Test
  fun `should accept name of exactly 100 characters`() {
    val name = "a".repeat(100)

    val participant = Participant(name, "anna@example.com")

    assertEquals(name, participant.name)
  }

  @Test
  fun `should reject name longer than 100 characters`() {
    val name = "a".repeat(101)

    val error =
      assertFailsWith<RegistrationValidationException> {
        Participant(name, "anna@example.com")
      }

    assertEquals(RegistrationErrorCode.INPUT_TOO_LONG, error.code)
  }

  @Test
  fun `should accept email of exactly 254 characters`() {
    val email = "a".repeat(242) + "@example.com"

    val participant = Participant("Anna", email)

    assertEquals(email, participant.email)
  }

  @Test
  fun `should reject email longer than 254 characters`() {
    val email = "a".repeat(243) + "@example.com"

    val error =
      assertFailsWith<RegistrationValidationException> {
        Participant("Anna", email)
      }

    assertEquals(RegistrationErrorCode.INPUT_TOO_LONG, error.code)
  }

  @Test
  fun `should reject email without a dot in the domain`() {
    val error =
      assertFailsWith<RegistrationValidationException> {
        Participant("Anna", "anna@example")
      }

    assertEquals(RegistrationErrorCode.INVALID_EMAIL, error.code)
  }
}
