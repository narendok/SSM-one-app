package com.example.ssmapplication3.data.repository

import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.util.suspendResult
import com.example.ssmapplication3.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.Google
import com.example.ssmapplication3.core.auth.GoogleOAuthCallback

interface AuthGateway {
    suspend fun signIn(email: String, password: String)
    suspend fun beginGoogleSignIn()
    suspend fun exchangeGoogleCode(code: String)
    suspend fun restore(): Boolean
    suspend fun signOut()
    suspend fun clear()
    fun userId(): String?
}

class SupabaseAuthGateway : AuthGateway {
    private val auth get() = SupabaseClientProvider.auth

    override suspend fun beginGoogleSignIn() {
        auth.awaitInitialization()
        auth.signInWith(Google, redirectUrl = GoogleOAuthCallback.REDIRECT_URL) {
            queryParams["prompt"] = "select_account"
        }
    }

    override suspend fun exchangeGoogleCode(code: String) {
        auth.awaitInitialization()
        check(!auth.codeVerifierCache.loadCodeVerifier().isNullOrBlank()) { "Sign-in request expired. Please try again." }
        auth.exchangeCodeForSession(code)
        check(!auth.currentSessionOrNull()?.accessToken.isNullOrBlank()) { "No authenticated session returned." }
    }

    override suspend fun signIn(email: String, password: String) {
        auth.awaitInitialization()
        auth.signInWith(Email) { this.email = email; this.password = password }
        check(!auth.currentSessionOrNull()?.accessToken.isNullOrBlank()) { "No authenticated session returned." }
    }

    override suspend fun restore(): Boolean {
        auth.awaitInitialization()
        // Validate with the server before accepting a stored identity.
        val stored = auth.sessionManager.loadSession() ?: return false
        if (stored.refreshToken.isBlank() || stored.refreshToken.startsWith("demo_")) {
            auth.clearSession()
            return false
        }
        auth.importSession(auth.refreshSession(stored.refreshToken))
        return !auth.currentUserOrNull()?.id.isNullOrBlank()
    }

    override suspend fun signOut() { auth.signOut() }
    override suspend fun clear() { auth.clearSession() }
    override fun userId(): String? = auth.currentUserOrNull()?.id
}

class AuthRepositoryImpl(private val gateway: AuthGateway = SupabaseAuthGateway()) : AuthRepository {
    override suspend fun beginGoogleSignIn(): Result<Unit> = suspendResult {
        gateway.clear()
        try { gateway.beginGoogleSignIn() } catch (error: Exception) {
            gateway.clear()
            throw error
        }
    }

    override suspend fun completeGoogleSignIn(callbackUrl: String): Result<Unit> = suspendResult {
        try {
            gateway.exchangeGoogleCode(GoogleOAuthCallback.authorizationCode(callbackUrl))
            check(!gateway.userId().isNullOrBlank()) { "User identity missing after sign-in." }
        } catch (error: Exception) {
            gateway.clear()
            throw error
        }
    }

    override suspend fun cancelGoogleSignIn(): Result<Unit> = suspendResult { gateway.clear() }

    override suspend fun login(email: String, password: String): Result<Unit> = suspendResult {
        gateway.clear()
        try {
            gateway.signIn(email, password)
            check(!gateway.userId().isNullOrBlank()) { "User identity missing after sign-in." }
        } catch (error: Exception) {
            gateway.clear()
            throw error
        }
    }

    override suspend fun restoreSession(): Result<Boolean> = suspendResult {
        try {
            val restored = gateway.restore()
            if (!restored || gateway.userId().isNullOrBlank()) {
                gateway.clear()
                false
            } else true
        } catch (error: Exception) {
            gateway.clear()
            throw error
        }
    }

    override suspend fun logout(): Result<Unit> = suspendResult {
        try { gateway.signOut() } finally { gateway.clear() }
    }

    override fun getCurrentUserId(): String? = gateway.userId()
}
