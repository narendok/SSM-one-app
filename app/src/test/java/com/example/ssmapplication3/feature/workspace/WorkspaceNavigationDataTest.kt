package com.example.ssmapplication3.feature.workspace

import com.example.ssmapplication3.feature.myday.DashboardTask
import com.example.ssmapplication3.feature.myday.TaskGrouping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceNavigationDataTest {
    @Test fun taskDepartmentIdTakesPrecedenceOverLegacyDepartmentName() {
        val task = DashboardTask("1", "Design review", "todo", "high", "Security", "hardware-id")
        assertTrue(TaskGrouping.inDepartment(task, "hardware-id", "HW", emptyList()))
        assertFalse(TaskGrouping.inDepartment(task, "security-id", "Security", emptyList()))
    }

    @Test fun hwAliasMatchesLegacyTaskData() {
        val task = DashboardTask("1", "Schematic", "in_progress", "normal", "Embedded Hardware")
        assertTrue(TaskGrouping.inDepartment(task, null, "HW", listOf("Embedded Hardware")))
        assertFalse(TaskGrouping.open(task.copy(status = "done")))
    }

    @Test fun driveListingShowsTopLevelThenSelectedFolderChildren() {
        val root = DriveEntry("root", "Engineering", "folder")
        val nested = DriveEntry("nested", "Design", "folder", "root")
        val file = DriveEntry("file", "BOM.xlsx", "file", "root")
        val entries = listOf(root, nested, file)
        assertEquals(listOf("Engineering"), DriveListing.children(entries, null, "").map { it.name })
        assertEquals(listOf("Design", "BOM.xlsx"), DriveListing.children(entries, "root", "").map { it.name })
        assertEquals(listOf("BOM.xlsx"), DriveListing.children(entries, "root", "bom").map { it.name })
    }
}
