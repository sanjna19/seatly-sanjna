package com.seatly.booking.desk

import com.seatly.user.CreateUserRequest
import com.seatly.user.LoginRequest
import com.seatly.user.LoginResponse
import com.seatly.user.UserRepository
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
class DeskControllerTest {
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Inject
    lateinit var deskRepository: DeskRepository

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var authToken: String

    @BeforeEach
    fun setUp() {
        deskRepository.deleteAll()
        userRepository.deleteAll()

        val createUserRequest =
            CreateUserRequest(
                email = "test@example.com",
                password = "password123",
                fullName = "Test User",
            )
        client.toBlocking().exchange(
            HttpRequest.POST("/users", createUserRequest),
            Argument.of(Any::class.java),
        )

        val loginRequest =
            LoginRequest(
                email = "test@example.com",
                password = "password123",
            )
        val loginResponse =
            client.toBlocking().retrieve(
                HttpRequest.POST("/users/login", loginRequest),
                LoginResponse::class.java,
            )

        authToken = loginResponse.token
    }

    @Test
    fun `should not allow creating desk without token`() {
        val createRequest =
            CreateDeskRequest(
                name = "Desk without token",
                location = "Somewhere",
            )

        val exception =
            assertThrows(HttpClientResponseException::class.java) {
                client.toBlocking().exchange(
                    HttpRequest.POST("/desks", createRequest),
                    DeskResponse::class.java,
                )
            }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }

    @Test
    fun `should not allow listing desks without token`() {
        val exception =
            assertThrows(HttpClientResponseException::class.java) {
                client.toBlocking().exchange(
                    HttpRequest.GET<Any>("/desks"),
                    Argument.listOf(DeskResponse::class.java),
                )
            }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }

    @Test
    fun `should create two desks and list them successfully`() {
        val createRequest1 =
            CreateDeskRequest(
                name = "Desk 1",
                location = "Floor 1, Zone A",
            )
        val createResponse1 =
            client.toBlocking().exchange(
                HttpRequest
                    .POST("/desks", createRequest1)
                    .bearerAuth(authToken),
                DeskResponse::class.java,
            )
        assertEquals(HttpStatus.CREATED, createResponse1.status)
        assertNotNull(createResponse1.body())
        assertEquals("Desk 1", createResponse1.body()?.name)
        assertEquals("Floor 1, Zone A", createResponse1.body()?.location)

        val createRequest2 =
            CreateDeskRequest(
                name = "Desk 2",
                location = "Floor 2, Zone B",
            )
        val createResponse2 =
            client.toBlocking().exchange(
                HttpRequest
                    .POST("/desks", createRequest2)
                    .bearerAuth(authToken),
                DeskResponse::class.java,
            )
        assertEquals(HttpStatus.CREATED, createResponse2.status)
        assertNotNull(createResponse2.body())
        assertEquals("Desk 2", createResponse2.body()?.name)
        assertEquals("Floor 2, Zone B", createResponse2.body()?.location)

        val listResponse =
            client.toBlocking().exchange(
                HttpRequest
                    .GET<Any>("/desks")
                    .bearerAuth(authToken),
                Argument.listOf(DeskResponse::class.java),
            )
        assertEquals(HttpStatus.OK, listResponse.status)
        assertNotNull(listResponse.body())
        assertEquals(2, listResponse.body()?.size)

        val desks = listResponse.body()!!
        assertEquals("Desk 1", desks[0].name)
        assertEquals("Floor 1, Zone A", desks[0].location)
        assertEquals("Desk 2", desks[1].name)
        assertEquals("Floor 2, Zone B", desks[1].location)
    }
}
