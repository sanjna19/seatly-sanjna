package com.seatly.desk

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Repository
interface DeskRepository : JpaRepository<Desk, Long>

@Entity
@Table(name = "desk")
data class Desk(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val name: String = "",
    @Column
    val location: String? = null,
)
