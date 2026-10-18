package com.example.ssmapplication3.core.security

import com.example.ssmapplication3.domain.model.UserProfile

object CapabilitiesManager {
    // UI hints only. Every backend read/mutation must still pass server authorization.

    fun hasCapability(userProfile: UserProfile, capability: String): Boolean {
        return isInternalEmployee(userProfile) &&
            userProfile.capabilities.contains(capability)
    }

    fun hasAnyCapability(userProfile: UserProfile, capabilities: Collection<String>): Boolean =
        capabilities.any { hasCapability(userProfile, it) }

    fun isInternalEmployee(userProfile: UserProfile): Boolean {
        return userProfile.userType == "INTERNAL_EMPLOYEE" && !userProfile.isSuspended
    }

    fun canAccessModule(userProfile: UserProfile, moduleName: String): Boolean {
        if (!isInternalEmployee(userProfile)) return false
        return when (moduleName.uppercase()) {
            "MY_DAY" -> true
            "MY_WORK" -> true
            "STORES" -> hasAnyCapability(userProfile, listOf(
                "inventory.view", "inventory.manage", "procurement.view", "procurement.manage"
            ))
            "PROJECTS", "DOCUMENTS" -> hasAnyCapability(userProfile, listOf(
                "projects.view", "projects.manage"
            ))
            // Other feature permission mappings have not been verified for mobile yet.
            else -> false
        }
    }
}
