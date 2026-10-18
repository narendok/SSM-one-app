package com.example.ssmapplication3.navigation

import com.example.ssmapplication3.domain.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class WorkspacePolicyTest {
    private val admin = UserProfile("admin", "admin@example.test", userType = "INTERNAL_EMPLOYEE",
        roles = listOf("admin"), capabilities = listOf("projects.view", "inventory.view", "inventory.manage", "procurement.view"))

    @Test fun hrAndSecurityDoNotInheritAdminOperationalMenus() {
        for (department in listOf("HR", "Human Resources", "Security", "Finance", "Operations")) {
            for (screen in listOf(Screen.Inventory, Screen.Scanner, Screen.Procurement, Screen.MaterialIssue, Screen.Projects, Screen.ProjectDocuments)) {
                assertFalse("$department / ${screen.route}", WorkspacePolicy.allows(admin, department, screen.route))
            }
        }
        assertTrue(WorkspacePolicy.allows(admin, "Human Resources", Screen.HrEmployees.route))
    }

    @Test fun engineeringAliasesHaveConsistentViews() {
        for (department in listOf("Hardware", "Embedded Hardware", "R&D", "Research & Development", "Hardware & R&D", "RND — Hardware & R&D")) {
            assertTrue(WorkspacePolicy.allows(admin, department, Screen.Inventory.route))
            assertTrue(WorkspacePolicy.allows(admin, department, Screen.ProjectDetail.route))
            assertTrue(WorkspacePolicy.allows(admin, department, Screen.ProjectDocuments.route))
        }
    }

    @Test fun selectingDepartmentNeverGrantsCapabilities() {
        val noPermissions = admin.copy(capabilities = emptyList())
        assertFalse(WorkspacePolicy.allows(noPermissions, "Hardware", Screen.Inventory.route))
        assertFalse(WorkspacePolicy.allows(noPermissions, "Hardware", Screen.Projects.route))
        assertFalse(WorkspacePolicy.allows(noPermissions, "Hardware", Screen.ProjectDocuments.route))
        assertFalse(WorkspacePolicy.allows(admin.copy(isSuspended = true), "HR", Screen.HrEmployees.route))
    }

    @Test fun procurementDoesNotGrantStockAndStoresRequiresInventoryManage() {
        val procurementReader = admin.copy(capabilities = listOf("procurement.view", "inventory.view"))
        assertTrue(WorkspacePolicy.allows(procurementReader, "Purchase", Screen.Procurement.route))
        assertTrue(WorkspacePolicy.allows(procurementReader, "Purchase", Screen.Inventory.route))
        assertFalse(WorkspacePolicy.allows(procurementReader, "PROC — Procurement, Stores & Incoming Quality", Screen.StockAdjustment.route))
        assertTrue(WorkspacePolicy.allows(admin, "Stores", Screen.MaterialReturns.route))
        assertTrue(WorkspacePolicy.allows(admin, "PROC — Procurement, Stores & Incoming Quality", Screen.MaterialReturns.route))
        assertFalse(WorkspacePolicy.allows(admin.copy(capabilities = listOf("procurement.manage")), "Stores", Screen.MaterialIssue.route))
    }

    @Test fun inventoryReadVisibilityIsLimitedToAuthorizedOperationalDepartments() {
        val inventoryReader = admin.copy(capabilities = listOf("inventory.view"))
        for (department in listOf("RND — Hardware & R&D", "PROC — Procurement, Stores & Incoming Quality", "PROD — Production & Calibration", "Hardware & R&D", "Procurement, Stores & Incoming Quality", "Production & Calibration")) {
            assertTrue(department, WorkspacePolicy.allows(inventoryReader, department, Screen.Inventory.route))
        }
        for (department in listOf("HR — Human Resources", "SAL — Sales", "FIN — Finance", "FAC — Facility, Maintenance, Security, Utilities & Assets", "OPS — Operations & QMS", "ADM — Administration", "Facility, Maintenance, Security, Utilities & Assets", "Operations & QMS")) {
            assertFalse(department, WorkspacePolicy.allows(inventoryReader, department, Screen.Inventory.route))
        }
    }

    @Test fun driveBomsFollowEngineeringProcurementAndProductionAliases() {
        for (department in listOf("Hardware", "Embedded Hardware", "Firmware", "Software", "Mechanical",
            "RND — Hardware & R&D", "Hardware & R&D", "Purchase", "Stores",
            "PROC — Procurement, Stores & Incoming Quality", "Procurement, Stores & Incoming Quality",
            "PROD — Production & Calibration", "Production & Calibration")) {
            assertTrue(department, WorkspacePolicy.showsProjectBoms(department))
        }
    }

    @Test fun driveBomsAreAbsentFromUnrelatedDepartmentViews() {
        for (department in listOf("HR", "HR — Human Resources", "Human Resources", "Security",
            "Facility", "FAC — Facility, Maintenance, Security, Utilities & Assets",
            "Facility, Maintenance, Security, Utilities & Assets", "Electrical", "Maintenance",
            "SAL — Sales", "FIN — Finance", "Finance", "OPS — Operations & QMS",
            "Operations & QMS", "ADM — Administration", "", "Unknown department")) {
            assertFalse(department, WorkspacePolicy.showsProjectBoms(department))
        }
    }
}
