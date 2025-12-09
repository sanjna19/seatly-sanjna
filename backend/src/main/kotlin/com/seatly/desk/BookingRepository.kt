package com.seatly.desk

import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Repository
interface BookingRepository : JpaRepository<Booking, Long> {
  @Query(
    """
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b
        WHERE b.deskId = :deskId
          AND b.startAt < :endAt
          AND b.endAt > :startAt
        """,
  )
  fun existsOverlappingBooking(
    deskId: Long,
    startAt: LocalDateTime,
    endAt: LocalDateTime,
  ): Boolean

  @Query(
    """
        SELECT b
        FROM Booking b
        WHERE b.deskId = :deskId
          AND b.startAt < :endAt
          AND b.endAt > :startAt
        """,
  )
  fun findOverlappingBookings(
    deskId: Long,
    startAt: LocalDateTime,
    endAt: LocalDateTime,
  ): List<Booking>
}

@Entity
@Table(name = "booking")
data class Booking(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "desk_id", nullable = false)
  val deskId: Long = -1,
  @Column(name = "user_id", nullable = false)
  val userId: Long = -1,
  @Column(name = "start_at", nullable = false)
  val startAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "end_at", nullable = false)
  val endAt: LocalDateTime = LocalDateTime.now(),
)
