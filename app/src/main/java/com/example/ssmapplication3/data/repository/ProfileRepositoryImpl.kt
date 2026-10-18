package com.example.ssmapplication3.data.repository

import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.util.suspendResult
import com.example.ssmapplication3.domain.model.*
import com.example.ssmapplication3.domain.repository.ProfileRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface IdentityDataSource {
    suspend fun profile(userId: String): ProfileRow
    suspend fun employee(userId: String): EmployeeRecord?
    suspend fun roles(userId: String): List<RoleRow>
    suspend fun permissions(employeeId: String): List<EmployeeAccessRow>
    suspend fun effectivePermissions(userId: String): List<String> = emptyList()
}

class SupabaseIdentityDataSource : IdentityDataSource {
    private val database get() = SupabaseClientProvider.postgrest
    override suspend fun effectivePermissions(userId: String): List<String> =
        listOf("projects.view", "projects.manage", "inventory.view", "inventory.manage",
            "procurement.view", "procurement.manage").filter { permission ->
            database.rpc("has_permission", buildJsonObject {
                put("_user_id", userId)
                put("_permission_key", permission)
            }).decodeAs<Boolean>()
        }
    override suspend fun profile(userId: String): ProfileRow = database.from("profiles")
        .select(Columns.list("id", "email", "display_name", "department")) {
            filter { eq("id", userId) }
        }.decodeSingle()

    override suspend fun employee(userId: String): EmployeeRecord? = database.from("employees")
        .select(Columns.list("id", "user_id", "employee_code", "designation", "primary_department_id",
            "employment_status", "display_name", "profile_photo_url")) {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull()

    override suspend fun roles(userId: String): List<RoleRow> = database.from("user_roles")
        .select(Columns.list("role")) { filter { eq("user_id", userId) } }.decodeList()

    // Same relation projection as the existing web useAuth.tsx implementation.
    override suspend fun permissions(employeeId: String): List<EmployeeAccessRow> =
        database.from("employee_access_roles")
            .select(Columns.raw("access_roles(access_role_permissions(permissions(key)))")) {
                filter { eq("employee_id", employeeId) }
            }.decodeList()
}

class ProfileRepositoryImpl(private val source: IdentityDataSource = SupabaseIdentityDataSource()) : ProfileRepository {
    override suspend fun fetchUserProfile(userId: String): Result<UserProfile> = suspendResult {
        val profile = source.profile(userId)
        check(profile.id == userId) { "Profile identity does not match the session." }
        val employee = source.employee(userId)
        check(employee == null || employee.userId == userId) { "Employee identity does not match the session." }
        val active = employee?.status == "ACTIVE"
        val keys = if (active) source.permissions(requireNotNull(employee).id)
            .flatMap { it.accessRole?.permissions.orEmpty() }
            .mapNotNull { it.permissions?.key }.distinct() else emptyList()
        UserProfile(
            id = profile.id, email = profile.email,
            fullName = employee?.displayName ?: profile.displayName,
            avatarUrl = employee?.avatarUrl,
            userType = if (active) "INTERNAL_EMPLOYEE" else "UNKNOWN",
            departmentId = employee?.departmentId,
            departmentName = profile.department,
            isSuspended = employee?.status == "SUSPENDED",
            roles = source.roles(userId).map { it.role },
            capabilities = if (active) (keys + source.effectivePermissions(userId)).distinct() else emptyList()
        )
    }

    override suspend fun fetchEmployeeRecord(userId: String): Result<EmployeeRecord?> = suspendResult {
        source.employee(userId)
    }
}
