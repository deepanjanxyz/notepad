package com.deepanjanxyz.notepad.data

import com.deepanjanxyz.notepad.data.repository.AuthRepositoryImpl
import com.deepanjanxyz.notepad.domain.repository.AuthRepository

object AuthRepositoryProvider {
    fun create(supabaseUrl: String, supabaseAnonKey: String): AuthRepository {
        return AuthRepositoryImpl(
            supabaseClient = SupabaseClientProvider.create(supabaseUrl, supabaseAnonKey)
        )
    }
}
