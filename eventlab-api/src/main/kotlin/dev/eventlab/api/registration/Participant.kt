package dev.eventlab.api.registration

private val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

class Participant(
  rawName: String,
  rawEmail: String,
) {
  val name = rawName.trim()
  val email = rawEmail.trim()

  init {
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
  }
}
