package com.example.ssmapplication3.core.network

import android.content.Context
import com.example.ssmapplication3.core.auth.SecureTokenStorage
import com.example.ssmapplication3.core.util.EnvironmentConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import com.example.ssmapplication3.core.auth.GoogleOAuthCallback
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseClientProvider {
    private lateinit var secureSessionStorage: SecureTokenStorage

    fun initialize(context: Context) {
        if (!::secureSessionStorage.isInitialized) secureSessionStorage = SecureTokenStorage(context)
    }

    val client: SupabaseClient by lazy {
        check(::secureSessionStorage.isInitialized) { "Backend client has not been initialized." }
        EnvironmentConfig.requireBackendConfiguration()
        createSupabaseClient(
            supabaseUrl = EnvironmentConfig.supabaseUrl,
            supabaseKey = EnvironmentConfig.supabaseAnonKey
        ) {
            install(Auth) {
                sessionManager = secureSessionStorage
                codeVerifierCache = secureSessionStorage
                flowType = FlowType.PKCE
                defaultRedirectUrl = GoogleOAuthCallback.REDIRECT_URL
                autoLoadFromStorage = false
            }
            install(Postgrest)
            install(Storage)
        }
    }

    val auth: Auth
        get() = client.auth

    val postgrest: Postgrest
        get() = client.postgrest

    val storage: Storage
        get() = client.storage
}
