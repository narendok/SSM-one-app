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

    @Test fun unassignedTaskIdentifiedForTriage() {
        val unassignedTask = DashboardTask("1", "Layout rework", "todo", "high", assigneeId = null)
        val assignedTask = DashboardTask("2", "Firmware flash", "todo", "medium", assigneeId = "member-123")
        assertTrue(TaskGrouping.isUnassigned(unassignedTask))
        assertFalse(TaskGrouping.isUnassigned(assignedTask))
    }

    @Test fun todayGroupingIncludesUrgentHighAndDueToday() {
        val today = java.time.LocalDate.now()
        val dueToday = DashboardTask("1", "Inspection", "todo", "low", dueDate = today.toString())
        val urgent = DashboardTask("2", "Critical fix", "todo", "urgent", dueDate = today.plusDays(5).toString())
        val blocked = DashboardTask("3", "Waiting parts", "blocked", "low", dueDate = today.plusDays(5).toString())
        val futureNormal = DashboardTask("4", "Documentation", "todo", "low", dueDate = today.plusDays(5).toString())

        assertTrue(TaskGrouping.today(dueToday, today))
        assertTrue(TaskGrouping.today(urgent, today))
        assertTrue(TaskGrouping.today(blocked, today))
        assertFalse(TaskGrouping.today(futureNormal, today))
    }

    @Test fun overdueGroupingIdentifiesPastDueDates() {
        val today = java.time.LocalDate.now()
        val pastTask = DashboardTask("1", "Old test", "todo", "normal", dueDate = today.minusDays(2).toString())
        val futureTask = DashboardTask("2", "Next week", "todo", "normal", dueDate = today.plusDays(2).toString())

        assertTrue(TaskGrouping.isOverdue(pastTask, today))
        assertFalse(TaskGrouping.isOverdue(futureTask, today))
    }

    @Test fun departmentProjectsLinkTasksCorrectly() {
        val taskA = DashboardTask("1", "Layout board", "todo", "normal", projectId = "proj-1")
        val taskB = DashboardTask("2", "Test firmware", "todo", "normal", projectId = "proj-1")
        val taskC = DashboardTask("3", "Write doc", "todo", "normal", projectId = "proj-2")
        val tasks = listOf(taskA, taskB, taskC)

        assertEquals(2, tasks.count { it.projectId == "proj-1" })
        assertEquals(1, tasks.count { it.projectId == "proj-2" })
    }
}
