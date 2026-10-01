package com.deepanjanxyz.notepad.data.setup

enum class BackendType { OFFLINE, SUPABASE, POCKETBASE, CUSTOM }

data class BackendConfig(
    val type: BackendType,
    val serverUrl: String? = null,
    val apiKey: String? = null,
    val passphrase: String? = null
)

data class SetupState(val completed: Boolean, val config: BackendConfig?)

sealed interface SetupResult {
    data class Success(val config: BackendConfig) : SetupResult
    data class Failure(val message: String) : SetupResult
}
