package com.example.ssmapplication3.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** UI identity assembled from authenticated backend reads, never a profiles table row. */
data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val userType: String = "UNKNOWN",
    val departmentId: String? = null,
    val departmentName: String? = null,
    val isSuspended: Boolean = false,
    val roles: List<String> = emptyList(),
    val capabilities: List<String> = emptyList()
)

@Serializable
data class ProfileRow(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String? = null,
    val department: String? = null
)

@Serializable
data class EmployeeRecord(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("employee_code") val employeeCode: String? = null,
    val designation: String? = null,
    @SerialName("primary_department_id") val departmentId: String? = null,
    @SerialName("employment_status") val status: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("profile_photo_url") val avatarUrl: String? = null
)

@Serializable
data class RoleRow(val role: String)

@Serializable
data class EmployeeAccessRow(@SerialName("access_roles") val accessRole: AccessRoleRow? = null)
@Serializable
data class AccessRoleRow(@SerialName("access_role_permissions") val permissions: List<RolePermissionRow> = emptyList())
@Serializable
data class RolePermissionRow(val permissions: PermissionRow? = null)
@Serializable
data class PermissionRow(val key: String)
