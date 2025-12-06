package com.seatly.user

import jakarta.inject.Singleton
import org.mindrot.jbcrypt.BCrypt

@Singleton
class PasswordService {
    fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))

    fun verify(
        password: String,
        hashedPassword: String,
    ): Boolean = BCrypt.checkpw(password, hashedPassword)
}
