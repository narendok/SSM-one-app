package com.example.ssmapplication3

import android.os.Bundle
import android.content.Intent
import androidx.activity.viewModels
import com.example.ssmapplication3.core.auth.GoogleOAuthCallback
import com.example.ssmapplication3.core.auth.SessionViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.sync.SyncCoordinator
import com.example.ssmapplication3.core.auth.SessionManager
import com.example.ssmapplication3.data.repository.AuthRepositoryImpl
import com.example.ssmapplication3.data.repository.ProfileRepositoryImpl
import com.example.ssmapplication3.navigation.AppNavGraph
import com.example.ssmapplication3.ui.theme.SSMApplication3Theme

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    private fun consumeCallback(intent: Intent?): String? {
        val url = intent?.dataString ?: return null
        intent.data = null
        return url.takeIf { intent.action == Intent.ACTION_VIEW && GoogleOAuthCallback.isCallback(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeCallback(intent)?.let(sessionViewModel::completeGoogleSignIn)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseClientProvider.initialize(applicationContext)
        SyncCoordinator.initialize(applicationContext)
        enableEdgeToEdge()
        sessionViewModel.start(consumeCallback(intent))

        setContent {
            SSMApplication3Theme {
                val sessionManager = sessionViewModel.sessionManager

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(sessionManager = sessionManager)
                }
            }
        }
    }
}
