
package com.seatly.user

import jakarta.inject.Singleton
import java.time.Instant

@Singleton
class UserManager(
    private val userRepository: UserRepository,
    private val passwordService: PasswordService,
) {
    fun createUser(command: CreateUserCommand): UserDto {
        if (userRepository.existsByEmail(command.email)) {
            throw EmailAlreadyInUseException(command.email)
        }

        val now = Instant.now()

        val user =
            User(
                email = command.email,
                passwordHash = passwordService.hash(command.password),
                fullName = command.fullName,
                createdAt = now,
                updatedAt = now,
            )

        val savedUser = userRepository.save(user)
        return UserDto.from(savedUser)
    }

    fun authenticate(
        email: String,
        password: String,
    ): UserDto {
        val user =
            userRepository.findByEmail(email)
                ?: throw InvalidCredentialsException()

        if (!passwordService.verify(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return UserDto.from(user)
    }
}

data class CreateUserCommand(
    val email: String,
    val password: String,
    val fullName: String?,
)

data class UserDto(
    val id: Long,
    val email: String,
    val fullName: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(user: User): UserDto =
            UserDto(
                id = user.id!!,
                email = user.email,
                fullName = user.fullName,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            )
    }
}

class EmailAlreadyInUseException(
    email: String,
) : RuntimeException("Email '$email' is already in use")

class InvalidCredentialsException : RuntimeException("Invalid email or password")
