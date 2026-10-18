package com.example.ssmapplication3.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.util.suspendResult
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Read-only fields from VERIFIED_DOCUMENT_CONTROL_READ_CONTRACT_2026-10-01.md. */
@Serializable
data class CompanyProcessStage(
    val id: String,
    @SerialName("stage_code") val code: String,
    val title: String,
    val description: String? = null,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("requires_gated_handover") val requiresGatedHandover: Boolean,
    @SerialName("is_active") val active: Boolean
)

@Serializable
data class ProjectProcessStageRecord(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("company_process_stage_id") val stageId: String,
    val status: String,
    @SerialName("owner_employee_id") val ownerId: String? = null,
    @SerialName("reviewer_employee_id") val reviewerId: String? = null,
    @SerialName("predecessor_record_id") val predecessorId: String? = null,
    @SerialName("linked_task_id") val taskId: String? = null,
    @SerialName("evidence_drive_node_id") val evidenceId: String? = null,
    @SerialName("change_request_id") val changeRequestId: String? = null,
    @SerialName("handover_note") val handoverNote: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("lifecycle_template_id") val templateId: String? = null,
    @SerialName("lifecycle_template_version") val templateVersion: Int? = null,
    @SerialName("lifecycle_template_stage_id") val templateStageId: String? = null
)

/** Read-only multi-predecessor edges from the October 2 lifecycle contract. */
@Serializable
data class ProjectStageDependency(
    val id: String,
    @SerialName("project_stage_record_id") val targetId: String,
    @SerialName("predecessor_stage_record_id") val predecessorId: String
)

data class ProjectLifecycleRow(val record: ProjectProcessStageRecord, val definition: CompanyProcessStage?)

object ProjectLifecycleListing {
    // Keep visible records even when taxonomy is unavailable; missing definitions
    // must not turn into fabricated NOT_STARTED or COMPLETE project records.
    fun rows(projectId: String, records: List<ProjectProcessStageRecord>, definitions: List<CompanyProcessStage>): List<ProjectLifecycleRow> {
        val byId = definitions.associateBy { it.id }
        return records.filter { it.projectId == projectId }
            .map { ProjectLifecycleRow(it, byId[it.stageId]) }
            .sortedWith(compareBy<ProjectLifecycleRow> { it.definition?.sortOrder ?: Int.MAX_VALUE }
                .thenBy { it.definition?.code ?: it.record.stageId })
    }

    fun predecessor(record: ProjectProcessStageRecord, records: List<ProjectProcessStageRecord>): ProjectProcessStageRecord? =
        records.firstOrNull { it.id == record.predecessorId && it.projectId == record.projectId }

    // Both graph representations remain visible; they must not fabricate a stage
    // when the predecessor record is outside the current RLS-visible result.
    fun predecessorIds(record: ProjectProcessStageRecord, edges: List<ProjectStageDependency>): List<String> =
        (listOfNotNull(record.predecessorId) + edges.filter { it.targetId == record.id }.map { it.predecessorId }).distinct()

    fun scopedDependencies(projectId: String, records: List<ProjectProcessStageRecord>, edges: List<ProjectStageDependency>): List<ProjectStageDependency> {
        val visibleIds = records.filter { it.projectId == projectId }.map { it.id }.toSet()
        return edges.filter { it.targetId in visibleIds }
    }
}

data class ProjectLifecycleState(
    val records: List<ProjectProcessStageRecord> = emptyList(),
    val definitions: List<CompanyProcessStage> = emptyList(),
    val recordsLoading: Boolean = true,
    val definitionsLoading: Boolean = true,
    val recordsError: String? = null,
    val definitionsError: String? = null,
    val dependencies: List<ProjectStageDependency> = emptyList(),
    val dependenciesLoading: Boolean = true,
    val dependenciesError: String? = null
)

enum class StageLink { TASK, EVIDENCE, CHANGE_REQUEST, OWNER, REVIEWER }

data class StageLinkDetails(
    val title: String,
    val id: String,
    val fields: List<Pair<String, String>> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

@Serializable
private data class LinkedLifecycleTask(
    val id: String, val title: String, val status: String, val priority: String,
    val description: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("assignee_id") val assigneeId: String? = null
)

@Serializable
private data class LinkedLifecycleEvidence(
    val id: String, val name: String,
    @SerialName("node_type") val nodeType: String,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("current_version") val version: Int,
    @SerialName("mime_type") val mime: String? = null,
    @SerialName("is_locked") val locked: Boolean,
    @SerialName("sha256_checksum") val checksum: String? = null,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class LinkedLifecycleChange(
    val id: String, val title: String, val status: String,
    @SerialName("change_code") val code: String,
    @SerialName("change_type") val type: String,
    val description: String? = null,
    @SerialName("impact_summary") val impactSummary: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("implemented_at") val implementedAt: String? = null
)

@Serializable
private data class LinkedLifecycleEmployee(
    val id: String,
    @SerialName("display_name") val name: String? = null,
    @SerialName("employee_code") val code: String? = null,
    val designation: String? = null
)

/** No writes, public sharing or controlled-transition RPCs in this repository. */
class ProjectLifecycleRepository {
    private val db get() = SupabaseClientProvider.postgrest

    suspend fun records(projectId: String): List<ProjectProcessStageRecord> =
        db.from("project_process_stage_records").select(Columns.list(
            "id", "project_id", "company_process_stage_id", "status", "owner_employee_id", "reviewer_employee_id",
            "predecessor_record_id", "linked_task_id", "evidence_drive_node_id", "change_request_id", "handover_note", "updated_at",
            "lifecycle_template_id", "lifecycle_template_version", "lifecycle_template_stage_id"
        )) { filter { eq("project_id", projectId) } }.decodeList<ProjectProcessStageRecord>()

    suspend fun definitions(): List<CompanyProcessStage> =
        db.from("company_process_stages").select(Columns.list(
            "id", "stage_code", "title", "description", "sort_order", "requires_gated_handover", "is_active"
        )).decodeList<CompanyProcessStage>()

    suspend fun dependencies(projectId: String, records: List<ProjectProcessStageRecord>): List<ProjectStageDependency> {
        val visibleIds = records.filter { it.projectId == projectId }.map { it.id }
        if (visibleIds.isEmpty()) return emptyList()
        val edges = db.from("project_stage_dependencies").select(Columns.list(
            "id", "project_stage_record_id", "predecessor_stage_record_id"
        )) { filter { isIn("project_stage_record_id", visibleIds) } }.decodeList<ProjectStageDependency>()
        return ProjectLifecycleListing.scopedDependencies(projectId, records, edges)
    }

    suspend fun linkedDetails(projectId: String, link: StageLink, id: String): List<Pair<String, String>>? = when (link) {
        StageLink.TASK -> db.from("project_tasks").select(Columns.list(
            "id", "title", "status", "priority", "description", "due_date", "assignee_id"
        )) { filter { eq("id", id); eq("project_id", projectId) } }.decodeList<LinkedLifecycleTask>().singleOrNull()?.let {
            listOf("Title" to it.title, "Status" to it.status, "Priority" to it.priority,
                "Due" to (it.dueDate ?: "Not scheduled"), "Assignee reference" to (it.assigneeId ?: "Unassigned"),
                "Description" to (it.description ?: "Not provided"))
        }
        StageLink.EVIDENCE -> db.from("drive_nodes").select(Columns.list(
            "id", "name", "node_type", "project_id", "current_version", "mime_type", "is_locked", "sha256_checksum", "updated_at"
        )) { filter { eq("id", id); eq("is_trashed", false) } }.decodeList<LinkedLifecycleEvidence>().singleOrNull()?.let {
            // An explicitly linked reference can be a common department document.
            // The node's RLS policy still controls access; storage paths are never fetched here.
            listOf("File" to it.name, "Kind" to it.nodeType, "Current file version" to it.version.toString(),
                "Type" to (it.mime ?: "Not provided"), "Locked" to if (it.locked) "Yes" else "No",
                "Project reference" to (it.projectId ?: "Common / department document"),
                "Checksum" to (it.checksum ?: "Not recorded"), "Updated" to it.updatedAt)
        }
        StageLink.CHANGE_REQUEST -> db.from("project_change_requests").select(Columns.list(
            "id", "title", "status", "change_code", "change_type", "description", "impact_summary", "approved_at", "implemented_at"
        )) { filter { eq("id", id); eq("project_id", projectId) } }.decodeList<LinkedLifecycleChange>().singleOrNull()?.let {
            listOf("Change" to it.code, "Title" to it.title, "Type" to it.type, "Status" to it.status,
                "Description" to (it.description ?: "Not provided"), "Impact" to (it.impactSummary ?: "Not provided"),
                "Approved" to (it.approvedAt ?: "Not recorded"), "Implemented" to (it.implementedAt ?: "Not recorded"))
        }
        StageLink.OWNER, StageLink.REVIEWER -> db.from("employees").select(Columns.list("id", "display_name", "employee_code", "designation")) {
            filter { eq("id", id) }
        }.decodeList<LinkedLifecycleEmployee>().singleOrNull()?.let {
            listOf("Name" to (it.name ?: "Not provided"), "Employee code" to (it.code ?: "Not provided"),
                "Designation" to (it.designation ?: "Not provided"))
        }
    }
}

class ProjectLifecycleViewModel(
    private val projectId: String,
    private val repository: ProjectLifecycleRepository = ProjectLifecycleRepository()
) : ViewModel() {
    private val mutable = MutableStateFlow(ProjectLifecycleState())
    val state = mutable.asStateFlow()
    private val linkedMutable = MutableStateFlow<StageLinkDetails?>(null)
    val linked = linkedMutable.asStateFlow()
    private var loadJob: Job? = null
    private var linkJob: Job? = null

    init { refresh() }

    fun refresh() {
        dismissLink()
        loadJob?.cancel()
        mutable.value = ProjectLifecycleState()
        loadJob = viewModelScope.launch {
            // Independent section results retain actual project records even when
            // the optional taxonomy read fails or isn't permitted.
            launch {
                suspendResult { repository.records(projectId) }
                    .onSuccess { records ->
                        mutable.update { it.copy(records = records, recordsLoading = false) }
                        suspendResult { repository.dependencies(projectId, records) }
                            .onSuccess { edges -> mutable.update { it.copy(dependencies = edges, dependenciesLoading = false) } }
                            .onFailure { mutable.update { it.copy(dependenciesLoading = false,
                                dependenciesError = "Additional stage dependencies are unavailable. Legacy links remain visible.") } }
                    }
                    .onFailure { mutable.update { it.copy(recordsLoading = false, dependenciesLoading = false,
                        recordsError = "Project stages could not be loaded.") } }
            }
            launch {
                suspendResult { repository.definitions() }
                    .onSuccess { definitions -> mutable.update { it.copy(definitions = definitions, definitionsLoading = false) } }
                    .onFailure { mutable.update { it.copy(definitionsLoading = false, definitionsError = "Stage names are unavailable. Visible records are shown by reference.") } }
            }
        }
    }

    fun showLink(link: StageLink, id: String) {
        linkJob?.cancel()
        val title = when (link) {
            StageLink.TASK -> "Linked task"
            StageLink.EVIDENCE -> "Linked evidence"
            StageLink.CHANGE_REQUEST -> "Change request"
            StageLink.OWNER -> "Stage owner"
            StageLink.REVIEWER -> "Stage reviewer"
        }
        linkedMutable.value = StageLinkDetails(title, id)
        linkJob = viewModelScope.launch {
            suspendResult { repository.linkedDetails(projectId, link, id) }
                .onSuccess { fields -> linkedMutable.value = StageLinkDetails(title, id, fields.orEmpty(), loading = false,
                    error = if (fields == null) "This linked record is unavailable or is not visible to your account." else null) }
                .onFailure { linkedMutable.value = StageLinkDetails(title, id, loading = false, error = "The linked record could not be loaded. Close and try again.") }
        }
    }

    fun dismissLink() {
        linkJob?.cancel()
        linkedMutable.value = null
    }
}

@Composable
fun ProjectLifecyclePanel(projectId: String) {
    val lifecycle: ProjectLifecycleViewModel = viewModel(
        key = "project-lifecycle-$projectId",
        factory = viewModelFactory { initializer { ProjectLifecycleViewModel(projectId) } }
    )
    val data by lifecycle.state.collectAsState()
    val linked by lifecycle.linked.collectAsState()
    var expandedId by rememberSaveable(projectId) { mutableStateOf<String?>(null) }
    val rows = ProjectLifecycleListing.rows(projectId, data.records, data.definitions)
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Project lifecycle", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = lifecycle::refresh) { Text("Refresh") }
            }
            Text("Stages, handovers and linked evidence", style = MaterialTheme.typography.bodySmall)
        }
        if (data.recordsLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Loading project stages…") }
        if (data.definitionsLoading && !data.recordsLoading) item { Text("Loading stage names…", style = MaterialTheme.typography.bodySmall) }
        data.recordsError?.let { message -> item {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = lifecycle::refresh) { Text("Retry") }
        } }
        data.definitionsError?.let { message -> item { Text(message, style = MaterialTheme.typography.bodySmall) } }
        if (data.dependenciesLoading && !data.recordsLoading) item { Text("Loading stage dependencies…", style = MaterialTheme.typography.bodySmall) }
        data.dependenciesError?.let { message -> item { Text(message, style = MaterialTheme.typography.bodySmall) } }
        if (!data.recordsLoading && data.recordsError == null) {
            item {
                if (rows.isEmpty()) Text("No lifecycle records are visible for this project. Records may be unassigned or outside your access.")
                else Text("${rows.size} recorded stages · ${rows.count { it.record.status == "COMPLETE" }} marked complete", style = MaterialTheme.typography.bodySmall)
            }
            items(rows, key = { it.record.id }) { row ->
                val expanded = expandedId == row.record.id
                Card(onClick = { expandedId = if (expanded) null else row.record.id }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(row.definition?.code ?: "Stage reference: ${row.record.stageId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(row.definition?.title ?: "Stage definition unavailable", style = MaterialTheme.typography.titleSmall)
                        Text(row.record.status.replace('_', ' '), style = MaterialTheme.typography.bodySmall)
                        Text(if (expanded) "Hide details" else "View owner, handover & evidence", style = MaterialTheme.typography.labelSmall)
                        if (expanded) {
                            row.definition?.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            if (row.definition?.active == false) Text("Archived stage definition", style = MaterialTheme.typography.bodySmall)
                            if (row.definition?.requiresGatedHandover == true) Text("Review handover required by the stage definition", style = MaterialTheme.typography.bodySmall)
                            Text("Updated: ${row.record.updatedAt}", style = MaterialTheme.typography.bodySmall)
                            Text("Handover note: ${row.record.handoverNote?.takeIf { it.isNotBlank() } ?: "Not recorded"}", style = MaterialTheme.typography.bodySmall)
                            row.record.templateId?.let { templateId ->
                                Text("Template: $templateId · version ${row.record.templateVersion?.toString() ?: "not recorded"}", style = MaterialTheme.typography.bodySmall)
                                row.record.templateStageId?.let { Text("Template stage: $it", style = MaterialTheme.typography.bodySmall) }
                            }
                            ProjectLifecycleListing.predecessorIds(row.record, data.dependencies).forEach { predecessorId ->
                                val predecessor = data.records.firstOrNull { it.id == predecessorId && it.projectId == projectId }
                                val definition = data.definitions.firstOrNull { it.id == predecessor?.stageId }
                                Text(if (predecessor == null) "Predecessor not visible: $predecessorId" else
                                    "Predecessor: ${definition?.code ?: predecessor.stageId} · ${predecessor.status.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall)
                            }
                            StageReferenceButton("Owner", row.record.ownerId, StageLink.OWNER, lifecycle::showLink)
                            StageReferenceButton("Reviewer", row.record.reviewerId, StageLink.REVIEWER, lifecycle::showLink)
                            StageReferenceButton("Task", row.record.taskId, StageLink.TASK, lifecycle::showLink)
                            StageReferenceButton("Evidence", row.record.evidenceId, StageLink.EVIDENCE, lifecycle::showLink)
                            StageReferenceButton("Change request", row.record.changeRequestId, StageLink.CHANGE_REQUEST, lifecycle::showLink)
                        }
                    }
                }
            }
        }
    }
    linked?.let { details ->
        AlertDialog(onDismissRequest = lifecycle::dismissLink,
            confirmButton = { TextButton(onClick = lifecycle::dismissLink) { Text("Close") } },
            title = { Text(details.title) },
            text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (details.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Loading linked record…") }
                details.error?.let { message -> item { Text(message) } }
                items(details.fields) { (label, value) -> Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                } }
                item { Text("Reference: ${details.id}", style = MaterialTheme.typography.labelSmall) }
            } })
    }
}

@Composable
private fun StageReferenceButton(label: String, id: String?, link: StageLink, onClick: (StageLink, String) -> Unit) {
    if (id == null) Text("$label: not linked", style = MaterialTheme.typography.bodySmall)
    else TextButton(onClick = { onClick(link, id) }) { Text("View ${label.lowercase()}") }
}
