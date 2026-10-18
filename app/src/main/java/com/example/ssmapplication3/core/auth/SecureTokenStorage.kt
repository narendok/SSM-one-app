package com.example.ssmapplication3.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One encrypted store, including SDK refresh-token rotation. No plaintext fallback. */
class SecureTokenStorage(context: Context) : io.github.jan.supabase.gotrue.SessionManager,
    io.github.jan.supabase.gotrue.CodeVerifierCache {
    private val appContext = context.applicationContext
    private val preferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            appContext, "ssm_one_secure_tokens", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun saveSession(session: UserSession) {
        check(preferences.edit().putString("session", Json.encodeToString(session))
            .remove("access_token").remove("refresh_token").commit()) { "Unable to save secure session." }
    }

    override suspend fun loadSession(): UserSession? =
        preferences.getString("session", null)?.let { Json.decodeFromString<UserSession>(it) }

    override suspend fun deleteSession() {
        check(preferences.edit().clear().commit()) { "Unable to clear secure session." }
    }

    override suspend fun saveCodeVerifier(codeVerifier: String) {
        check(preferences.edit().putString("oauth_verifier", codeVerifier)
            .putLong("oauth_started_at", System.currentTimeMillis()).commit()) { "Unable to save sign-in request." }
    }

    override suspend fun loadCodeVerifier(): String? {
        if (!GoogleOAuthCallback.isFresh(preferences.getLong("oauth_started_at", 0), System.currentTimeMillis())) {
            deleteCodeVerifier()
            return null
        }
        return preferences.getString("oauth_verifier", null)
    }

    override suspend fun deleteCodeVerifier() {
        check(preferences.edit().remove("oauth_verifier").remove("oauth_started_at").commit()) {
            "Unable to clear sign-in request."
        }
    }
}
