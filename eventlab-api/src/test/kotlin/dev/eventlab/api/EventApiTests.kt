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
  fun `should return 400 with invalid email without occupying a seat`() {
    createEvent(capacity = 1)
    val requestBody =
      """
      {
          "name": "Anna",
          "email": "not-an-email"
      }
      """.trimIndent()
    val request =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)

    val registrationResult = mvc.perform(request)
    val eventResult = mvc.perform(get("/api/event"))

    registrationResult
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("INVALID_EMAIL"))
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(1))
  }

  @Test
  fun `should create registration and occupy a seat`() {
    createEvent(capacity = 1)
    val requestBody =
      """
      {
          "name": "Anna",
          "email": "anna@example.com"
      }
      """.trimIndent()
    val request =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)

    val registrationResult = mvc.perform(request)
    val eventResult = mvc.perform(get("/api/event"))

    registrationResult
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.registration.name").value("Anna"))
      .andExpect(
        jsonPath("$.registration.email").value("anna@example.com"),
      ).andExpect(jsonPath("$.registration.id").isNotEmpty())
      .andExpect(jsonPath("$.accessToken").isNotEmpty())
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
  }

  @Test
  fun `should reject duplicate email regardless of case`() {
    createEvent(capacity = 1)
    val originalRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Anna",
              "email": "anna@example.com"
          }
          """.trimIndent(),
        )
    mvc
      .perform(originalRequest)
      .andExpect(status().isCreated())
    val duplicateRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Another Anna",
              "email": "ANNA@example.com"
          }
          """.trimIndent(),
        )

    val registrationResult = mvc.perform(duplicateRequest)
    val eventResult = mvc.perform(get("/api/event"))

    registrationResult
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
  }

  @Test
  fun `should reject another participant when event is full`() {
    createEvent(capacity = 1)
    val originalRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Anna",
              "email": "anna@example.com"
          }
          """.trimIndent(),
        )
    mvc
      .perform(originalRequest)
      .andExpect(status().isCreated())
    val anotherRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Boris",
              "email": "boris@example.com"
          }
          """.trimIndent(),
        )

    val registrationResult = mvc.perform(anotherRequest)
    val eventResult = mvc.perform(get("/api/event"))

    registrationResult
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("EVENT_FULL"))
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
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
    val eventResult = mvc.perform(get("/api/event"))

    assertEquals(1, statuses.count { it == 201 })
    assertEquals(participantCount - 1, statuses.count { it == 409 })
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
  }

  @Test
  fun `should return registration with valid access token`() {
    createEvent()
    val createRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Anna",
              "email": "anna@example.com"
          }
          """.trimIndent(),
        )
    val responseBody =
      mvc
        .perform(createRequest)
        .andExpect(status().isCreated())
        .andReturn()
        .response
        .contentAsString
    val id = JsonPath.read<String>(responseBody, "$.registration.id")
    val accessToken = JsonPath.read<String>(responseBody, "$.accessToken")
    val readRequest =
      get("/api/registrations/{id}", id)
        .header("Authorization", "Bearer $accessToken")

    val result = mvc.perform(readRequest)

    result
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(id))
      .andExpect(jsonPath("$.name").value("Anna"))
      .andExpect(jsonPath("$.email").value("anna@example.com"))
      .andExpect(jsonPath("$.accessToken").doesNotExist())
  }

  @Test
  fun `should return 404 for registration with invalid access token`() {
    createEvent()
    val createRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Anna",
              "email": "anna@example.com"
          }
          """.trimIndent(),
        )
    val responseBody =
      mvc
        .perform(createRequest)
        .andExpect(status().isCreated())
        .andReturn()
        .response
        .contentAsString
    val id = JsonPath.read<String>(responseBody, "$.registration.id")
    val readRequest =
      get("/api/registrations/{id}", id)
        .header("Authorization", "Bearer invalid-token")

    val result = mvc.perform(readRequest)

    result
      .andExpect(status().isNotFound())
  }

  @Test
  fun `should return 404 for registration without access token`() {
    createEvent()
    val createRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Anna",
              "email": "anna@example.com"
          }
          """.trimIndent(),
        )
    val responseBody =
      mvc
        .perform(createRequest)
        .andExpect(status().isCreated())
        .andReturn()
        .response
        .contentAsString
    val id = JsonPath.read<String>(responseBody, "$.registration.id")
    val readRequest = get("/api/registrations/{id}", id)

    val result = mvc.perform(readRequest)

    result
      .andExpect(status().isNotFound())
  }

  @Test
  fun `should return 404 for nonexistent registration`() {
    val readRequest =
      get("/api/registrations/{id}", "missing-registration")
        .header("Authorization", "Bearer some-token")

    val result = mvc.perform(readRequest)

    result
      .andExpect(status().isNotFound())
  }

  @Test
  fun `should cancel registration and release a seat`() {
    createEvent(capacity = 1)
    val createRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Anna",
              "email": "anna@example.com"
          }
          """.trimIndent(),
        )
    val responseBody =
      mvc
        .perform(createRequest)
        .andExpect(status().isCreated())
        .andReturn()
        .response
        .contentAsString
    val id = JsonPath.read<String>(responseBody, "$.registration.id")
    val token = JsonPath.read<String>(responseBody, "$.accessToken")
    val cancelRequest =
      delete("/api/registrations/{id}", id)
        .header("Authorization", "Bearer $token")
    val readRequest =
      get("/api/registrations/{id}", id)
        .header("Authorization", "Bearer $token")

    val cancellationResult = mvc.perform(cancelRequest)
    val registrationResult = mvc.perform(readRequest)
    val eventResult = mvc.perform(get("/api/event"))

    cancellationResult
      .andExpect(status().isNoContent())
    registrationResult
      .andExpect(status().isNotFound())
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(1))
  }

  @Test
  fun `should reject cancellation with invalid token and preserve registration`() {
    createEvent(capacity = 1)
    val createRequest =
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
          """
          {
              "name": "Anna",
              "email": "anna@example.com"
          }
          """.trimIndent(),
        )
    val responseBody =
      mvc
        .perform(createRequest)
        .andExpect(status().isCreated())
        .andReturn()
        .response
        .contentAsString
    val id = JsonPath.read<String>(responseBody, "$.registration.id")
    val token = JsonPath.read<String>(responseBody, "$.accessToken")
    val cancelRequest =
      delete("/api/registrations/{id}", id)
        .header("Authorization", "Bearer invalid-token")
    val readRequest =
      get("/api/registrations/{id}", id)
        .header("Authorization", "Bearer $token")

    val cancellationResult = mvc.perform(cancelRequest)
    val registrationResult = mvc.perform(readRequest)
    val eventResult = mvc.perform(get("/api/event"))

    cancellationResult
      .andExpect(status().isNotFound())
    registrationResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(id))
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
  }

  @Test
  fun `should not store access token in database`() {
    createEvent()
    val responseBody =
      mvc
        .perform(registrationRequest("Anna", "anna@example.com"))
        .andReturn()
        .response
        .contentAsString
    val id = JsonPath.read<String>(responseBody, "$.registration.id")
    val accessToken = JsonPath.read<String>(responseBody, "$.accessToken")

    val storedRow =
      jdbcClient
        .sql("SELECT r::text FROM registrations r WHERE id = :id")
        .param("id", UUID.fromString(id))
        .query(String::class.java)
        .single()

    assertFalse(storedRow.contains(accessToken))
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
    createEvent(startsAt = "2026-11-14T15:00:00Z", capacity = 1)
    given(clock.instant()).willReturn(Instant.parse("2026-11-14T15:00:00Z"))

    val registrationResult =
      mvc.perform(registrationRequest("Anna", "anna@example.com"))
    val eventResult = mvc.perform(get("/api/event"))

    registrationResult
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("REGISTRATION_CLOSED"))
    eventResult
      .andExpect(jsonPath("$.registrationOpen").value(false))
      .andExpect(jsonPath("$.availableSeats").value(1))
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
    val firstBody =
      mvc
        .perform(
          registrationRequest("Anna", "anna@example.com", idempotencyKey),
        ).andExpect(status().isCreated())
        .andReturn()
        .response
        .contentAsString
    val firstId = JsonPath.read<String>(firstBody, "$.registration.id")

    val retryResult =
      mvc.perform(
        registrationRequest("Anna", "anna@example.com", idempotencyKey),
      )
    val eventResult = mvc.perform(get("/api/event"))

    retryResult
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.registration.id").value(firstId))
      .andExpect(jsonPath("$.accessToken").isNotEmpty())
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
  }

  @Test
  fun `should return the same registration regardless of idempotency key case`() {
    createEvent()
    val createdId = createRegistration("3f6c2b9e-8a41-4d7f-9c55-0e2a7b1d4f60")

    val result = register("3F6C2B9E-8A41-4D7F-9C55-0E2A7B1D4F60")

    result.andExpect(jsonPath("$.registration.id").value(createdId))
  }

  @Test
  fun `should replace access token when registration is retried`() {
    createEvent()
    val idempotencyKey = UUID.randomUUID().toString()
    val firstBody =
      mvc
        .perform(
          registrationRequest("Anna", "anna@example.com", idempotencyKey),
        ).andExpect(status().isCreated())
        .andReturn()
        .response
        .contentAsString
    val retryBody =
      mvc
        .perform(
          registrationRequest("Anna", "anna@example.com", idempotencyKey),
        ).andExpect(status().isCreated())
        .andReturn()
        .response
        .contentAsString
    val id = JsonPath.read<String>(retryBody, "$.registration.id")
    val firstToken = JsonPath.read<String>(firstBody, "$.accessToken")
    val retryToken = JsonPath.read<String>(retryBody, "$.accessToken")

    val firstTokenResult =
      mvc.perform(
        get("/api/registrations/{id}", id)
          .header("Authorization", "Bearer $firstToken"),
      )
    val retryTokenResult =
      mvc.perform(
        get("/api/registrations/{id}", id)
          .header("Authorization", "Bearer $retryToken"),
      )

    firstTokenResult
      .andExpect(status().isNotFound())
    retryTokenResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(id))
  }

  @Test
  fun `should reject idempotency key reused with different data`() {
    createEvent(capacity = 1)
    val idempotencyKey = UUID.randomUUID().toString()
    mvc
      .perform(registrationRequest("Anna", "anna@example.com", idempotencyKey))
      .andExpect(status().isCreated())

    val reuseResult =
      mvc.perform(
        registrationRequest("Boris", "boris@example.com", idempotencyKey),
      )
    val eventResult = mvc.perform(get("/api/event"))

    reuseResult
      .andExpect(status().isUnprocessableContent())
      .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"))
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
  }

  @Test
  fun `should return 400 with malformed idempotency key without occupying the seat`() {
    createEvent(capacity = 1)
    val registrationResult =
      mvc.perform(
        registrationRequest("Anna", "anna@example.com", "not-a-uuid"),
      )
    val eventResult = mvc.perform(get("/api/event"))

    registrationResult
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"))
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(1))
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
    val eventResult = mvc.perform(get("/api/event"))

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
    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
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

  private fun createRegistration(idempotencyKey: String): String {
    val body = register(idempotencyKey).andReturn().response.contentAsString

    return JsonPath.read(body, "$.registration.id")
  }

  private fun register(idempotencyKey: String): ResultActions =
    mvc.perform(
      registrationRequest("Anna", "anna@example.com", idempotencyKey),
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
}
