
package com.seatly.booking.desk

import jakarta.inject.Singleton

@Singleton
class DeskManager(
    private val deskRepository: DeskRepository,
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
