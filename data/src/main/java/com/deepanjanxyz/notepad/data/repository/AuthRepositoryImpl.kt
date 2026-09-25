package com.deepanjanxyz.notepad.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import com.deepanjanxyz.notepad.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<Unit> = execute {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> = execute {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun logout(): Result<Unit> = execute {
        supabaseClient.auth.signOut()
    }

    private suspend fun execute(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
