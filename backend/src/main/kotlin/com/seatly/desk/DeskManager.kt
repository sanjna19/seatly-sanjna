package com.seatly.desk

import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.HttpStatus


@Singleton
class DeskManager(
  private val deskRepository: DeskRepository,
  private val bookingRepository: BookingRepository,
) {
  fun createDesk(command: CreateDeskCommand): DeskDto {
    val desk =
      Desk(
        name = command.name,
        location = command.location,
      )

    val savedDesk = deskRepository.save(desk)
    return DeskDto.from(savedDesk)
  }

  fun listDesks(): List<DeskDto> = deskRepository.findAll().map { DeskDto.from(it) }

  fun listDeskAvailability(
    deskId: Long,
    startAt: LocalDateTime,
    endAt: LocalDateTime,
  ): List<AvailabilityDto> {
    require(!endAt.isBefore(startAt)) {
      "endAt must not be before startAt"
    }

    val normalizedRequestedStart = startAt.truncatedTo(ChronoUnit.MINUTES)
    val normalizedRequestedEnd = endAt.truncatedTo(ChronoUnit.MINUTES)

    val windowStart = normalizedRequestedStart.roundDownToHalfHour()
    val windowEnd = normalizedRequestedEnd.roundUpToHalfHour()

    if (!windowStart.isBefore(windowEnd)) {
      return emptyList()
    }

    val bookings =
      bookingRepository.findOverlappingBookings(
        deskId = deskId,
        startAt = windowStart,
        endAt = windowEnd,
      )

    val slots = mutableListOf<AvailabilityDto>()
    var slotStart = windowStart
    val slotMinutes = 30L

    while (slotStart.isBefore(windowEnd)) {
      val slotEnd = slotStart.plusMinutes(slotMinutes)

      val isBooked =
        bookings.any { booking ->
          booking.startAt.isBefore(slotEnd) && booking.endAt.isAfter(slotStart)
        }

      val status =
        if (isBooked) AvailabilityStatus.BOOKED else AvailabilityStatus.AVAILABLE

      slots.add(
        AvailabilityDto(
          startAt = slotStart,
          endAt = slotEnd,
          status = status,
        ),
      )

      slotStart = slotEnd
    }

    return slots
  }

  fun createBooking(command: CreateBookingCommand): BookingDto {
    require(command.startAt.isBefore(command.endAt)) {
      "startAt must be before endAt"
    }

    val normalizedStart = command.startAt.truncatedTo(ChronoUnit.MINUTES)
    val normalizedEnd = command.endAt.truncatedTo(ChronoUnit.MINUTES)

    if (!command.recurring) {

      if (bookingRepository.existsOverlappingBooking(command.deskId, normalizedStart, normalizedEnd)) {
        throw HttpStatusException(
          HttpStatus.BAD_REQUEST,
          "Desk is already booked for the given time range"
        )
      }

      val booking =
        Booking(
          deskId = command.deskId,
          userId = command.userId,
          startAt = normalizedStart,
          endAt = normalizedEnd,
        )

      val savedBooking = bookingRepository.save(booking)
      return BookingDto.from(savedBooking)
    }
    // Generate all weekly occurrences
    val occurrences = (0 until command.weeks).map { i ->
      val newStart = normalizedStart.plusWeeks(i.toLong())
      val newEnd = normalizedEnd.plusWeeks(i.toLong())
      newStart to newEnd
    }

    // Validate ALL occurrences before saving anything
    occurrences.forEach { (start, end) ->
      if (bookingRepository.existsOverlappingBooking(
          command.deskId,
          start,
          end
        )
      ) {
        throw HttpStatusException(
          HttpStatus.BAD_REQUEST,
          "Recurring booking conflicts with an existing booking on: $start"
        )
      }
    }

    // Save ALL occurrences (atomic behavior)
    val savedBookings = occurrences.map { (start, end) ->
      val booking = Booking(
        deskId = command.deskId,
        userId = command.userId,
        startAt = start,
        endAt = end,
      )
      bookingRepository.save(booking)
    }

    // Return the FIRST booking (or we can return whole series later)
    return BookingDto.from(savedBookings.first())
  }
}

private fun LocalDateTime.roundDownToHalfHour(): LocalDateTime {
  val minute = if (this.minute < 30) 0 else 30
  return this
    .withMinute(minute)
    .withSecond(0)
    .withNano(0)
}

private fun LocalDateTime.roundUpToHalfHour(): LocalDateTime {
  val needsIncrement = this.minute % 30 != 0 || this.second != 0 || this.nano != 0
  val base =
    if (needsIncrement) this.plusMinutes(30 - (this.minute % 30).toLong()) else this
  val minute = if (base.minute < 30) 0 else 30
  return base
    .withMinute(minute)
    .withSecond(0)
    .withNano(0)
}

data class CreateDeskCommand(
  val name: String,
  val location: String?,
)

data class DeskDto(
  val id: Long,
  val name: String,
  val location: String?,
) {
  companion object {
    fun from(desk: Desk): DeskDto =
      DeskDto(
        id = desk.id!!,
        name = desk.name,
        location = desk.location,
      )
  }
}

data class AvailabilityDto(
  val startAt: LocalDateTime,
  val endAt: LocalDateTime,
  val status: AvailabilityStatus,
)

data class CreateBookingCommand(
  val deskId: Long,
  val userId: Long,
  val startAt: LocalDateTime,
  val endAt: LocalDateTime,
  val recurring: Boolean,
  val weeks: Int,
)

data class BookingDto(
  val id: Long,
  val deskId: Long,
  val userId: Long,
  val startAt: LocalDateTime,
  val endAt: LocalDateTime,
) {
  companion object {
    fun from(booking: Booking): BookingDto =
      BookingDto(
        id = booking.id!!,
        deskId = booking.deskId,
        userId = booking.userId,
        startAt = booking.startAt,
        endAt = booking.endAt,
      )
  }
}
