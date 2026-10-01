package com.deepanjanxyz.notepad.data.setup

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.deepanjanxyz.notepad.data.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import java.net.HttpURLConnection
import java.net.URL

class SetupRepository(private val store: EncryptedSetupStore) {
    val state: Flow<SetupState> = store.state

    suspend fun saveOffline(): SetupResult {
        val config = BackendConfig(BackendType.OFFLINE)
        store.save(config)
        return SetupResult.Success(config)
    }

    suspend fun verify(config: BackendConfig): SetupResult = withContext(Dispatchers.IO) {
        val url = config.serverUrl?.trim().orEmpty()
        val parsed = Uri.parse(url)
        if (parsed.scheme !in setOf("http", "https") || parsed.host.isNullOrBlank()) {
            return@withContext SetupResult.Failure("Invalid URL. Use an http:// or https:// address.")
        }
        if (config.type == BackendType.SUPABASE && config.apiKey.isNullOrBlank()) {
            return@withContext SetupResult.Failure("API key is required for Supabase.")
        }
        if (config.type != BackendType.SUPABASE && config.passphrase.orEmpty().length < 12) {
            return@withContext SetupResult.Failure("Passphrase must contain at least 12 characters.")
        }
        val endpoint = if (config.type == BackendType.SUPABASE) {
            url.trimEnd('/') + "/rest/v1/"
        } else url
        val connection = try {
            (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 7_000
                readTimeout = 7_000
                config.apiKey?.let {
                    setRequestProperty("apikey", it)
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            return@withContext SetupResult.Failure("Invalid server URL: ${error.message ?: "unable to parse URL"}")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) return@withContext SetupResult.Failure("Server responded with HTTP $code.")
            SetupResult.Success(config)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            SetupResult.Failure("Server unreachable: ${error.message ?: "connection failed"}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun save(config: BackendConfig) {
        store.save(config)
    }

    suspend fun requestEmailOtp(config: BackendConfig, email: String) {
        SupabaseClientProvider.configure(
            config.serverUrl ?: error("Supabase URL is missing"),
            config.apiKey ?: error("Supabase API key is missing")
        )
        val client = SupabaseClientProvider.current()
            ?: error("Supabase client could not be initialized")
        client.auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    suspend fun verifyEmailOtp(email: String, code: String) {
        val client = SupabaseClientProvider.current() ?: error("Supabase client is not configured")
        client.auth.verifyEmailOtp(OtpType.Email.EMAIL, email, code)
    }
}
