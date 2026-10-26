package dev.eventlab.api.registration

class Participant private constructor(
  val name: String,
  val email: String,
) {
  companion object {
    private val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    fun of(
      name: String,
      email: String,
    ): Participant {
      val trimmedName = name.trim()
      val trimmedEmail = email.trim()

      if (trimmedName.isEmpty() || trimmedEmail.isEmpty()) {
        throw RegistrationValidationException(
          RegistrationErrorCode.REQUIRED_FIELDS,
        )
      }

      if (trimmedName.length > 100 || trimmedEmail.length > 254) {
        throw RegistrationValidationException(
          RegistrationErrorCode.INPUT_TOO_LONG,
        )
      }

      if (!emailPattern.matches(trimmedEmail)) {
        throw RegistrationValidationException(
          RegistrationErrorCode.INVALID_EMAIL,
        )
      }

      return Participant(trimmedName, trimmedEmail)
    }
  }
}
