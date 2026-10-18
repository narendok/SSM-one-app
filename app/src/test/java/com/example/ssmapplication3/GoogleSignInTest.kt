package com.example.ssmapplication3

import com.example.ssmapplication3.core.auth.*
import com.example.ssmapplication3.data.repository.*
import com.example.ssmapplication3.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GoogleSignInTest {
    private val callback = GoogleOAuthCallback.REDIRECT_URL + "?code=test-code"
    private class Gateway : AuthGateway {
        var id: String? = null
        var pending = false
        var launches = 0
        var exchanges = 0
        var failLaunch = false
        var failExchange = false
        override suspend fun signIn(email: String, password: String) { id = "u1" }
        override suspend fun beginGoogleSignIn() {
            if (failLaunch) error("No browser")
            launches++; pending = true
        }
        override suspend fun exchangeGoogleCode(code: String) {
            check(pending && !failExchange)
            exchanges++; pending = false; id = "u1"
        }
        override suspend fun restore() = false
        override suspend fun signOut() = Unit
        override suspend fun clear() { id = null; pending = false }
        override fun userId() = id
    }
    private class Identity : IdentityDataSource {
        var status = "ACTIVE"
        var missing = false
        override suspend fun profile(userId: String) = ProfileRow("u1", "person@example.test", "Person")
        override suspend fun employee(userId: String): EmployeeRecord? =
            if (missing) null else EmployeeRecord("e1", "u1", status = status)
        override suspend fun roles(userId: String) = listOf(RoleRow("admin"))
        override suspend fun permissions(employeeId: String) = emptyList<EmployeeAccessRow>()
    }
    @Test fun validCodeIsDecoded() {
        assertEquals("test-code", GoogleOAuthCallback.authorizationCode(callback))
    }
    @Test fun malformedAndTokenCallbacksAreRejected() {
        val base = GoogleOAuthCallback.REDIRECT_URL
        for (url in listOf("https://auth/callback?code=x", "com.ssmone.mobile://evil/callback?code=x",
            "com.ssmone.mobile://user@auth/callback?code=x", "com.ssmone.mobile://auth:80/callback?code=x",
            "$base/extra?code=x", "$base?code=", "$base?code=x&code=y", "$base?code=x&%63ode=y",
            "$base?code=x&error=denied", "$base#access_token=x", "$base?access_token=x&code=y",
            "$base?code=%0A", "$base?code=%ZZ")) {
            assertTrue("Accepted invalid callback", runCatching { GoogleOAuthCallback.authorizationCode(url) }.isFailure)
        }
    }
    @Test fun pendingRequestsExpireAndRejectClockRollback() {
        assertTrue(GoogleOAuthCallback.isFresh(1000, 1001))
        assertFalse(GoogleOAuthCallback.isFresh(0, 1001))
        assertFalse(GoogleOAuthCallback.isFresh(1000, 999))
        assertFalse(GoogleOAuthCallback.isFresh(1000, 1001 + GoogleOAuthCallback.MAX_AGE_MS))
    }
    @Test fun openingBrowserDoesNotAuthenticateAndDuplicateStartIsIgnored() = runBlocking {
        val gateway = Gateway()
        val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(Identity()))
        manager.beginGoogleSignIn(); manager.beginGoogleSignIn()
        assertEquals(1, gateway.launches)
        assertNull(gateway.id)
        assertEquals(SessionState.AwaitingGoogle, manager.sessionState.value)
    }
    @Test fun successfulCallbackChecksEmployeeAndPreservesServerRoles() = runBlocking {
        val gateway = Gateway()
        val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(Identity()))
        manager.beginGoogleSignIn()
        assertTrue(manager.completeGoogleSignIn(callback).isSuccess)
        val state = manager.sessionState.value as SessionState.Authenticated
        assertEquals("u1", state.userProfile.id)
        assertTrue(state.capabilities.isEmpty())
        manager.completeGoogleSignIn(callback)
        assertEquals(1, gateway.exchanges)
    }
    @Test fun processRestartCallbackCanUsePersistedPendingRequest() = runBlocking {
        val gateway = Gateway().apply { pending = true }
        val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(Identity()))
        assertTrue(manager.completeGoogleSignIn(callback).isSuccess)
    }
    @Test fun callbackWithoutPendingRequestFailsClosed() = runBlocking {
        val gateway = Gateway()
        val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(Identity()))
        assertTrue(manager.completeGoogleSignIn(callback).isFailure)
        assertTrue(manager.sessionState.value is SessionState.Error)
        assertNull(gateway.id)
    }
    @Test fun cancellationInvalidatesLateCallback() = runBlocking {
        val gateway = Gateway()
        val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(Identity()))
        manager.beginGoogleSignIn(); manager.cancelGoogleSignIn()
        assertEquals(SessionState.Unauthenticated, manager.sessionState.value)
        assertTrue(manager.completeGoogleSignIn(callback).isFailure)
        assertNull(gateway.id)
    }
    @Test fun missingAndInactiveEmployeesCannotEnterThroughGoogle() = runBlocking {
        for (status in listOf("MISSING", "INVITED", "SUSPENDED", "EXITED")) {
            val gateway = Gateway()
            val identity = Identity().apply { this.status = status; missing = status == "MISSING" }
            val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(identity))
            manager.beginGoogleSignIn()
            assertTrue(manager.completeGoogleSignIn(callback).isFailure)
            assertTrue(manager.sessionState.value is SessionState.AccessDenied)
            assertNull(gateway.id)
        }
    }
    @Test fun launchFailureRemainsRetryable() = runBlocking {
        val gateway = Gateway().apply { failLaunch = true }
        val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(Identity()))
        assertTrue(manager.beginGoogleSignIn().isFailure)
        assertTrue(manager.sessionState.value is SessionState.Error)
        gateway.failLaunch = false
        assertTrue(manager.beginGoogleSignIn().isSuccess)
    }
    @Test fun failedExchangeClearsPendingAndIdentity() = runBlocking {
        val gateway = Gateway().apply { pending = true; failExchange = true }
        assertTrue(AuthRepositoryImpl(gateway).completeGoogleSignIn(callback).isFailure)
        assertFalse(gateway.pending)
        assertNull(gateway.id)
    }
}
