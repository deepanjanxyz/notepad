package com.deepanjanxyz.notepad.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.MemorySessionManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {
    @Volatile
    private var configuredClient: SupabaseClient? = null
    private var configuredEndpoint: String? = null
    private var configuredKey: String? = null

    @Synchronized
    fun configure(url: String, anonKey: String) {
        if (configuredEndpoint == url && configuredKey == anonKey && configuredClient != null) return
        configuredClient?.auth?.stopAutoRefreshForCurrentSession()
        configuredClient = create(url, anonKey)
        configuredEndpoint = url
        configuredKey = anonKey
    }

    fun current(): SupabaseClient? = configuredClient

    @Synchronized
    fun clear() {
        configuredClient?.auth?.stopAutoRefreshForCurrentSession()
        configuredClient = null
        configuredEndpoint = null
        configuredKey = null
    }

    fun create(url: String, anonKey: String): SupabaseClient = createSupabaseClient(
        supabaseUrl = url,
        supabaseKey = anonKey
    ) {
        install(Auth) {
            sessionManager = MemorySessionManager()
            autoLoadFromStorage = false
            autoSaveToStorage = false
        }
        install(Postgrest)
    }
}
