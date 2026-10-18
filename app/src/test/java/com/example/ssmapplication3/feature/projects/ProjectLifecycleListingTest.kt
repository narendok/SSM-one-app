package com.example.ssmapplication3.feature.projects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectLifecycleListingTest {
    private fun record(id: String, stageId: String, projectId: String = "project", predecessorId: String? = null) =
        ProjectProcessStageRecord(id, projectId, stageId, "IN_PROGRESS", predecessorId = predecessorId, updatedAt = "2026-10-02T00:00:00Z")

    @Test fun taxonomyDoesNotCreateProjectRecordsOrLeakOtherProjects() {
        val definitions = listOf(
            CompanyProcessStage("requirements", "HW_01", "Requirements", sortOrder = 1, requiresGatedHandover = true, active = true),
            CompanyProcessStage("release", "HW_37", "Release", sortOrder = 37, requiresGatedHandover = true, active = true)
        )
        val visible = ProjectLifecycleListing.rows("project", listOf(record("own", "requirements"), record("other", "release", "another-project")), definitions)
        assertEquals(listOf("own"), visible.map { it.record.id })
        assertEquals("IN_PROGRESS", visible.single().record.status)
    }

    @Test fun unavailableTaxonomyPreservesVisibleRecords() {
        val visible = ProjectLifecycleListing.rows("project", listOf(record("own", "requirements")), emptyList())
        assertEquals("own", visible.single().record.id)
        assertNull(visible.single().definition)
    }

    @Test fun existingRecordsRetainArchivedDefinitionsAndBackendOrder() {
        val definitions = listOf(
            CompanyProcessStage("pilot", "HW_25", "Pilot", sortOrder = 25, requiresGatedHandover = true, active = false),
            CompanyProcessStage("requirements", "HW_01", "Requirements", sortOrder = 1, requiresGatedHandover = true, active = true)
        )
        val visible = ProjectLifecycleListing.rows("project", listOf(record("later", "pilot"), record("first", "requirements")), definitions)
        assertEquals(listOf("first", "later"), visible.map { it.record.id })
        assertEquals(false, visible.last().definition?.active)
    }

    @Test fun predecessorMustBeVisibleInTheSameProject() {
        val later = record("later", "pilot", predecessorId = "earlier")
        assertNull(ProjectLifecycleListing.predecessor(later, listOf(record("earlier", "requirements", "another-project"))))
        assertEquals("earlier", ProjectLifecycleListing.predecessor(later, listOf(record("earlier", "requirements")))?.id)
    }

    @Test fun mixedGraphRetainsDistinctPredecessorsWithoutOtherTargetEdges() {
        val later = record("later", "pilot", predecessorId = "legacy")
        val edges = listOf(
            ProjectStageDependency("same", "later", "legacy"),
            ProjectStageDependency("additional", "later", "not-visible"),
            ProjectStageDependency("another", "other-target", "foreign")
        )
        assertEquals(listOf("legacy", "not-visible"), ProjectLifecycleListing.predecessorIds(later, edges))
    }

    @Test fun dependencyReadsRequireVisibleTargetInRequestedProject() {
        val edges = listOf(ProjectStageDependency("own", "ours", "hidden"), ProjectStageDependency("other", "theirs", "foreign"))
        val records = listOf(record("ours", "pilot"), record("theirs", "pilot", "another-project"))
        assertEquals(listOf("own"), ProjectLifecycleListing.scopedDependencies("project", records, edges).map { it.id })
        assertEquals(emptyList<ProjectStageDependency>(), ProjectLifecycleListing.scopedDependencies("project", emptyList(), edges))
    }
}
