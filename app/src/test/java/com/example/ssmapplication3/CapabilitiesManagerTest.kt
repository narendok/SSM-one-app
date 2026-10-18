package com.example.ssmapplication3

import com.example.ssmapplication3.core.security.CapabilitiesManager
import com.example.ssmapplication3.domain.model.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilitiesManagerTest {

    @Test
    fun testInternalEmployeeAccess() {
        val employee = UserProfile(
            id = "user_123",
            email = "employee@ssmone.internal",
            userType = "INTERNAL_EMPLOYEE",
            capabilities = listOf("inventory.view")
        )

        assertTrue(CapabilitiesManager.isInternalEmployee(employee))
        assertTrue(CapabilitiesManager.hasCapability(employee, "inventory.view"))
        assertTrue(CapabilitiesManager.canAccessModule(employee, "STORES"))
        assertFalse(CapabilitiesManager.canAccessModule(employee, "PRODUCTION"))
    }

    @Test
    fun testExternalCustomerAccessBlocked() {
        val customer = UserProfile(
            id = "cust_123",
            email = "customer@external.com",
            userType = "EXTERNAL_CUSTOMER",
            capabilities = listOf("VIEW_PRODUCT")
        )

        assertFalse(CapabilitiesManager.isInternalEmployee(customer))
        assertFalse(CapabilitiesManager.canAccessModule(customer, "STORES"))
        assertFalse(CapabilitiesManager.canAccessModule(customer, "MY_DAY"))
    }

    @Test
    fun testSuspendedUserAccessBlocked() {
        val suspendedEmployee = UserProfile(
            id = "user_456",
            email = "suspended@ssmone.internal",
            userType = "INTERNAL_EMPLOYEE",
            isSuspended = true,
            capabilities = listOf("UNIVERSAL_SCANNER")
        )

        assertFalse(CapabilitiesManager.isInternalEmployee(suspendedEmployee))
        assertFalse(CapabilitiesManager.hasCapability(suspendedEmployee, "UNIVERSAL_SCANNER"))
    }
}
