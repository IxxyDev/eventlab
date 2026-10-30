package dev.eventlab.api.event

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventTest {
  @Test
  fun `should leave free seats while registrations are below capacity`() {
    val event = event(capacity = 3, registrationCount = 2)

    assertEquals(1, event.availableSeats)
    assertTrue(event.hasAvailableSeats)
  }

  @Test
  fun `should have no free seats once every seat is taken`() {
    val event = event(capacity = 3, registrationCount = 3)

    assertEquals(0, event.availableSeats)
    assertFalse(event.hasAvailableSeats)
  }

  @Test
  fun `should keep registration open one second before the event starts`() {
    val event = event()

    val open = event.isRegistrationOpen(Instant.parse("2026-11-14T14:59:59Z"))

    assertTrue(open)
  }

  @Test
  fun `should close registration at the moment the event starts`() {
    val event = event()

    val open = event.isRegistrationOpen(Instant.parse("2026-11-14T15:00:00Z"))

    assertFalse(open)
  }

  private fun event(
    capacity: Int = 1,
    registrationCount: Int = 0,
  ): Event =
    Event(
      id = "sound-workshop",
      organizationName = "Mixer school",
      title = "Sound workshop",
      description = "Description",
      startsAt = Instant.parse("2026-11-14T15:00:00Z"),
      endsAt = Instant.parse("2026-11-14T17:00:00Z"),
      timeZone = ZoneId.of("Europe/Moscow"),
      venue = "Studio",
      capacity = capacity,
      registrationCount = registrationCount,
    )
}
