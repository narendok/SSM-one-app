package com.example.ssmapplication3.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun beginGoogleSignIn(): Result<Unit>
    suspend fun completeGoogleSignIn(callbackUrl: String): Result<Unit>
    suspend fun cancelGoogleSignIn(): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun restoreSession(): Result<Boolean>
    fun getCurrentUserId(): String?
}
