package com.deepanjanxyz.notepad.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
}
