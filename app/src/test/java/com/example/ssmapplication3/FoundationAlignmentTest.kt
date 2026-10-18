package com.example.ssmapplication3

import com.example.ssmapplication3.core.auth.SessionManager
import com.example.ssmapplication3.core.auth.SessionState
import com.example.ssmapplication3.core.security.CapabilitiesManager
import com.example.ssmapplication3.data.repository.*
import com.example.ssmapplication3.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class FoundationAlignmentTest {
    private class FakeAuth : AuthGateway {
        override suspend fun beginGoogleSignIn() = Unit
        override suspend fun exchangeGoogleCode(code: String) { id = "u1" }
        var id: String? = null
        var signInError: Exception? = null
        var restoreError: Exception? = null
        var logoutError: Exception? = null
        var restoreResult = false
        var clearCount = 0
        override suspend fun signIn(email: String, password: String) {
            signInError?.let { throw it }; id = "u1"
        }
        override suspend fun restore(): Boolean {
            restoreError?.let { throw it }
            if (restoreResult) id = "u1"
            return restoreResult
        }
        override suspend fun signOut() { logoutError?.let { throw it } }
        override suspend fun clear() { clearCount++; id = null }
        override fun userId() = id
    }
    private class FakeIdentity : IdentityDataSource {
        var record: EmployeeRecord? = EmployeeRecord("e1", "u1", status = "ACTIVE")
        var profileError: Exception? = null
        var permissionError: Exception? = null
        var profileId = "u1"
        override suspend fun profile(userId: String): ProfileRow {
            profileError?.let { throw it }; return ProfileRow(profileId, "employee@example.test", "Person")
        }
        override suspend fun employee(userId: String) = record
        override suspend fun roles(userId: String) = listOf(RoleRow("member"))
        override suspend fun permissions(employeeId: String): List<EmployeeAccessRow> {
            permissionError?.let { throw it }
            return listOf(EmployeeAccessRow(AccessRoleRow(listOf(
                RolePermissionRow(PermissionRow("inventory.view")),
                RolePermissionRow(PermissionRow("inventory.view"))
            ))))
        }
    }

    @Test fun rejectedLoginNeverCreatesDemoIdentity() = runBlocking {
        val gateway = FakeAuth().apply { signInError = IllegalStateException("Rejected") }
        val repository = AuthRepositoryImpl(gateway)
        assertTrue(repository.login("admin@ssmone.internal", "wrong").isFailure)
        assertNull(repository.getCurrentUserId())
    }
    @Test fun failedRestoreClearsPreviousIdentity() = runBlocking {
        val gateway = FakeAuth().apply { id = "old"; restoreError = IllegalStateException("Expired") }
        assertTrue(AuthRepositoryImpl(gateway).restoreSession().isFailure)
        assertNull(gateway.id)
    }
    @Test fun emptyRestoreDoesNotAuthenticate() = runBlocking {
        val gateway = FakeAuth().apply { id = "old" }
        assertFalse(AuthRepositoryImpl(gateway).restoreSession().getOrThrow())
        assertNull(gateway.id)
    }
    @Test fun successfulRestoreUsesServerIdentity() = runBlocking {
        val gateway = FakeAuth().apply { restoreResult = true }
        val repository = AuthRepositoryImpl(gateway)
        assertTrue(repository.restoreSession().getOrThrow())
        assertEquals("u1", repository.getCurrentUserId())
    }
    @Test fun remoteLogoutFailureStillClearsLocalIdentity() = runBlocking {
        val gateway = FakeAuth().apply { id = "u1"; logoutError = IllegalStateException("Offline") }
        assertTrue(AuthRepositoryImpl(gateway).logout().isFailure)
        assertNull(gateway.id)
    }
    @Test fun cancellationIsNotConvertedToLoginFailure() = runBlocking {
        val gateway = FakeAuth().apply { signInError = CancellationException("Cancelled") }
        try { AuthRepositoryImpl(gateway).login("user", "pass"); fail("Must cancel") }
        catch (_: CancellationException) { assertNull(gateway.id) }
    }
    @Test fun profileFailureNeverFabricatesAdmin() = runBlocking {
        val source = FakeIdentity().apply { profileError = IllegalStateException("Denied") }
        assertTrue(ProfileRepositoryImpl(source).fetchUserProfile("u1").isFailure)
    }
    @Test fun activeEmployeeResolvesOnlyReturnedPermissionKeys() = runBlocking {
        val profile = ProfileRepositoryImpl(FakeIdentity()).fetchUserProfile("u1").getOrThrow()
        assertTrue(CapabilitiesManager.isInternalEmployee(profile))
        assertEquals(listOf("inventory.view"), profile.capabilities)
        assertEquals(listOf("member"), profile.roles)
    }
    @Test fun permissionReadFailureFailsClosed() = runBlocking {
        val source = FakeIdentity().apply { permissionError = IllegalStateException("Denied") }
        assertTrue(ProfileRepositoryImpl(source).fetchUserProfile("u1").isFailure)
    }
    @Test fun mismatchedProfileIsRejected() = runBlocking {
        val source = FakeIdentity().apply { profileId = "other" }
        assertTrue(ProfileRepositoryImpl(source).fetchUserProfile("u1").isFailure)
    }
    @Test fun mismatchedEmployeeIsRejected() = runBlocking {
        val source = FakeIdentity().apply { record = EmployeeRecord("e1", "other", status = "ACTIVE") }
        assertTrue(ProfileRepositoryImpl(source).fetchUserProfile("u1").isFailure)
    }
    @Test fun onlyActiveLinkedEmployeesCanEnterApp() = runBlocking {
        for (status in listOf("INVITED", "SUSPENDED", "EXITED", "UNKNOWN")) {
            val source = FakeIdentity().apply { record = EmployeeRecord("e1", "u1", status = status) }
            val gateway = FakeAuth()
            val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(source))
            assertTrue(manager.login("user", "pass").isFailure)
            assertTrue(manager.sessionState.value is SessionState.AccessDenied)
            assertNull(gateway.id)
        }
    }
    @Test fun absentEmployeeIsDenied() = runBlocking {
        val source = FakeIdentity().apply { record = null }
        val gateway = FakeAuth()
        val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(source))
        assertTrue(manager.login("user", "pass").isFailure)
        assertNull(gateway.id)
    }
    @Test fun validEmployeeLoginAndLogoutChangeSessionState() = runBlocking {
        val manager = SessionManager(AuthRepositoryImpl(FakeAuth()), ProfileRepositoryImpl(FakeIdentity()))
        assertTrue(manager.login("user", "pass").isSuccess)
        assertTrue(manager.sessionState.value is SessionState.Authenticated)
        manager.logout()
        assertEquals(SessionState.Unauthenticated, manager.sessionState.value)
    }
    @Test fun restoredSessionStillChecksEmployeeStatus() = runBlocking {
        val gateway = FakeAuth().apply { restoreResult = true }
        val source = FakeIdentity().apply { record = EmployeeRecord("e1", "u1", status = "SUSPENDED") }
        val manager = SessionManager(AuthRepositoryImpl(gateway), ProfileRepositoryImpl(source))
        manager.initializeSession()
        assertTrue(manager.sessionState.value is SessionState.AccessDenied)
        assertNull(gateway.id)
    }
    @Test fun adminLabelDoesNotGrantArbitraryCapabilities() {
        val profile = UserProfile("u1", "admin@example.test", userType = "INTERNAL_EMPLOYEE", roles = listOf("ADMIN", "admin"))
        assertFalse(CapabilitiesManager.hasCapability(profile, "inventory.manage"))
    }
    @Test fun unknownIdentityDoesNotDefaultToEmployee() {
        assertFalse(CapabilitiesManager.isInternalEmployee(UserProfile("u1", "user@example.test")))
    }
    @Test fun backendEmployeeJsonUsesSnakeCaseAndAllowsNulls() {
        val row = Json.decodeFromString<EmployeeRecord>("""{"id":"e1","user_id":null,"employee_code":null,"employment_status":"INVITED","primary_department_id":"d1"}""")
        assertNull(row.userId)
        assertNull(row.employeeCode)
        assertEquals("INVITED", row.status)
        assertEquals("d1", row.departmentId)
    }
    @Test fun profileJsonMapsDisplayName() {
        val row = Json.decodeFromString<ProfileRow>("""{"id":"u1","email":"user@example.test","display_name":"Example","department":"hardware"}""")
        assertEquals("Example", row.displayName)
        assertEquals("hardware", row.department)
    }
    @Test fun webPermissionProjectionDecodesNullableRelations() {
        val rows = Json.decodeFromString<List<EmployeeAccessRow>>("""[{"access_roles":{"access_role_permissions":[{"permissions":{"key":"inventory.view"}},{"permissions":null}]}},{"access_roles":null}]""")
        assertEquals("inventory.view", rows.first().accessRole!!.permissions.first().permissions!!.key)
        assertNull(rows.last().accessRole)
    }
    @Test fun unsupportedDeviceContractCannotReportSuccess() = runBlocking {
        val repository = DeviceRepositoryImpl()
        assertTrue(repository.registerDevice("u1", emptyMap()).isFailure)
        assertTrue(repository.checkDeviceRevoked("device").isFailure)
    }
}
