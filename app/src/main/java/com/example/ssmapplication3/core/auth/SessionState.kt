package com.example.ssmapplication3.core.auth

import com.example.ssmapplication3.domain.model.UserProfile

sealed class SessionState {
    object Initializing : SessionState()
    object Unauthenticated : SessionState()
    object Authenticating : SessionState()
    object AwaitingGoogle : SessionState()
    data class Authenticated(
        val userProfile: UserProfile,
        val capabilities: Set<String>
    ) : SessionState()
    object SessionExpired : SessionState()
    data class AccessDenied(val reason: String) : SessionState()
    object Suspended : SessionState()
    data class Error(val message: String) : SessionState()
}
