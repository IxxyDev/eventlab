package dev.eventlab.api

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import javax.sql.DataSource
import kotlin.test.assertEquals

@SpringBootTest
@Import(TestContainersConfiguration::class)
class EventApiTests {
  @Autowired
  private lateinit var jdbcClient: JdbcClient

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  @Autowired
  private lateinit var dataSource: DataSource
  private lateinit var mvc: MockMvc

  @BeforeEach
  fun setUp() {
    jdbcClient.sql("DELETE FROM registrations").update()
    mvc = createMvc()
  }

  @Test
  fun `should return 400 with invalid email without occupying a seat`() {
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
    val participantCount = 10
    val connections = (1..participantCount).map { dataSource.connection }
    connections.forEach { it.close() }
    val startLine = CyclicBarrier(participantCount)
    val requests =
      (1..participantCount).map { number ->
        val server = createMvc()
        Callable {
          startLine.await()
          server
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
  fun `should keep seat occupied after service restart`() {
    val requestBody =
      """
      {
        "name": "Anna",
        "email": "anna@example.com"
      }
      """.trimIndent()
    mvc.perform(
      post("/api/event/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody),
    )
    val restartedMvc = createMvc()

    val eventResult = restartedMvc.perform(get("/api/event"))

    eventResult
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.availableSeats").value(0))
  }

  @Test
  fun `should reject duplicate email after service restart`() {
    mvc.perform(registrationRequest("Anna", "anna@example.com"))
    val restartedMvc = createMvc()

    val result =
      restartedMvc.perform(registrationRequest("Anna", "anna@example.com"))

    result
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
  }

  @Test
  fun `should return registration after service restart`() {
    val responseBody =
      mvc
        .perform(registrationRequest("Anna", "anna@example.com"))
        .andReturn()
        .response
        .contentAsString
    val id = JsonPath.read<String>(responseBody, "$.registration.id")
    val accessToken = JsonPath.read<String>(responseBody, "$.accessToken")
    val restartedMvc = createMvc()
    val readRequest =
      get("/api/registrations/{id}", id)
        .header("Authorization", "Bearer $accessToken")

    val result = restartedMvc.perform(readRequest)

    result
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(id))
  }

  private fun createMvc(): MockMvc {
    val eventRepository = EventRepository(jdbcClient)
    val registrationRepository = RegistrationRepository(jdbcClient)

    return MockMvcBuilders
      .standaloneSetup(
        EventController(EventService(eventRepository, registrationRepository)),
        RegistrationController(
          RegistrationService(
            eventRepository,
            registrationRepository,
            transactionTemplate,
          ),
        ),
      ).setControllerAdvice(ApiExceptionHandler())
      .build()
  }

  private fun registrationRequest(
    name: String,
    email: String,
  ): MockHttpServletRequestBuilder =
    post("/api/event/registrations")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name": "$name", "email": "$email"}""")
}
