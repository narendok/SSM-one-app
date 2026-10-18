package com.example.ssmapplication3.core.ui

/**
 * Explicit success/failure result for repository calls that need to expose a
 * recoverable backend error to a ViewModel.
 */
sealed interface ResourceResult<out T> {
    data class Success<T>(val value: T) : ResourceResult<T>

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : ResourceResult<Nothing>
}
