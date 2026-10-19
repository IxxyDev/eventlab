package dev.eventlab.eventlab_api

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import com.jayway.jsonpath.JsonPath

class EventControllerTests {
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        val eventService = EventService()
        mvc = MockMvcBuilders
            .standaloneSetup(EventController(eventService))
            .build()
    }

    @Test
    fun `should return 400 with invalid email without occupying a seat`() {
        val requestBody = """
        {
            "name": "Anna",
            "email": "not-an-email"
        }
        """.trimIndent()
        val request = post("/api/event/registrations")
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
        val requestBody = """
        {
            "name": "Anna",
            "email": "anna@example.com"
        }
        """.trimIndent()
        val request = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)

        val registrationResult = mvc.perform(request)
        val eventResult = mvc.perform(get("/api/event"))

        registrationResult
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.registration.name").value("Anna"))
            .andExpect(jsonPath("$.registration.email").value("anna@example.com"))
            .andExpect(jsonPath("$.registration.id").isNotEmpty())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
        eventResult
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availableSeats").value(0))
    }

    @Test
    fun `should reject duplicate email regardless of case`() {
        val originalRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Anna",
                    "email": "anna@example.com"
                }
                """.trimIndent()
            )
        mvc.perform(originalRequest)
            .andExpect(status().isCreated())
        val duplicateRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Another Anna",
                    "email": "ANNA@example.com"
                }
                """.trimIndent()
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
        val originalRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Anna",
                    "email": "anna@example.com"
                }
                """.trimIndent()
            )
        mvc.perform(originalRequest)
            .andExpect(status().isCreated())
        val anotherRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Boris",
                    "email": "boris@example.com"
                }
                """.trimIndent()
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
    fun `should return registration with valid access token`() {
        val createRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Anna",
                    "email": "anna@example.com"
                }
                """.trimIndent()
            )
        val responseBody = mvc.perform(createRequest)
            .andExpect(status().isCreated())
            .andReturn()
            .response
            .contentAsString
        val id = JsonPath.read<String>(responseBody, "$.registration.id")
        val accessToken = JsonPath.read<String>(responseBody, "$.accessToken")
        val readRequest = get("/api/registrations/{id}", id)
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
        val createRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Anna",
                    "email": "anna@example.com"
                }
                """.trimIndent()
            )
        val responseBody = mvc.perform(createRequest)
            .andExpect(status().isCreated())
            .andReturn()
            .response
            .contentAsString
        val id = JsonPath.read<String>(responseBody, "$.registration.id")
        val readRequest = get("/api/registrations/{id}", id)
            .header("Authorization", "Bearer invalid-token")

        val result = mvc.perform(readRequest)

        result
            .andExpect(status().isNotFound())
    }

    @Test
    fun `should return 404 for registration without access token`() {
        val createRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Anna",
                    "email": "anna@example.com"
                }
                """.trimIndent()
            )
        val responseBody = mvc.perform(createRequest)
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
        val readRequest = get("/api/registrations/{id}", "missing-registration")
            .header("Authorization", "Bearer some-token")

        val result = mvc.perform(readRequest)

        result
            .andExpect(status().isNotFound())
    }

    @Test
    fun `should cancel registration and release a seat`() {
        val createRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Anna",
                    "email": "anna@example.com"
                }
                """.trimIndent()
            )
        val responseBody = mvc.perform(createRequest)
            .andExpect(status().isCreated())
            .andReturn()
            .response
            .contentAsString
        val id = JsonPath.read<String>(responseBody, "$.registration.id")
        val token = JsonPath.read<String>(responseBody, "$.accessToken")
        val cancelRequest = delete("/api/registrations/{id}", id)
            .header("Authorization", "Bearer $token")
        val readRequest = get("/api/registrations/{id}", id)
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
        val createRequest = post("/api/event/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                    "name": "Anna",
                    "email": "anna@example.com"
                }
                """.trimIndent()
            )
        val responseBody = mvc.perform(createRequest)
            .andExpect(status().isCreated())
            .andReturn()
            .response
            .contentAsString
        val id = JsonPath.read<String>(responseBody, "$.registration.id")
        val token = JsonPath.read<String>(responseBody, "$.accessToken")
        val cancelRequest = delete("/api/registrations/{id}", id)
            .header("Authorization", "Bearer invalid-token")
        val readRequest = get("/api/registrations/{id}", id)
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
}
