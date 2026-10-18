package com.example.ssmapplication3.core.auth

import com.example.ssmapplication3.core.security.CapabilitiesManager
import com.example.ssmapplication3.domain.repository.AuthRepository
import com.example.ssmapplication3.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionManager(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    private val lock = Mutex()
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Initializing)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    suspend fun initializeSession() = lock.withLock {
        _sessionState.value = SessionState.Initializing
        val restored = authRepository.restoreSession()
        when {
            restored.isFailure -> _sessionState.value = SessionState.Error("Session could not be restored. Please sign in again.")
            restored.getOrDefault(false) -> resolveIdentity()
            else -> _sessionState.value = SessionState.Unauthenticated
        }
    }

    suspend fun login(email: String, pass: String): Result<Unit> = lock.withLock {
        _sessionState.value = SessionState.Authenticating
        val result = authRepository.login(email, pass)
        if (result.isFailure) {
            _sessionState.value = SessionState.Error("Sign-in failed. Check your credentials and connection.")
            result
        } else resolveIdentity()
    }

    suspend fun beginGoogleSignIn(): Result<Unit> = lock.withLock {
        if (_sessionState.value is SessionState.Authenticated || _sessionState.value is SessionState.AwaitingGoogle) {
            return@withLock Result.success(Unit)
        }
        val result = authRepository.beginGoogleSignIn()
        _sessionState.value = if (result.isSuccess) SessionState.AwaitingGoogle
            else SessionState.Error("Could not open Google sign-in. Please try again.")
        result
    }

    suspend fun completeGoogleSignIn(callbackUrl: String): Result<Unit> = lock.withLock {
        // A replayed or unsolicited link must not replace an already verified identity.
        if (_sessionState.value is SessionState.Authenticated) return@withLock Result.success(Unit)
        _sessionState.value = SessionState.Authenticating
        val result = authRepository.completeGoogleSignIn(callbackUrl)
        if (result.isFailure) {
            _sessionState.value = SessionState.Error("Google sign-in expired or was not completed. Please try again.")
            result
        } else resolveIdentity()
    }

    /** Reloads the same identity, roles and permissions the web client reads from Supabase. */
    suspend fun refreshIdentity(): Result<Unit> = lock.withLock {
        if (authRepository.getCurrentUserId() == null) {
            _sessionState.value = SessionState.Unauthenticated
            return@withLock Result.failure(IllegalStateException("No active session."))
        }
        _sessionState.value = SessionState.Authenticating
        resolveIdentity()
    }

    suspend fun cancelGoogleSignIn() = lock.withLock {
        if (_sessionState.value is SessionState.AwaitingGoogle) {
            authRepository.cancelGoogleSignIn()
            _sessionState.value = SessionState.Unauthenticated
        }
    }

    private suspend fun resolveIdentity(): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
        val profile = userId?.let { profileRepository.fetchUserProfile(it).getOrNull() }
        val message = when {
            profile == null || profile.id != userId -> "Unable to verify your employee profile."
            profile.isSuspended -> "Your employee account is suspended."
            !CapabilitiesManager.isInternalEmployee(profile) -> "An active linked employee account is required."
            else -> null
        }
        if (message != null) {
            authRepository.logout()
            _sessionState.value = SessionState.AccessDenied(message)
            return Result.failure(IllegalStateException(message))
        }
        _sessionState.value = SessionState.Authenticated(requireNotNull(profile), profile.capabilities.toSet())
        return Result.success(Unit)
    }

    suspend fun logout() = lock.withLock {
        try { authRepository.logout() } finally { _sessionState.value = SessionState.Unauthenticated }
    }
}
