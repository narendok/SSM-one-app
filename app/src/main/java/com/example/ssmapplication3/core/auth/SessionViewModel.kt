package com.example.ssmapplication3.core.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.data.repository.AuthRepositoryImpl
import com.example.ssmapplication3.data.repository.ProfileRepositoryImpl
import kotlinx.coroutines.launch

/** Retains the session controller across activity recreation while the browser is open. */
class SessionViewModel(application: Application) : AndroidViewModel(application) {
    init { SupabaseClientProvider.initialize(application) }
    val sessionManager = SessionManager(AuthRepositoryImpl(), ProfileRepositoryImpl())
    private var started = false

    fun start(callbackUrl: String?) {
        if (started) {
            if (callbackUrl != null) completeGoogleSignIn(callbackUrl)
            return
        }
        started = true
        viewModelScope.launch {
            // Do not run normal restore first: it would discard the pending PKCE verifier.
            if (callbackUrl != null) sessionManager.completeGoogleSignIn(callbackUrl)
            else sessionManager.initializeSession()
        }
    }

    fun completeGoogleSignIn(url: String) {
        viewModelScope.launch { sessionManager.completeGoogleSignIn(url) }
    }
}
