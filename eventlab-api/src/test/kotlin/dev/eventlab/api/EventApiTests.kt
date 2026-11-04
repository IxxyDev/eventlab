package dev.eventlab.api

import com.jayway.jsonpath.JsonPath
import dev.eventlab.api.event.EVENT_ID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfiguration::class)
class EventApiTests {
  @Autowired
  private lateinit var mvc: MockMvc

  @MockitoBean
  private lateinit var clock: Clock

  @Autowired
  private lateinit var jdbcClient: JdbcClient

  @Autowired
  private lateinit var dataSource: DataSource

  @BeforeEach
  fun setUp() {
    jdbcClient.sql("TRUNCATE registrations, events, organizations").update()
    given(clock.instant()).willReturn(Instant.parse("2026-11-01T09:00:00Z"))
  }

  @Test
  fun `should reject invalid email`() {
    createEvent()

    val registrationResult =
      mvc.perform(
        registrationRequest("Anna", "not-an-email"),
      )

    registrationResult
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("INVALID_EMAIL"))
  }

  @Test
  fun `should create registration`() {
    createEvent()

    val registrationResult =
      mvc.perform(registrationRequest("Anna", "anna@example.com"))

    registrationResult
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.registration.name").value("Anna"))
      .andExpect(
        jsonPath("$.registration.email").value("anna@example.com"),
      ).andExpect(jsonPath("$.registration.id").isNotEmpty())
      .andExpect(jsonPath("$.accessToken").isNotEmpty())
  }

  @Test
  fun `should occupy a seat on registration`() {
    createEvent(capacity = 1)

    mvc.perform(registrationRequest("Anna", "anna@example.com"))

    expectAvailableSeats(0)
  }

  @Test
  fun `should reject duplicate email regardless of case`() {
    createEvent(capacity = 2)
    createRegistration("Anna", "anna@example.com")

    val registrationResult =
      mvc.perform(registrationRequest("Another Anna", "ANNA@example.com"))

    registrationResult
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
  }

  @Test
  fun `should reject another participant when event is full`() {
    createEvent(capacity = 1)
    createRegistration("Anna", "anna@example.com")

    val registrationResult =
      mvc.perform(registrationRequest("Boris", "boris@example.com"))

    registrationResult
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("EVENT_FULL"))
  }

  @Test
  fun `should give the last seat to only one of many simultaneous participants`() {
    createEvent(capacity = 1)
    val participantCount = 10
    warmUpConnectionPool(participantCount)
    val startLine = CyclicBarrier(participantCount)
    val requests =
      (1..participantCount).map { number ->
        Callable {
          startLine.await()
          mvc
            .perform(
              registrationRequest(
                "Participant $number",
                "participant$number@example.com",
              ),
            ).andReturn()
            .response
            .status
        }
      }

    val statuses =
      Executors.newFixedThreadPool(participantCount).use { executor ->
        executor.invokeAll(requests).map { it.get() }
      }

    assertEquals(1, statuses.count { it == 201 })
    assertEquals(participantCount - 1, statuses.count { it == 409 })
    expectAvailableSeats(0)
  }

  @Test
  fun `should return registration with valid access token`() {
    createEvent()
    val registration = createRegistration("Anna", "anna@example.com")

    val result = readRegistration(registration.id, registration.accessToken)

    result
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(registration.id))
      .andExpect(jsonPath("$.name").value("Anna"))
      .andExpect(jsonPath("$.email").value("anna@example.com"))
  }

  @Test
  fun `should not return access token when reading registration`() {
    createEvent()
    val registration = createRegistration()

    val result = readRegistration(registration.id, registration.accessToken)

    result
      .andExpect(jsonPath("$.accessToken").doesNotExist())
  }

  @Test
  fun `should return 404 for registration with access token of another registration`() {
    createEvent()
    val registration = createRegistration("Anna", "anna@example.com")
    val anotherRegistration = createRegistration("Boris", "boris@example.com")

    val result =
      readRegistration(registration.id, anotherRegistration.accessToken)

    result
      .andExpect(status().isNotFound())
  }

  @Test
  fun `should return 404 for registration without access token`() {
    createEvent()
    val registration = createRegistration()
    val readRequest = get("/api/registrations/{id}", registration.id)

    val result = mvc.perform(readRequest)

    result
      .andExpect(status().isNotFound())
  }

  @Test
  fun `should return 404 for nonexistent registration`() {
    val result = readRegistration("missing-registration", "some-token")

    result
      .andExpect(status().isNotFound())
  }

  @Test
  fun `should cancel registration`() {
    createEvent()
    val registration = createRegistration()

    val cancellationResult =
      cancelRegistration(registration.id, registration.accessToken)

    cancellationResult
      .andExpect(status().isNoContent())
  }

  @Test
  fun `should release a seat on cancellation`() {
    createEvent(capacity = 1)
    val registration = createRegistration()

    cancelRegistration(registration.id, registration.accessToken)

    expectAvailableSeats(1)
  }

  @Test
  fun `should not cancel registration with access token of another registration`() {
    createEvent()
    val registration = createRegistration("Anna", "anna@example.com")
    val anotherRegistration = createRegistration("Boris", "boris@example.com")

    val cancellationResult =
      cancelRegistration(registration.id, anotherRegistration.accessToken)

    cancellationResult
      .andExpect(status().isNotFound())
  }

  @Test
  fun `should keep a seat on cancellation with access token of another registration`() {
    createEvent(capacity = 2)
    val registration = createRegistration("Anna", "anna@example.com")
    val anotherRegistration = createRegistration("Boris", "boris@example.com")

    cancelRegistration(registration.id, anotherRegistration.accessToken)

    expectAvailableSeats(0)
  }

  @Test
  fun `should not store access token in database`() {
    createEvent()
    val registration = createRegistration()

    val storedRow =
      jdbcClient
        .sql("SELECT r::text FROM registrations r WHERE id = :id")
        .param("id", UUID.fromString(registration.id))
        .query(String::class.java)
        .single()

    assertFalse(storedRow.contains(registration.accessToken))
  }

  @Test
  fun `should return event schedule with event time zone`() {
    createEvent(
      startsAt = "2026-11-14T15:00:00Z",
      endsAt = "2026-11-14T17:00:00Z",
      timeZone = "Europe/Moscow",
    )

    val eventResult = mvc.perform(get("/api/event"))

    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.startsAt").value("2026-11-14T15:00:00Z"))
      .andExpect(jsonPath("$.endsAt").value("2026-11-14T17:00:00Z"))
      .andExpect(jsonPath("$.timeZone").value("Europe/Moscow"))
  }

  @Test
  fun `should report registration as open before event starts`() {
    createEvent(startsAt = "2026-11-14T15:00:00Z")
    given(clock.instant()).willReturn(Instant.parse("2026-11-01T09:00:00Z"))

    val eventResult = mvc.perform(get("/api/event"))

    eventResult
      .andExpect(jsonPath("$.registrationOpen").value(true))
  }

  @Test
  fun `should list events from the nearest with their organizers`() {
    createEvent(
      id = "sound-workshop",
      organizationName = "Школа звукорежиссуры «Микшер»",
      startsAt = "2026-11-14T15:00:00Z",
    )
    createEvent(
      id = "stage-lighting",
      organizationName = "Лаборатория света «Луч»",
      startsAt = "2026-11-07T14:00:00Z",
      timeZone = "Asia/Yekaterinburg",
    )

    val eventsResult = mvc.perform(get("/api/events"))

    eventsResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].id").value("stage-lighting"))
      .andExpect(
        jsonPath("$[0].organizationName").value("Лаборатория света «Луч»"),
      ).andExpect(jsonPath("$[0].timeZone").value("Asia/Yekaterinburg"))
      .andExpect(jsonPath("$[1].id").value("sound-workshop"))
      .andExpect(
        jsonPath("$[1].organizationName")
          .value("Школа звукорежиссуры «Микшер»"),
      )
  }

  @Test
  fun `should return event by its id`() {
    createEvent(id = "sound-workshop")
    createEvent(
      id = "stage-lighting",
      organizationName = "Лаборатория света «Луч»",
    )

    val eventResult = mvc.perform(get("/api/events/{id}", "stage-lighting"))

    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("stage-lighting"))
      .andExpect(
        jsonPath("$.organizationName").value("Лаборатория света «Луч»"),
      )
  }

  @Test
  fun `should return 404 for nonexistent event`() {
    createEvent()

    val eventResult = mvc.perform(get("/api/events/{id}", "missing-event"))

    eventResult
      .andExpect(status().isNotFound())
  }

  @Test
  fun `should reject registration once event has started`() {
    createEvent(startsAt = "2026-11-14T15:00:00Z")
    given(clock.instant()).willReturn(Instant.parse("2026-11-15T09:00:00Z"))

    val registrationResult =
      mvc.perform(registrationRequest("Anna", "anna@example.com"))

    registrationResult
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("REGISTRATION_CLOSED"))
  }

  @Test
  fun `should report registration as closed once event has started`() {
    createEvent(startsAt = "2026-11-14T15:00:00Z")
    given(clock.instant()).willReturn(Instant.parse("2026-11-15T09:00:00Z"))

    val eventResult = mvc.perform(get("/api/event"))

    eventResult
      .andExpect(jsonPath("$.registrationOpen").value(false))
  }

  @Test
  fun `should accept registration one second before event start`() {
    createEvent(startsAt = "2026-11-14T15:00:00Z")
    given(clock.instant()).willReturn(Instant.parse("2026-11-14T14:59:59Z"))

    val registrationResult =
      mvc.perform(registrationRequest("Anna", "anna@example.com"))

    registrationResult
      .andExpect(status().isCreated())
  }

  @Test
  fun `should return the same registration when retried with the same idempotency key`() {
    createEvent(capacity = 1)
    val idempotencyKey = UUID.randomUUID().toString()
    val registration = createRegistration(idempotencyKey = idempotencyKey)

    val retryResult =
      mvc.perform(
        registrationRequest("Anna", "anna@example.com", idempotencyKey),
      )

    retryResult
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.registration.id").value(registration.id))
      .andExpect(jsonPath("$.accessToken").isNotEmpty())
    expectAvailableSeats(0)
  }

  @Test
  fun `should return the same registration regardless of idempotency key case`() {
    createEvent()
    val registration =
      createRegistration(
        idempotencyKey = "3f6c2b9e-8a41-4d7f-9c55-0e2a7b1d4f60",
      )

    val result = register("3F6C2B9E-8A41-4D7F-9C55-0E2A7B1D4F60")

    result.andExpect(jsonPath("$.registration.id").value(registration.id))
  }

  @Test
  fun `should replace access token when registration is retried`() {
    createEvent()
    val idempotencyKey = UUID.randomUUID().toString()
    val first = createRegistration(idempotencyKey = idempotencyKey)
    val retry = createRegistration(idempotencyKey = idempotencyKey)

    val firstTokenResult =
      readRegistration(retry.id, first.accessToken)
    val retryTokenResult =
      readRegistration(retry.id, retry.accessToken)

    firstTokenResult
      .andExpect(status().isNotFound())
    retryTokenResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(retry.id))
  }

  @Test
  fun `should reject idempotency key reused with different data`() {
    createEvent()
    val idempotencyKey = UUID.randomUUID().toString()
    createRegistration(idempotencyKey = idempotencyKey)

    val reuseResult =
      mvc.perform(
        registrationRequest("Boris", "boris@example.com", idempotencyKey),
      )

    reuseResult
      .andExpect(status().isUnprocessableContent())
      .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"))
  }

  @Test
  fun `should reject malformed idempotency key`() {
    createEvent()

    val registrationResult =
      mvc.perform(
        registrationRequest("Anna", "anna@example.com", "not-a-uuid"),
      )

    registrationResult
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"))
  }

  @Test
  fun `should create one registration for simultaneous retries with the same idempotency key`() {
    createEvent(capacity = 1)
    val attemptCount = 5
    val idempotencyKey = UUID.randomUUID().toString()
    warmUpConnectionPool(attemptCount)
    val startLine = CyclicBarrier(attemptCount)
    val requests =
      (1..attemptCount).map {
        Callable {
          startLine.await()
          mvc
            .perform(
              registrationRequest("Anna", "anna@example.com", idempotencyKey),
            ).andReturn()
            .response
        }
      }

    val responses =
      Executors.newFixedThreadPool(attemptCount).use { executor ->
        executor.invokeAll(requests).map { it.get() }
      }

    assertEquals(List(attemptCount) { 201 }, responses.map { it.status })
    assertEquals(
      1,
      responses
        .map {
          JsonPath.read<String>(
            it.contentAsString,
            "$.registration.id",
          )
        }.distinct()
        .size,
    )
    expectAvailableSeats(0)
  }

  private fun createEvent(
    id: String = EVENT_ID,
    organizationName: String = "Школа звукорежиссуры «Микшер»",
    startsAt: String = "2026-11-14T15:00:00Z",
    endsAt: String = "2026-11-14T17:00:00Z",
    timeZone: String = "Europe/Moscow",
    capacity: Int = 20,
  ) {
    val organizationId = UUID.randomUUID().toString()

    jdbcClient
      .sql("INSERT INTO organizations (id, name) VALUES (:id, :name)")
      .param("id", organizationId)
      .param("name", organizationName)
      .update()
    jdbcClient
      .sql(
        """
        INSERT INTO events (
          id,
          organization_id,
          title,
          description,
          starts_at,
          ends_at,
          time_zone,
          venue,
          capacity
        )
        VALUES (
          :id,
          :organizationId,
          'Как устроен звук на концерте',
          'Разберём путь звука от микрофона до колонок.',
          :startsAt,
          :endsAt,
          :timeZone,
          'Пространство «Сцена», зал № 2',
          :capacity
        )
        """.trimIndent(),
      ).param("id", id)
      .param("organizationId", organizationId)
      .param("startsAt", OffsetDateTime.parse(startsAt))
      .param("endsAt", OffsetDateTime.parse(endsAt))
      .param("timeZone", timeZone)
      .param("capacity", capacity)
      .update()
  }

  private fun createRegistration(
    name: String = "Anna",
    email: String = "anna@example.com",
    idempotencyKey: String? = null,
  ): CreatedRegistration {
    val body =
      mvc
        .perform(registrationRequest(name, email, idempotencyKey))
        .andReturn()
        .response
        .contentAsString

    return CreatedRegistration(
      id = JsonPath.read(body, "$.registration.id"),
      accessToken = JsonPath.read(body, "$.accessToken"),
    )
  }

  private fun register(idempotencyKey: String): ResultActions =
    mvc.perform(
      registrationRequest("Anna", "anna@example.com", idempotencyKey),
    )

  private fun readRegistration(
    id: String,
    accessToken: String,
  ): ResultActions =
    mvc.perform(
      get("/api/registrations/{id}", id)
        .header("Authorization", "Bearer $accessToken"),
    )

  private fun cancelRegistration(
    id: String,
    accessToken: String,
  ): ResultActions =
    mvc.perform(
      delete("/api/registrations/{id}", id)
        .header("Authorization", "Bearer $accessToken"),
    )

  private fun registrationRequest(
    name: String,
    email: String,
    idempotencyKey: String? = null,
  ): MockHttpServletRequestBuilder {
    val request =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"name": "$name", "email": "$email"}""")

    if (idempotencyKey != null) {
      request.header("Idempotency-Key", idempotencyKey)
    }

    return request
  }

  private fun warmUpConnectionPool(size: Int) {
    val connections = (1..size).map { dataSource.connection }
    connections.forEach { it.close() }
  }

  private fun expectAvailableSeats(count: Int) {
    mvc
      .perform(get("/api/event"))
      .andExpect(jsonPath("$.availableSeats").value(count))
  }
}

private data class CreatedRegistration(
  val id: String,
  val accessToken: String,
)
