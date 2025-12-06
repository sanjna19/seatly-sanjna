
package com.seatly.user

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@MicronautTest
class UserControllerTest {
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Inject
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun clearDatabase() {
        userRepository.deleteAll()
    }

    @Test
    fun `should create user and login successfully`() {
        val createRequest =
            CreateUserRequest(
                email = "test@example.com",
                password = "password123",
                fullName = "Test User",
            )

        val createResponse =
            client.toBlocking().exchange(
                HttpRequest.POST("/users", createRequest),
                UserResponse::class.java,
            )

        assertEquals(HttpStatus.CREATED, createResponse.status)
        assertNotNull(createResponse.body())
        assertEquals("test@example.com", createResponse.body()?.email)

        val loginRequest =
            LoginRequest(
                email = "test@example.com",
                password = "password123",
            )

        val loginResponse =
            client.toBlocking().exchange(
                HttpRequest.POST("/users/login", loginRequest),
                LoginResponse::class.java,
            )

        assertEquals(HttpStatus.OK, loginResponse.status)
        assertNotNull(loginResponse.body())
        assertNotNull(loginResponse.body()?.token)
    }
}
