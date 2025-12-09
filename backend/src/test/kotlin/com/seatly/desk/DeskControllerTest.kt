package com.seatly.desk

import com.seatly.user.CreateUserRequest
import com.seatly.user.LoginRequest
import com.seatly.user.LoginResponse
import com.seatly.user.UserRepository
import com.seatly.user.UserResponse
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@MicronautTest(transactional = false)
class DeskControllerTest {
  @Inject
  @field:Client("/")
  lateinit var client: HttpClient

  @Inject
  lateinit var deskRepository: DeskRepository

  @Inject
  lateinit var userRepository: UserRepository

  @Inject
  lateinit var bookingRepository: BookingRepository

  private lateinit var authUser: UserResponse
  private lateinit var authToken: String

  @BeforeEach
  fun setUp() {
    bookingRepository.deleteAll()
    deskRepository.deleteAll()
    userRepository.deleteAll()

    val createUserRequest =
      CreateUserRequest(
        email = "test@example.com",
        password = "password123",
        fullName = "Test User",
      )
    authUser =
      client.toBlocking().retrieve(
        HttpRequest.POST("/users", createUserRequest),
        Argument.of(UserResponse::class.java),
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
  fun `should not allow retrieving availability without token`() {
    val desk =
      createDesk(
        client = client,
        authToken = authToken,
        name = "Desk 1",
        location = "Floor 1, Zone B",
      )

    val now = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES)
    val startAt = now
    val endAt = now.plusHours(2)

    val path =
      "/desks/${desk.id}/availability?startAt=$startAt&endAt=$endAt"

    val exception =
      assertThrows(HttpClientResponseException::class.java) {
        client.toBlocking().exchange(
          HttpRequest.GET<Any>(path),
          Argument.listOf(AvailabilityResponse::class.java),
        )
      }
    assertEquals(HttpStatus.UNAUTHORIZED, exception.status)

    val okResponse =
      client.toBlocking().exchange(
        HttpRequest
          .GET<Any>(path)
          .bearerAuth(authToken),
        Argument.listOf(AvailabilityResponse::class.java),
      )

    assertEquals(HttpStatus.OK, okResponse.status)
    assertNotNull(okResponse.body())
  }

  @Test
  fun `should not allow creating booking without token`() {
    val now = LocalDateTime.now().plusHours(1)
    val createRequest =
      CreateBookingRequest(
        startAt = now,
        endAt = now.plusHours(1),
      )

    val exception =
      assertThrows(HttpClientResponseException::class.java) {
        client.toBlocking().exchange(
          HttpRequest.POST("/desks/1/booking", createRequest),
          BookingResponse::class.java,
        )
      }

    assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
  }

  @Test
  fun `should create two desks and list them successfully`() {
    val desk1 =
      createDesk(
        client = client,
        authToken = authToken,
        name = "Desk 1",
        location = "Floor 1, Zone A",
      )
    assertEquals("Desk 1", desk1.name)
    assertEquals("Floor 1, Zone A", desk1.location)

    val desk2 =
      createDesk(
        client = client,
        authToken = authToken,
        name = "Desk 2",
        location = "Floor 2, Zone B",
      )
    assertEquals("Desk 2", desk2.name)
    assertEquals("Floor 2, Zone B", desk2.location)

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

  @Test
  fun `should create booking successfully`() {
    val desk: DeskResponse =
      createDesk(
        client = client,
        authToken = authToken,
        name = "Booking Desk 1",
        location = "Booking Floor 1",
      )
    val deskId = desk.id!!

    val now = LocalDateTime.now().plusHours(1)
    val createBookingRequest =
      CreateBookingRequest(
        startAt = now,
        endAt = now.plusHours(1),
      )

    val bookingResponse =
      client.toBlocking().exchange(
        HttpRequest
          .POST("desks/$deskId/bookings", createBookingRequest)
          .bearerAuth(authToken),
        BookingResponse::class.java,
      )

    assertEquals(HttpStatus.CREATED, bookingResponse.status)
    val booking = bookingResponse.body()
    assertNotNull(booking)
    assertEquals(deskId, booking!!.deskId)
    assertEquals(authUser.id, booking.userId)
    assertEquals(createBookingRequest.startAt.truncatedTo(ChronoUnit.MINUTES), booking.startAt)
    assertEquals(createBookingRequest.endAt.truncatedTo(ChronoUnit.MINUTES), booking.endAt)
  }

  @Test
  fun `Should list available desk bookings within time range`() {
    val desk: DeskResponse =
      createDesk(
        client = client,
        authToken = authToken,
        name = "Availability Desk 1",
        location = "Availability Floor 1",
      )
    val deskId = desk.id!!

    val baseTime =
      LocalDateTime
        .now()
        .plusHours(1)
        .truncatedTo(ChronoUnit.HOURS)
    val bookingStart = baseTime.plusHours(1)
    val bookingEnd = bookingStart.plusMinutes(30)
    val createBookingRequest =
      CreateBookingRequest(
        startAt = bookingStart,
        endAt = bookingEnd,
      )
    val bookingResponse =
      client.toBlocking().exchange(
        HttpRequest
          .POST("desks/$deskId/bookings", createBookingRequest)
          .bearerAuth(authToken),
        BookingResponse::class.java,
      )
    assertEquals(HttpStatus.CREATED, bookingResponse.status)

    val availabilityStart = baseTime
    val availabilityEnd = baseTime.plusHours(3)
    val path =
      "/desks/$deskId/availability?startAt=$availabilityStart&endAt=$availabilityEnd"
    val availabilityResponse =
      client.toBlocking().exchange(
        HttpRequest
          .GET<Any>(path)
          .bearerAuth(authToken),
        Argument.listOf(AvailabilityResponse::class.java),
      )
    assertEquals(HttpStatus.OK, availabilityResponse.status)
    val availability = availabilityResponse.body()
    assertNotNull(availability)

    assertEquals(6, availability!!.size)

    val truncatedBookingStart = bookingStart.truncatedTo(ChronoUnit.MINUTES)
    val truncatedBookingEnd = bookingEnd.truncatedTo(ChronoUnit.MINUTES)

    val bookedSlot =
      availability.firstOrNull {
        it.startAt == truncatedBookingStart && it.endAt == truncatedBookingEnd
      }
    assertNotNull(bookedSlot)
    assertEquals(AvailabilityStatus.BOOKED, bookedSlot!!.status)

    val otherSlots = availability.filter { it != bookedSlot }
    assertEquals(5, otherSlots.size)
    assertTrue(otherSlots.all { it.status == AvailabilityStatus.AVAILABLE })
  }
}
