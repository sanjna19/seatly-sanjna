
package com.seatly.booking.desk

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@Controller("/desks")
open class DeskController(
    private val deskManager: DeskManager,
) {
    @Post
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun createDesk(
        @Body @Valid request: CreateDeskRequest,
    ): HttpResponse<DeskResponse> {
        val created = deskManager.createDesk(request.toCommand())
        val responseBody = DeskResponse.from(created)
        return HttpResponse.status<DeskResponse>(HttpStatus.CREATED).body(responseBody)
    }

    @Get
    @Secured(SecurityRule.IS_AUTHENTICATED)
    open fun listDesks(): HttpResponse<List<DeskResponse>> {
        val desks = deskManager.listDesks()
        val responseBody = desks.map { DeskResponse.from(it) }
        return HttpResponse.ok(responseBody)
    }
}

@Serdeable
data class CreateDeskRequest(
    @field:NotBlank
    val name: String,
    val location: String? = null,
) {
    fun toCommand(): CreateDeskCommand =
        CreateDeskCommand(
            name = name,
            location = location,
        )
}

@Serdeable
data class DeskResponse(
    val id: Long?,
    val name: String,
    val location: String?,
) {
    companion object {
        fun from(desk: DeskDto): DeskResponse =
            DeskResponse(
                id = desk.id,
                name = desk.name,
                location = desk.location,
            )
    }
}
