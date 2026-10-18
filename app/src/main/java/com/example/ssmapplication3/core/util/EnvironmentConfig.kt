package com.example.ssmapplication3.core.util

import com.example.ssmapplication3.BuildConfig

enum class Environment(val displayName: String, val showBadge: Boolean) {
    SHARED("SHARED BACKEND", true),
    DEV("DEV", true),
    UAT("UAT / STAGING", true),
    PROD("PRODUCTION", false)
}

object EnvironmentConfig {
    val currentEnvironment: Environment by lazy {
        when (BuildConfig.ENVIRONMENT_NAME.uppercase()) {
            "SHARED" -> Environment.SHARED
            "PROD", "PRODUCTION" -> Environment.PROD
            "UAT", "STAGING" -> Environment.UAT
            else -> Environment.DEV
        }
    }

    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL

    val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY

    fun requireBackendConfiguration() {
        check(supabaseUrl.startsWith("https://") && !supabaseUrl.contains("your-supabase-project")) {
            "Backend URL is not configured."
        }
        check(supabaseAnonKey.isNotBlank() && !supabaseAnonKey.contains("placeholder")) {
            "Backend client key is not configured."
        }
    }
}
