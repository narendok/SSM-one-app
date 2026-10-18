package com.example.ssmapplication3.feature.workspace

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.util.suspendResult
import com.example.ssmapplication3.core.ui.*
import com.example.ssmapplication3.navigation.WorkspacePolicy
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable data class DriveEntry(
    val id: String, val name: String,
    @SerialName("node_type") val nodeType: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("storage_bucket") val bucket: String? = null,
    @SerialName("storage_path") val path: String? = null,
    @SerialName("current_version") val currentVersion: Int = 1,
    @SerialName("is_locked") val locked: Boolean = false,
    @SerialName("sha256_checksum") val checksum: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("department_id") val departmentId: String? = null,
    @SerialName("folder_kind") val folderKind: String = "STANDARD"
) {
    val folder get() = nodeType.equals("folder", true)
    val hasSource get() = !name.endsWith(".controlled-record", true) && !bucket.isNullOrBlank() && !path.isNullOrBlank()
}

@Serializable data class DriveRevision(
    val id: String, val version: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("change_summary") val changeSummary: String? = null,
    @SerialName("sha256_checksum") val checksum: String? = null,
    @SerialName("file_size_bytes") val sizeBytes: Long
)
@Serializable data class ControlledDocumentRead(
    val id: String,
    @SerialName("document_number") val number: String,
    @SerialName("document_status") val status: String,
    @SerialName("controlled_version") val version: Int,
    @SerialName("source_revision_id") val sourceRevisionId: String? = null,
    @SerialName("reviewer_decision") val reviewerDecision: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("released_at") val releasedAt: String? = null,
    @SerialName("change_reason") val changeReason: String? = null
)
@Serializable data class DocumentEventRead(
    val id: String,
    @SerialName("event_type") val type: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("to_status") val toStatus: String? = null,
    val version: Int? = null,
    val note: String? = null,
    @SerialName("revision_id") val revisionId: String? = null
)
data class DriveHistory(
    val nodeId: String? = null, val revisions: List<DriveRevision> = emptyList(),
    val loading: Boolean = false, val error: String? = null,
    val control: ControlledDocumentRead? = null, val events: List<DocumentEventRead> = emptyList(),
    val controlError: String? = null
)

@Serializable data class DriveProject(val id: String, val name: String)
@Serializable data class DriveBom(
    val id: String, @SerialName("project_id") val projectId: String,
    @SerialName("bom_number") val number: String, val name: String,
    val revision: String? = null, @SerialName("line_count") val lineCount: Int,
    @SerialName("source_filename") val sourceFilename: String? = null
)

enum class DriveArea(val label: String, val title: String) {
    ALL("All", "All department documents"),
    COMMON("Common", "Common Controlled Documents"),
    REVIEWS("Reviews", "Periodic and Conditional Reviews"),
    PROJECTS("Projects", "Project Deliverables")
}

object DriveListing {
    fun normalizeDepartmentId(departmentId: String?): String? = departmentId?.takeIf { it.isNotBlank() }

    // A null department is unmapped data, not a shared/global department.
    fun scoped(entries: List<DriveEntry>, departmentId: String?, projectId: String? = null): List<DriveEntry> =
        if (departmentId.isNullOrBlank()) emptyList() else entries.filter {
            it.departmentId == departmentId && (projectId == null || it.projectId == projectId)
        }

    fun projectIds(entries: List<DriveEntry>): Set<String> = entries.mapNotNull { it.projectId }.toSet()

    fun bomsForProjects(boms: List<DriveBom>, projectIds: Set<String>): List<DriveBom> =
        boms.filter { it.projectId in projectIds }

    fun namesForProjects(projects: List<DriveProject>, projectIds: Set<String>): Map<String, String> =
        projects.filter { it.id in projectIds }.associate { it.id to it.name }

    fun parentWithinScope(entries: List<DriveEntry>, nodeId: String?): String? =
        entries.firstOrNull { it.id == nodeId }?.parentId?.takeIf { parentId ->
            entries.any { it.id == parentId }
        }

    fun children(entries: List<DriveEntry>, parent: String?, query: String): List<DriveEntry> =
        entries.filter { if (parent == null) it.parentId == null || entries.none { other -> other.id == it.parentId } else it.parentId == parent }
            .filter { it.name.contains(query, true) }.sortedWith(compareByDescending<DriveEntry> { it.folder }.thenBy { it.name.lowercase() })

    /** Category presentation uses verified root kinds, never folder names or project titles.
     * Input must already be scoped to the selected department/project by scoped(). */
    fun inArea(entries: List<DriveEntry>, area: DriveArea): List<DriveEntry> {
        if (area == DriveArea.ALL) return entries
        // No authoritative review category mapping has been deployed yet.
        if (area == DriveArea.REVIEWS) return emptyList()
        val kinds = if (area == DriveArea.COMMON) setOf("DEPARTMENT_STANDARDS")
            else setOf("INTERNAL_PROJECTS", "CLIENT_PROJECTS")
        val byId = entries.associateBy { it.id }
        return entries.filter { entry ->
            var ancestor: DriveEntry? = entry
            val visited = mutableSetOf<String>()
            var matches = false
            while (ancestor != null && visited.add(ancestor.id)) {
                if (ancestor.folder && ancestor.folderKind in kinds) { matches = true; break }
                ancestor = ancestor.parentId?.let(byId::get)
            }
            matches
        }
    }
}
data class DriveData(
    val entries: List<DriveEntry> = emptyList(),
    val boms: List<DriveBom> = emptyList(),
    val projects: Map<String, String> = emptyMap(),
    val loading: Boolean = true, val error: String? = null,
    val departmentId: String? = null,
    val linkedRecordsError: String? = null,
    val projectId: String? = null
)

internal data class DriveRequest(val departmentId: String?, val generation: Long, val projectId: String? = null)
internal data class DriveHistoryRequest(val scope: DriveRequest, val sequence: Long, val nodeId: String)

/** Invalidates earlier responses even if a user switches A -> B -> A or refreshes twice. */
internal class DriveRequestScope {
    private var initialized = false
    private var generation = 0L
    private var historySequence = 0L
    var departmentId: String? = null
        private set
    var projectId: String? = null
        private set

    fun select(departmentId: String?, projectId: String? = null): Boolean {
        val normalized = DriveListing.normalizeDepartmentId(departmentId)
        if (initialized && this.departmentId == normalized && this.projectId == projectId) return false
        initialized = true
        this.departmentId = normalized
        this.projectId = projectId
        generation++
        historySequence++
        return true
    }

    fun beginRefresh(): DriveRequest {
        generation++
        historySequence++
        return current()
    }

    fun current() = DriveRequest(departmentId, generation, projectId)
    fun accepts(request: DriveRequest) = initialized && request == current()
    fun beginHistory(nodeId: String) = DriveHistoryRequest(current(), ++historySequence, nodeId)
    fun accepts(request: DriveHistoryRequest) = accepts(request.scope) && request.sequence == historySequence
}

sealed interface DriveEffect {
    val departmentId: String?
    val generation: Long
    val projectId: String?
    data class Open(val url: String, val mime: String?, override val departmentId: String?, override val generation: Long,
                    override val projectId: String? = null) : DriveEffect
    data class Message(val text: String, override val departmentId: String?, override val generation: Long,
                       override val projectId: String? = null) : DriveEffect
}
class DepartmentDriveViewModel : ViewModel() {
    private val requestScope = DriveRequestScope()
    private var includeProjectBoms = false
    private var listingJob: Job? = null
    private var historyJob: Job? = null
    private var openJob: Job? = null
    private val historyMutable = MutableStateFlow(DriveHistory())
    val history = historyMutable.asStateFlow()
    private val mutable = MutableStateFlow(DriveData())
    val state = mutable.asStateFlow()
    private val effectsMutable = MutableSharedFlow<DriveEffect>()
    val effects = effectsMutable.asSharedFlow()
    fun selectDepartment(departmentId: String?, showProjectBoms: Boolean, projectId: String? = null) {
        val scopeChanged = requestScope.select(departmentId, projectId)
        val presentationChanged = includeProjectBoms != showProjectBoms
        includeProjectBoms = showProjectBoms
        if (scopeChanged || presentationChanged) refresh()
    }

    fun accepts(effect: DriveEffect): Boolean =
        requestScope.accepts(DriveRequest(effect.departmentId, effect.generation, effect.projectId))

    fun refresh() {
        val request = requestScope.beginRefresh()
        val loadProjectBoms = includeProjectBoms
        listingJob?.cancel()
        historyJob?.cancel()
        openJob?.cancel()
        historyMutable.value = DriveHistory()
        // Clear data synchronously, before a new backend query starts.
        mutable.value = DriveData(departmentId = request.departmentId, projectId = request.projectId)
        val departmentId = request.departmentId
        if (departmentId == null) {
            mutable.value = DriveData(loading = false, departmentId = null, projectId = request.projectId,
                error = "A department must be assigned or selected before opening its Drive.")
            return
        }
        listingJob = viewModelScope.launch {
        suspendResult {
            SupabaseClientProvider.postgrest.from("drive_nodes")
                .select(Columns.list("id", "name", "node_type", "parent_id", "mime_type", "storage_bucket", "storage_path", "current_version", "is_locked", "sha256_checksum", "updated_at", "project_id", "department_id", "folder_kind")) {
                    filter {
                        eq("is_trashed", false)
                        eq("department_id", departmentId)
                        request.projectId?.let { eq("project_id", it) }
                    }
                }.decodeList<DriveEntry>()
        }.onSuccess { response ->
            if (!requestScope.accepts(request)) return@onSuccess
            val entries = DriveListing.scoped(response, departmentId, request.projectId)
            val projectIds = DriveListing.projectIds(entries)
            val db = SupabaseClientProvider.postgrest
            val bomResult = suspendResult {
                if (!loadProjectBoms || projectIds.isEmpty()) emptyList() else
                    db.from("project_boms").select(Columns.list("id", "project_id", "bom_number", "name", "revision", "line_count", "source_filename")) {
                        filter { isIn("project_id", projectIds.toList()) }
                    }.decodeList<DriveBom>()
            }
            val projectResult = suspendResult {
                if (projectIds.isEmpty()) emptyList() else
                    db.from("projects").select(Columns.list("id", "name")) {
                        filter { isIn("id", projectIds.toList()) }
                    }.decodeList<DriveProject>()
            }
            val boms = DriveListing.bomsForProjects(bomResult.getOrDefault(emptyList()), projectIds)
            val projects = DriveListing.namesForProjects(projectResult.getOrDefault(emptyList()), projectIds)
            if (requestScope.accepts(request)) mutable.value = DriveData(entries, boms, projects,
                loading = false, departmentId = departmentId, projectId = request.projectId,
                linkedRecordsError = if (bomResult.isFailure || projectResult.isFailure)
                    "Some linked project or BOM records could not be loaded. Refresh to retry." else null)
        }
            .onFailure { if (requestScope.accepts(request)) mutable.value = DriveData(loading = false,
                error = "Drive could not be loaded. Refresh to try again.", departmentId = departmentId,
                projectId = request.projectId) }
        }
    }
    fun loadHistory(file: DriveEntry) {
        if (!isVisible(file)) return
        val departmentId = file.departmentId ?: return
        val request = requestScope.beginHistory(file.id)
        historyJob?.cancel()
        historyMutable.value = DriveHistory(nodeId = file.id, loading = true)
        historyJob = viewModelScope.launch {
        val revisionResult = suspendResult {
            SupabaseClientProvider.postgrest.from("drive_node_revisions")
                .select(Columns.list("id", "version", "created_at", "change_summary", "sha256_checksum", "file_size_bytes")) {
                    filter { eq("node_id", file.id) }
                }.decodeList<DriveRevision>().sortedByDescending { it.version }
        }
            val controlResult = suspendResult {
                SupabaseClientProvider.postgrest.from("document_control_registers")
                    .select(Columns.list("id", "document_number", "document_status", "controlled_version", "source_revision_id", "reviewer_decision", "approved_at", "released_at", "change_reason")) {
                        filter { eq("drive_node_id", file.id); eq("department_id", departmentId) }
                    }.decodeList<ControlledDocumentRead>().firstOrNull()
            }
            val control = controlResult.getOrNull()
            val eventResult = suspendResult {
                if (control == null) emptyList() else SupabaseClientProvider.postgrest.from("document_control_events")
                    .select(Columns.list("id", "event_type", "created_at", "to_status", "version", "note", "revision_id")) {
                        filter { eq("register_id", control.id) }
                    }.decodeList<DocumentEventRead>().sortedByDescending { it.createdAt }
            }
            if (requestScope.accepts(request) && isVisible(file)) historyMutable.value = DriveHistory(
                nodeId = file.id, revisions = revisionResult.getOrDefault(emptyList()), control = control,
                error = if (revisionResult.isFailure) "Revision history could not be loaded. Try again." else null,
                events = eventResult.getOrDefault(emptyList()),
                controlError = if (controlResult.isFailure || eventResult.isFailure) "Document-control history could not be loaded. Try again." else null
            )
        }
    }
    private fun isVisible(file: DriveEntry) = file.departmentId == requestScope.departmentId &&
        mutable.value.entries.any { it == file } && !mutable.value.loading

    fun open(file: DriveEntry) {
        if (!isVisible(file)) return
        val request = requestScope.current()
        openJob?.cancel()
        openJob = viewModelScope.launch {
        if (file.name.endsWith(".controlled-record", true) || file.bucket.isNullOrBlank() || file.path.isNullOrBlank()) {
            if (requestScope.accepts(request)) effectsMutable.emit(DriveEffect.Message(
                "This record has no uploaded source file available to open.", request.departmentId, request.generation, request.projectId))
        } else suspendResult { SupabaseClientProvider.storage.from(file.bucket).createSignedUrl(file.path, 10.minutes) }
            .onSuccess { if (requestScope.accepts(request)) effectsMutable.emit(DriveEffect.Open(it, file.mimeType,
                request.departmentId, request.generation, request.projectId)) }
            .onFailure { if (requestScope.accepts(request)) effectsMutable.emit(DriveEffect.Message(
                "File could not be opened. Access may have changed.", request.departmentId, request.generation, request.projectId)) }
        }
    }
}

@Composable
fun DepartmentDriveScreen(viewModel: DepartmentDriveViewModel, department: String, departmentId: String?, projectId: String? = null, onProjectClick: ((String) -> Unit)? = null) {
    val scopeDepartmentId = DriveListing.normalizeDepartmentId(departmentId)
    val showProjectBoms = WorkspacePolicy.showsProjectBoms(department)
    val loadedData by viewModel.state.collectAsState()
    // Suppress the previous scope during the frame before LaunchedEffect starts the new load.
    val data = if (loadedData.departmentId == scopeDepartmentId && loadedData.projectId == projectId) loadedData
        else DriveData(departmentId = scopeDepartmentId, projectId = projectId)
    val history by viewModel.history.collectAsState()
    var parent by rememberSaveable(scopeDepartmentId, projectId) { mutableStateOf<String?>(null) }
    var query by rememberSaveable(scopeDepartmentId, projectId) { mutableStateOf("") }
    var section by rememberSaveable(scopeDepartmentId, projectId, showProjectBoms) { mutableStateOf(0) }
    var area by rememberSaveable(scopeDepartmentId, projectId) { mutableStateOf(DriveArea.ALL) }
    var selectedBom by remember(scopeDepartmentId, projectId, showProjectBoms) { mutableStateOf<DriveBom?>(null) }
    var selectedFile by remember(scopeDepartmentId, projectId) { mutableStateOf<DriveEntry?>(null) }
    LaunchedEffect(viewModel, scopeDepartmentId, projectId, showProjectBoms) {
        viewModel.selectDepartment(scopeDepartmentId, showProjectBoms, projectId)
    }
    LaunchedEffect(data.loading) {
        if (data.loading) { selectedFile = null; selectedBom = null }
    }
    LaunchedEffect(data.entries, data.loading) {
        if (!data.loading && parent != null && data.entries.none { it.id == parent }) {
            parent = null
            query = ""
        }
    }
    val areaEntries = DriveListing.inArea(data.entries, if (projectId == null) area else DriveArea.ALL)
    val current = areaEntries.firstOrNull { it.id == parent }
    val context = LocalContext.current
    BackHandler(parent != null) { parent = DriveListing.parentWithinScope(areaEntries, parent); query = "" }
    LaunchedEffect(viewModel, scopeDepartmentId, projectId) {
        viewModel.effects.collect { effect ->
            if (effect.departmentId != scopeDepartmentId || effect.projectId != projectId || !viewModel.accepts(effect)) return@collect
            when (effect) {
                is DriveEffect.Message -> Toast.makeText(context, effect.text, Toast.LENGTH_LONG).show()
                is DriveEffect.Open -> {
                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(effect.url), effect.mime ?: "*/*")
                    runCatching { context.startActivity(intent) }.onFailure {
                        Toast.makeText(context, "No compatible viewer is installed for this file.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    when {
        data.loading -> LoadingView("Loading Drive…")
        data.error != null -> ErrorView(data.error!!, viewModel::refresh)
        else -> {
            val entries = DriveListing.children(areaEntries, parent, query)
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (projectId == null) "Drive" else "Project documents", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                } }
                if (showProjectBoms) item { TabRow(selectedTabIndex = section) {
                    Tab(selected = section == 0, onClick = { section = 0 }, text = { Text("Files") })
                    Tab(selected = section == 1, onClick = { section = 1 }, text = { Text("Project BOMs (${data.boms.size})") })
                } }
                if (!showProjectBoms || section == 0) {
                    if (projectId == null) item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DriveArea.entries.forEach { choice ->
                                FilterChip(selected = area == choice, onClick = {
                                    area = choice; parent = null; query = ""; selectedFile = null
                                }, label = { Text(choice.label) })
                            }
                        }
                        Text(area.title, style = MaterialTheme.typography.titleSmall)
                        if (area == DriveArea.REVIEWS) Text(
                            "Review folders are not configured in the shared backend yet. Existing files remain available under All.",
                            style = MaterialTheme.typography.bodySmall)
                        if (area == DriveArea.PROJECTS) Text("Internal and client project folders", style = MaterialTheme.typography.bodySmall)
                    }
                    item { Text(current?.name ?: "$department documents", style = MaterialTheme.typography.titleSmall) }
                    if (parent == null) item { Text(if (projectId == null)
                        "Folders mapped to this department and available to your account. Unmapped legacy folders require an ownership review in the web application."
                        else "Files linked to this project in the selected department. Shared reference documents remain in the department Drive.", style = MaterialTheme.typography.bodySmall) }
                    if (parent != null) item { TextButton(onClick = { parent = DriveListing.parentWithinScope(areaEntries, parent); query = "" }) { Text("Up one folder") } }
                    item { OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Filter this folder") }, modifier = Modifier.fillMaxWidth()) }
                    if (entries.isEmpty() && area != DriveArea.REVIEWS) item { Text(if (query.isBlank()) "No visible items in this folder" else "No matching files or folders") }
                    items(entries, key = { it.id }) { entry ->
                        Card(onClick = { if (entry.folder) { parent = entry.id; query = "" } else viewModel.open(entry) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(entry.name, style = MaterialTheme.typography.titleSmall)
                                Text(when {
                                    entry.folder -> "Folder · Open"
                                    !entry.hasSource -> "Document reference · Source file not uploaded"
                                    else -> entry.mimeType ?: "File · Open"
                                }, style = MaterialTheme.typography.labelSmall)
                                if (!entry.folder) {
                                    Text("Version ${entry.currentVersion}${if (entry.locked) " · Locked" else ""}", style = MaterialTheme.typography.labelSmall)
                                    TextButton(onClick = { selectedFile = entry; viewModel.loadHistory(entry) }) { Text("Details & history") }
                                }
                            }
                        }
                    }
                } else {
                    val boms = data.boms.filter { listOf(it.number, it.name, it.revision.orEmpty(), data.projects[it.projectId].orEmpty()).any { value -> value.contains(query, true) } }
                    item { Text("Project BOMs are shown here from their authoritative project records. They are not duplicated as Drive files.", style = MaterialTheme.typography.bodySmall) }
                    data.linkedRecordsError?.let { message -> item { Text(message) } }
                    item { OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Find BOM, revision or project") }, modifier = Modifier.fillMaxWidth()) }
                    if (boms.isEmpty()) item { Text("No project BOMs linked to visible documents in this department") }
                    items(boms, key = { it.id }) { bom ->
                        Card(onClick = { selectedBom = bom }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                            Text(data.projects[bom.projectId] ?: "Authorized project", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(bom.name, style = MaterialTheme.typography.titleSmall)
                            Text(listOf(bom.number, bom.revision?.let { "Rev $it" }, "${bom.lineCount} lines").filterNotNull().joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            bom.sourceFilename?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        } }
                    }
                }
            }
        }
    }
    selectedFile?.let { file ->
        AlertDialog(
            onDismissRequest = { selectedFile = null },
            title = { Text(file.name) },
            confirmButton = { TextButton(onClick = { selectedFile = null }) { Text("Close") } },
            dismissButton = { TextButton(onClick = { viewModel.open(file) }, enabled = file.hasSource) { Text("Open file") } },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Version ${file.currentVersion} · ${if (file.locked) "Locked" else "Unlocked"}")
                        if (!file.hasSource) Text("Source file not uploaded", style = MaterialTheme.typography.bodySmall)
                        file.projectId?.let { Text("Project: ${data.projects[it] ?: "Linked project"}") }
                        file.updatedAt?.let { Text("Updated: $it") }
                        file.checksum?.let { Text("SHA-256: $it", style = MaterialTheme.typography.bodySmall) }
                        Text("File history", style = MaterialTheme.typography.titleSmall)
                    }
                    when {
                        history.nodeId != file.id || history.loading -> item { Text("Loading history…") }
                        history.error != null -> item {
                            Text(history.error!!)
                            TextButton(onClick = { viewModel.loadHistory(file) }) { Text("Retry") }
                        }
                        history.revisions.isEmpty() -> item { Text("No revision records available to this account.") }
                        else -> items(history.revisions, key = { it.id }) { revision ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Version ${revision.version}", style = MaterialTheme.typography.titleSmall)
                                Text(revision.createdAt, style = MaterialTheme.typography.bodySmall)
                                revision.changeSummary?.let { Text(it) }
                                Text("${revision.sizeBytes} bytes", style = MaterialTheme.typography.bodySmall)
                                revision.checksum?.let { Text("SHA-256: $it", style = MaterialTheme.typography.bodySmall) }
                                HorizontalDivider()
                            }
                        }
                    }
                    if (history.nodeId == file.id && !history.loading) {
                        item { Text("Document control", style = MaterialTheme.typography.titleSmall) }
                        when {
                            history.controlError != null -> item {
                                Text(history.controlError!!)
                                TextButton(onClick = { viewModel.loadHistory(file) }) { Text("Retry") }
                            }
                            history.control == null -> item { Text("No controlled-document register is visible for this file.") }
                            else -> {
                                val record = history.control!!
                                item {
                                    Text("${record.number} · ${record.status} · Version ${record.version}")
                                    Text("Source revision: ${record.sourceRevisionId ?: "Not recorded"}", style = MaterialTheme.typography.bodySmall)
                                    record.reviewerDecision?.let { Text("Review: $it") }
                                    record.approvedAt?.let { Text("Approved: $it") }
                                    record.releasedAt?.let { Text("Released: $it") }
                                    record.changeReason?.let { Text("Change reason: $it") }
                                }
                                if (history.events.isEmpty()) item { Text("No event history available to this account.") }
                                items(history.events, key = { "control-${it.id}" }) { event ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(listOfNotNull(event.type, event.toStatus, event.version?.let { "Version $it" }).joinToString(" · "), style = MaterialTheme.typography.titleSmall)
                                        Text(event.createdAt, style = MaterialTheme.typography.bodySmall)
                                        event.note?.let { Text(it) }
                                        event.revisionId?.let { Text("Revision: $it", style = MaterialTheme.typography.bodySmall) }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
    selectedBom?.let { bom -> AlertDialog(
        onDismissRequest = { selectedBom = null },
        confirmButton = { TextButton(onClick = { selectedBom = null }) { Text("Close") } },
        dismissButton = onProjectClick?.let { onClick -> { TextButton(onClick = { selectedBom = null; onClick(bom.projectId) }) { Text("Inspect BOM in Project") } } },
        title = { Text(bom.name) },
        text = { Column {
            Text("${bom.number}${bom.revision?.let { " · Rev $it" } ?: ""}")
            Text("${bom.lineCount} line items")
            Text(bom.sourceFilename?.let { "Source: $it" } ?: "No source spreadsheet is linked for mobile download.")
        } }
    ) }
}
