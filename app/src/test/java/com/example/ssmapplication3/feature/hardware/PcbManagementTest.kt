package com.example.ssmapplication3.feature.hardware

import org.junit.Assert.*
import org.junit.Test

class PcbManagementTest {

    @Test
    fun pcbStatusIsOpenForActiveRepairStates() {
        val received = PcbTask(boardName = "SSM-BRD-001", issue = "Short circuit", status = "received")
        val inProgress = PcbTask(boardName = "SSM-BRD-002", issue = "Cap replacement", status = "in_progress")
        val waitingParts = PcbTask(boardName = "SSM-BRD-003", issue = "Missing IC", status = "waiting_parts")

        assertTrue(received.isOpen)
        assertTrue(inProgress.isOpen)
        assertTrue(waitingParts.isOpen)
    }

    @Test
    fun pcbStatusIsNotOpenForTerminalStates() {
        val repaired = PcbTask(boardName = "SSM-BRD-004", issue = "Reflow solder", status = "repaired")
        val failed = PcbTask(boardName = "SSM-BRD-005", issue = "PCB trace burned", status = "failed")
        val scrapped = PcbTask(boardName = "SSM-BRD-006", issue = "Delamination", status = "scrapped")
        val returned = PcbTask(boardName = "SSM-BRD-007", issue = "Returned to vendor", status = "returned")

        assertFalse(repaired.isOpen)
        assertFalse(failed.isOpen)
        assertFalse(scrapped.isOpen)
        assertFalse(returned.isOpen)
    }

    @Test
    fun pcbStatusLabelHumanReadable() {
        assertEquals("Received", PcbTask(boardName = "B1", issue = "I1", status = "received").statusLabel)
        assertEquals("In Progress", PcbTask(boardName = "B1", issue = "I1", status = "in_progress").statusLabel)
        assertEquals("Waiting Parts", PcbTask(boardName = "B1", issue = "I1", status = "waiting_parts").statusLabel)
        assertEquals("Repaired", PcbTask(boardName = "B1", issue = "I1", status = "repaired").statusLabel)
        assertEquals("Failed", PcbTask(boardName = "B1", issue = "I1", status = "failed").statusLabel)
        assertEquals("Scrapped", PcbTask(boardName = "B1", issue = "I1", status = "scrapped").statusLabel)
        assertEquals("Returned", PcbTask(boardName = "B1", issue = "I1", status = "returned").statusLabel)
    }

    @Test
    fun pcbTaskRootCauseAndPriority() {
        val task = PcbTask(
            boardName = "SSM-MCU-V2",
            issue = "MCU overheating",
            priority = "urgent",
            status = "in_progress",
            rootCause = "thermal",
            rootCauseNotes = "Heatsink pad missing"
        )

        assertEquals("urgent", task.priority)
        assertEquals("thermal", task.rootCause)
        assertEquals("Heatsink pad missing", task.rootCauseNotes)
        assertTrue(task.isOpen)
    }
}
