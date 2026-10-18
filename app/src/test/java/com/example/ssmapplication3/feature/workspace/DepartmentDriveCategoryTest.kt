package com.example.ssmapplication3.feature.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DepartmentDriveCategoryTest {
    private fun folder(id: String, kind: String = "STANDARD", parent: String? = null, department: String = "hw") =
        DriveEntry(id, "Misleading project policy name", "FOLDER", parentId = parent, departmentId = department, folderKind = kind)

    @Test fun categoriesFollowVerifiedAncestorsIncludingBothProjectClassifications() {
        val entries = listOf(folder("common", "DEPARTMENT_STANDARDS"), folder("sub", parent = "common"),
            DriveEntry("policy", "Policy.pdf", "FILE", parentId = "sub", departmentId = "hw"),
            folder("internal", "INTERNAL_PROJECTS"), folder("client", "CLIENT_PROJECTS"),
            folder("project", parent = "client"), folder("legacy"))
        assertEquals(listOf("common", "sub", "policy"), DriveListing.inArea(entries, DriveArea.COMMON).map { it.id })
        assertEquals(listOf("internal", "client", "project"), DriveListing.inArea(entries, DriveArea.PROJECTS).map { it.id })
        assertEquals(entries, DriveListing.inArea(entries, DriveArea.ALL))
    }

    @Test fun categoryDoesNotInferMissingRootsNamesOrUnconfiguredReviews() {
        val entries = listOf(folder("a", parent = "b"), folder("b", parent = "a"),
            folder("orphan", parent = "missing"), DriveEntry("spoof", "Reviews", "FILE", folderKind = "DEPARTMENT_STANDARDS"))
        assertTrue(DriveListing.inArea(entries, DriveArea.COMMON).isEmpty())
        assertTrue(DriveListing.inArea(entries, DriveArea.PROJECTS).isEmpty())
        assertTrue(DriveListing.inArea(entries, DriveArea.REVIEWS).isEmpty())
    }

    @Test fun foreignDepartmentRootsCannotClassifyScopedOrphans() {
        val entries = listOf(folder("foreign", "DEPARTMENT_STANDARDS", department = "hr"),
            folder("owned-orphan", parent = "foreign"), folder("owned", "INTERNAL_PROJECTS"))
        val scoped = DriveListing.scoped(entries, "hw")
        assertTrue(DriveListing.inArea(scoped, DriveArea.COMMON).isEmpty())
        assertEquals(listOf("owned"), DriveListing.inArea(scoped, DriveArea.PROJECTS).map { it.id })
    }
}
