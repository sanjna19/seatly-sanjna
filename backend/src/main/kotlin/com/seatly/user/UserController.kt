package com.seatly.user

import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.security.token.generator.TokenGenerator
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.Optional

@Controller("/users")
open class UserController(
    private val userManager: UserManager,
    private val tokenGenerator: TokenGenerator,
) {
    @Post
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun createUser(
        @Body @Valid request: CreateUserRequest,
    ): HttpResponse<UserResponse> {
        val created = userManager.createUser(request.toCommand())
        val responseBody = UserResponse.from(created)
        return HttpResponse.status<UserResponse>(HttpStatus.CREATED).body(responseBody)
    }

    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    open fun login(
        @Body @Valid request: LoginRequest,
    ): HttpResponse<LoginResponse> {
        val user = userManager.authenticate(request.email, request.password)

        val claims: Map<String, Any> =
            mapOf(
                "sub" to user.id.toString(),
                "email" to user.email,
                "fullName" to (user.fullName ?: ""),
            )

        val token: String =
            (tokenGenerator.generateToken(claims) as Optional<String>)
                .orElseThrow { IllegalStateException("Failed to generate JWT token") }

        val body = LoginResponse.from(user, token)
        return HttpResponse.ok(body)
    }

    @Error(global = true)
    fun handleEmailAlreadyInUse(e: EmailAlreadyInUseException): HttpResponse<Map<String, String>> =
        HttpResponse
            .status<Map<String, String>>(HttpStatus.CONFLICT)
            .body(mapOf("message" to e.message.orEmpty()))

    @Error(global = true)
    fun handleInvalidCredentials(e: InvalidCredentialsException): HttpResponse<Map<String, String>> =
        HttpResponse
            .status<Map<String, String>>(HttpStatus.UNAUTHORIZED)
            .body(mapOf("message" to e.message.orEmpty()))
}

@Serdeable
data class CreateUserRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
    val fullName: String? = null,
) {
    fun toCommand(): CreateUserCommand =
        CreateUserCommand(
            email = email,
            password = password,
            fullName = fullName,
        )
}

@Serdeable
data class UserResponse(
    val id: Long?,
    val email: String,
    val fullName: String?,
) {
    companion object {
        fun from(user: UserDto): UserResponse =
            UserResponse(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
            )
    }
}

@Serdeable
data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
)

@Serdeable
data class LoginResponse(
    val token: String,
    val id: Long?,
    val email: String,
    val fullName: String?,
) {
    companion object {
        fun from(
            user: UserDto,
            token: String,
        ): LoginResponse =
            LoginResponse(
                token = token,
                id = user.id,
                email = user.email,
                fullName = user.fullName,
            )
    }
}
