package com.example.ssmapplication3.feature.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DepartmentDriveScopeTest {
    @Test fun projectDocumentsExcludeOtherProjectsAndDepartmentCommonFiles() {
        val entries = listOf(
            DriveEntry("p1", "BOM.xlsx", "file", projectId = "one", departmentId = "hardware"),
            DriveEntry("p2", "BOM.xlsx", "file", projectId = "two", departmentId = "hardware"),
            DriveEntry("common", "Policy.pdf", "file", departmentId = "hardware"),
            DriveEntry("foreign", "BOM.xlsx", "file", projectId = "one", departmentId = "hr")
        )
        assertEquals(listOf("p1"), DriveListing.scoped(entries, "hardware", "one").map { it.id })
        assertTrue(DriveListing.scoped(entries, null, "one").isEmpty())
        assertTrue(DriveListing.scoped(entries, "hardware", " ").isEmpty())
        assertEquals(listOf("p1", "p2", "common"), DriveListing.scoped(entries, "hardware").map { it.id })
    }

    @Test fun switchingProjectInvalidatesListingHistoryAndFileDeliveryEvenWithinOneDepartment() {
        val scope = DriveRequestScope()
        scope.select("hardware", "one")
        val oldListing = scope.beginRefresh()
        val oldHistory = scope.beginHistory("file-one")
        val oldOpen = scope.current()
        scope.select("hardware", "two")
        assertFalse(scope.accepts(oldListing))
        assertFalse(scope.accepts(oldHistory))
        assertFalse(scope.accepts(oldOpen))
        scope.select("hardware", "one")
        assertFalse(scope.accepts(oldListing))
        assertTrue(scope.accepts(scope.beginRefresh()))
    }

    @Test fun departmentWideAndProjectSpecificRequestsCannotReuseTheSameResult() {
        val scope = DriveRequestScope()
        scope.select("hardware")
        val departmentRequest = scope.beginRefresh()
        assertTrue(scope.select("hardware", "one"))
        assertFalse(scope.accepts(departmentRequest))
        val projectRequest = scope.beginRefresh()
        assertFalse(scope.select("hardware", "one"))
        assertTrue(scope.select("hardware"))
        assertFalse(scope.accepts(projectRequest))
    }

    @Test fun absentAndBlankDepartmentIdsShareTheSameUnassignedScope() {
        assertEquals(null, DriveListing.normalizeDepartmentId(null))
        assertEquals(null, DriveListing.normalizeDepartmentId(""))
        assertEquals(null, DriveListing.normalizeDepartmentId("  "))
        val scope = DriveRequestScope()
        assertTrue(scope.select("  "))
        assertEquals(null, scope.departmentId)
        assertFalse(scope.select(null))
    }

    @Test fun selectedDepartmentUsesOwnershipIdAndExcludesUnmappedLegacyData() {
        val entries = listOf(
            DriveEntry("hw", "HR named folder", "folder", departmentId = "hardware"),
            DriveEntry("hr", "Hardware named folder", "folder", departmentId = "hr"),
            DriveEntry("unmapped", "Hardware legacy", "folder")
        )
        assertEquals(listOf("hw"), DriveListing.scoped(entries, "hardware").map { it.id })
        assertEquals(listOf("hr"), DriveListing.scoped(entries, "hr").map { it.id })
        assertTrue(DriveListing.scoped(entries, null).isEmpty())
        assertTrue(DriveListing.scoped(entries, "").isEmpty())
    }

    @Test fun parentOutsideSelectedDepartmentDoesNotRevealItsSiblings() {
        val root = DriveEntry("global", "All departments", "folder")
        val hw = DriveEntry("hw", "Engineering", "folder", "global", departmentId = "hardware")
        val hr = DriveEntry("hr", "Employees", "folder", "global", departmentId = "hr")
        val child = DriveEntry("file", "Spec.txt", "file", "hw", departmentId = "hardware")
        val scoped = DriveListing.scoped(listOf(root, hw, hr, child), "hardware")
        assertEquals(listOf("hw"), DriveListing.children(scoped, null, "").map { it.id })
        assertEquals(listOf("file"), DriveListing.children(scoped, "hw", "").map { it.id })
        assertEquals(null, DriveListing.parentWithinScope(scoped, "hw"))
        assertEquals("hw", DriveListing.parentWithinScope(scoped, "file"))
    }

    @Test fun linkedBomsAndProjectNamesRequireAProjectInScopedDriveNodes() {
        val entries = listOf(
            DriveEntry("hw-file", "BOM.xlsx", "file", projectId = "p-hw", departmentId = "hardware"),
            DriveEntry("hr-file", "Record.txt", "file", projectId = "p-hr", departmentId = "hr"),
            DriveEntry("legacy", "Unmapped.xlsx", "file", projectId = "p-legacy")
        )
        val projects = DriveListing.projectIds(DriveListing.scoped(entries, "hardware"))
        val boms = listOf(
            DriveBom("1", "p-hw", "BOM-1", "HW BOM", lineCount = 1),
            DriveBom("2", "p-hr", "BOM-2", "HR BOM", lineCount = 1),
            DriveBom("3", "p-legacy", "BOM-3", "Unmapped BOM", lineCount = 1)
        )
        assertEquals(listOf("1"), DriveListing.bomsForProjects(boms, projects).map { it.id })
        assertEquals(mapOf("p-hw" to "HW project"), DriveListing.namesForProjects(listOf(
            DriveProject("p-hw", "HW project"), DriveProject("p-hr", "HR project")
        ), projects))
        assertTrue(DriveListing.bomsForProjects(boms, emptySet()).isEmpty())
    }

    @Test fun aLateResponseIsRejectedEvenWhenDepartmentIsSelectedAgain() {
        val scope = DriveRequestScope()
        scope.select("hardware")
        val firstHardwareRequest = scope.beginRefresh()
        scope.select("hr")
        val hrRequest = scope.beginRefresh()
        scope.select("hardware")
        val currentHardwareRequest = scope.beginRefresh()
        assertFalse(scope.accepts(firstHardwareRequest))
        assertFalse(scope.accepts(hrRequest))
        assertTrue(scope.accepts(currentHardwareRequest))
    }

    @Test fun refreshingInvalidatesOldListingAndFileOpenResults() {
        val scope = DriveRequestScope()
        scope.select("hardware")
        val oldRequest = scope.beginRefresh()
        val fileOpenRequest = scope.current()
        val newRequest = scope.beginRefresh()
        assertFalse(scope.accepts(oldRequest))
        assertFalse(scope.accepts(fileOpenRequest))
        assertTrue(scope.accepts(newRequest))
    }

    @Test fun switchingScopeOrReloadingTheSameFileRejectsPreviousHistoryResults() {
        val scope = DriveRequestScope()
        scope.select("hardware")
        scope.beginRefresh()
        val oldHistory = scope.beginHistory("same-file")
        val currentHistory = scope.beginHistory("same-file")
        assertFalse(scope.accepts(oldHistory))
        assertTrue(scope.accepts(currentHistory))
        scope.select("hr")
        assertFalse(scope.accepts(currentHistory))
    }
}
