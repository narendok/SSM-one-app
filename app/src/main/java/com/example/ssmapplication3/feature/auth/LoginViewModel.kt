package com.example.ssmapplication3.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.auth.SessionManager
import com.example.ssmapplication3.core.auth.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val sessionState: StateFlow<SessionState> = sessionManager.sessionState

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    fun login() {
        if (_isLoading.value) return
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            _errorMessage.value = "Please enter both email and password."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = sessionManager.login(currentEmail, currentPassword)
                if (result.isFailure) {
                    _errorMessage.value = "Sign-in was not completed. Check your account and connection."
                }
            } finally {
                _isLoading.value = false
                _password.value = ""
            }
        }
    }

    fun signInWithGoogle() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _password.value = ""
            try { sessionManager.beginGoogleSignIn() }
            finally { _isLoading.value = false }
        }
    }

    fun cancelGoogleSignIn() {
        viewModelScope.launch { sessionManager.cancelGoogleSignIn() }
    }
}
