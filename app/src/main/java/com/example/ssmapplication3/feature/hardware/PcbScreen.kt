package com.example.ssmapplication3.feature.hardware

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.sync.SyncCoordinator
import com.example.ssmapplication3.core.ui.EmptyView
import com.example.ssmapplication3.core.ui.ErrorView
import com.example.ssmapplication3.core.ui.LoadingView
import com.example.ssmapplication3.core.util.suspendResult
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PcbTask(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("board_name") val boardName: String,
    val issue: String,
    val priority: String = "medium", // low, medium, high, urgent
    val status: String = "received", // received, in_progress, waiting_parts, repaired, failed, scrapped, returned
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("assignee_id") val assigneeId: String? = null,
    @SerialName("root_cause") val rootCause: String? = null,
    @SerialName("root_cause_notes") val rootCauseNotes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val isOpen: Boolean get() = status in setOf("received", "in_progress", "waiting_parts")
    val statusLabel: String get() = when (status.lowercase()) {
        "received" -> "Received"
        "in_progress" -> "In Progress"
        "waiting_parts" -> "Waiting Parts"
        "repaired" -> "Repaired"
        "failed" -> "Failed"
        "scrapped" -> "Scrapped"
        "returned" -> "Returned"
        else -> status.replace('_', ' ')
    }
}

@Serializable
data class PcbProjectItem(val id: String, val name: String, val code: String? = null)

@Serializable
data class PcbMemberItem(val id: String, val name: String)

sealed interface PcbUiState {
    data object Loading : PcbUiState
    data class Content(
        val tasks: List<PcbTask>,
        val projects: Map<String, String> = emptyMap(),
        val members: Map<String, String> = emptyMap(),
        val actionMessage: String? = null,
        val isSubmitting: Boolean = false
    ) : PcbUiState
    data class Failure(val message: String) : PcbUiState
}

class PcbViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<PcbUiState>(PcbUiState.Loading)
    val state: StateFlow<PcbUiState> = _state.asStateFlow()
    private var scopedProjectId: String? = null

    init {
        refresh()
    }

    fun setScopedProject(projectId: String?) {
        if (scopedProjectId != projectId) {
            scopedProjectId = projectId
            refresh()
        }
    }

    fun clearMessage() {
        val current = _state.value
        if (current is PcbUiState.Content) {
            _state.value = current.copy(actionMessage = null)
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.value = PcbUiState.Loading
        val db = SupabaseClientProvider.postgrest
        val targetProjectId = scopedProjectId

        suspendResult {
            val taskList = db.from("pcb_tasks").select(
                Columns.list("id", "board_name", "issue", "priority", "status", "project_id", "assignee_id", "root_cause", "root_cause_notes", "created_at", "updated_at")
            ) {
                if (!targetProjectId.isNullOrBlank()) {
                    filter { eq("project_id", targetProjectId) }
                }
            }.decodeList<PcbTask>()

            val projectMap = runCatching {
                db.from("projects").select(Columns.list("id", "name", "code"))
                    .decodeList<PcbProjectItem>()
                    .associate { it.id to (it.code?.let { c -> "[$c] " }.orEmpty() + it.name) }
            }.getOrDefault(emptyMap())

            val memberMap = runCatching {
                db.from("rd_members").select(Columns.list("id", "name")) {
                    filter { eq("active", true) }
                }.decodeList<PcbMemberItem>().associate { it.id to it.name }
            }.getOrDefault(emptyMap())

            PcbUiState.Content(
                tasks = taskList.sortedByDescending { it.createdAt ?: "" },
                projects = projectMap,
                members = memberMap
            )
        }.onSuccess { content ->
            _state.value = content
        }.onFailure {
            _state.value = PcbUiState.Failure("Unable to load PCB repair records. Refresh to try again.")
        }
    }

    fun createPcbTask(
        boardName: String,
        issue: String,
        priority: String,
        projectId: String?,
        assigneeId: String?,
        rootCause: String?,
        rootCauseNotes: String?
    ) = viewModelScope.launch {
        val current = _state.value as? PcbUiState.Content ?: return@launch
        _state.value = current.copy(isSubmitting = true)

        val newTask = PcbTask(
            id = UUID.randomUUID().toString(),
            boardName = boardName.trim(),
            issue = issue.trim(),
            priority = priority,
            status = "received",
            projectId = projectId?.takeIf { it.isNotBlank() },
            assigneeId = assigneeId?.takeIf { it.isNotBlank() },
            rootCause = rootCause?.takeIf { it.isNotBlank() },
            rootCauseNotes = rootCauseNotes?.trim()?.takeIf { it.isNotBlank() }
        )

        val db = SupabaseClientProvider.postgrest
        val result = suspendResult {
            db.from("pcb_tasks").insert(newTask)
        }

        result.onSuccess {
            _state.value = current.copy(
                tasks = listOf(newTask) + current.tasks,
                isSubmitting = false,
                actionMessage = "PCB task '${newTask.boardName}' created successfully."
            )
        }.onFailure {
            // Offline fallback
            try {
                val sync = SyncCoordinator.get(getApplication())
                sync.saveLocalDraft("create_pcb_task:${newTask.id}", "${newTask.boardName}|${newTask.issue}")
                _state.value = current.copy(
                    tasks = listOf(newTask) + current.tasks,
                    isSubmitting = false,
                    actionMessage = "PCB task saved locally as draft. Will post when online."
                )
            } catch (_: Exception) {
                _state.value = current.copy(
                    isSubmitting = false,
                    actionMessage = "Failed to save PCB task. Check network connection."
                )
            }
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: String) = viewModelScope.launch {
        val current = _state.value as? PcbUiState.Content ?: return@launch
        val existing = current.tasks.firstOrNull { it.id == taskId } ?: return@launch
        val updated = current.tasks.map { if (it.id == taskId) it.copy(status = newStatus) else it }
        _state.value = current.copy(tasks = updated, isSubmitting = true)

        val db = SupabaseClientProvider.postgrest
        val result = suspendResult {
            db.from("pcb_tasks").update(mapOf("status" to newStatus)) {
                filter { eq("id", taskId) }
            }
        }

        result.onSuccess {
            _state.value = current.copy(
                tasks = updated,
                isSubmitting = false,
                actionMessage = "Board '${existing.boardName}' moved to ${newStatus.replace('_', ' ')}."
            )
        }.onFailure {
            try {
                val sync = SyncCoordinator.get(getApplication())
                sync.saveLocalDraft("update_pcb_status:$taskId", newStatus)
                _state.value = current.copy(
                    tasks = updated,
                    isSubmitting = false,
                    actionMessage = "Status update saved as local draft. Will sync when online."
                )
            } catch (_: Exception) {
                _state.value = current.copy(
                    isSubmitting = false,
                    actionMessage = "Could not update status online."
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcbScreen(
    viewModel: PcbViewModel,
    projectId: String? = null,
    onBack: (() -> Unit)? = null
) {
    LaunchedEffect(projectId) {
        viewModel.setScopedProject(projectId)
    }

    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var activeFilter by rememberSaveable { mutableStateOf("Open") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToUpdate by remember { mutableStateOf<PcbTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PCB Repair & Hardware", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add PCB Task")
            }
        }
    ) { padding ->
        when (val current = state) {
            PcbUiState.Loading -> LoadingView("Loading PCB hardware records…")
            is PcbUiState.Failure -> ErrorView(current.message, viewModel::refresh)
            is PcbUiState.Content -> {
                val openTasks = current.tasks.filter { it.isOpen }
                val completedTasks = current.tasks.filterNot { it.isOpen }
                val filteredByStatus = when (activeFilter) {
                    "Open" -> openTasks
                    "Waiting Parts" -> current.tasks.filter { it.status == "waiting_parts" }
                    "Completed" -> completedTasks
                    else -> current.tasks
                }

                val filtered = filteredByStatus.filter { task ->
                    listOf(
                        task.boardName,
                        task.issue,
                        task.status,
                        task.priority,
                        task.rootCause.orEmpty(),
                        current.projects[task.projectId].orEmpty(),
                        current.members[task.assigneeId].orEmpty()
                    ).any { it.contains(query, ignoreCase = true) }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    current.actionMessage?.let { msg ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    TextButton(onClick = viewModel::clearMessage) { Text("Dismiss") }
                                }
                            }
                        }
                    }

                    // Hardware KPI Summary
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("PCB Repair Pulse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text("${openTasks.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("In Repair", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            val waiting = current.tasks.count { it.status == "waiting_parts" }
                                            Text("$waiting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Waiting Parts", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text("${completedTasks.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Repaired / Done", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search & Filter Tabs
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Filter boards, issues or assignees") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Open", "Waiting Parts", "Completed", "All").forEach { tab ->
                                val count = when (tab) {
                                    "Open" -> openTasks.size
                                    "Waiting Parts" -> current.tasks.count { it.status == "waiting_parts" }
                                    "Completed" -> completedTasks.size
                                    else -> current.tasks.size
                                }
                                FilterChip(
                                    selected = activeFilter == tab,
                                    onClick = { activeFilter = tab },
                                    label = { Text("$tab ($count)") }
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            EmptyView("No PCB tasks found", "No matching hardware board repair tasks visible in this view.")
                        }
                    } else {
                        items(filtered, key = { it.id }) { task ->
                            PcbTaskCard(
                                task = task,
                                projectName = current.projects[task.projectId],
                                assigneeName = current.members[task.assigneeId],
                                onStatusClick = { taskToUpdate = task }
                            )
                        }
                    }
                }

                if (showCreateDialog) {
                    CreatePcbTaskDialog(
                        projects = current.projects,
                        members = current.members,
                        defaultProjectId = projectId,
                        onDismiss = { showCreateDialog = false },
                        onSubmit = { bName, iss, prio, pId, aId, rCause, rNotes ->
                            viewModel.createPcbTask(bName, iss, prio, pId, aId, rCause, rNotes)
                            showCreateDialog = false
                        }
                    )
                }

                taskToUpdate?.let { task ->
                    UpdatePcbStatusDialog(
                        task = task,
                        onDismiss = { taskToUpdate = null },
                        onUpdate = { newStatus ->
                            viewModel.updateTaskStatus(task.id, newStatus)
                            taskToUpdate = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PcbTaskCard(
    task: PcbTask,
    projectName: String?,
    assigneeName: String?,
    onStatusClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.boardName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (task.status.lowercase()) {
                        "repaired" -> MaterialTheme.colorScheme.primaryContainer
                        "waiting_parts" -> MaterialTheme.colorScheme.tertiaryContainer
                        "failed", "scrapped" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = task.statusLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(task.issue, style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority: ${task.priority.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.priority in setOf("high", "urgent")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                projectName?.let {
                    Text("· $it", style = MaterialTheme.typography.labelSmall)
                }
                assigneeName?.let {
                    Text("· $it", style = MaterialTheme.typography.labelSmall)
                }
            }

            task.rootCause?.let { cause ->
                Text(
                    text = "Root Cause: ${cause.replace('_', ' ').capitalizeFirst()}${task.rootCauseNotes?.let { ": $it" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onStatusClick) {
                    Text("Update Status")
                }
            }
        }
    }
}

@Composable
fun CreatePcbTaskDialog(
    projects: Map<String, String>,
    members: Map<String, String>,
    defaultProjectId: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String?, String?, String?, String?) -> Unit
) {
    var boardName by remember { mutableStateOf("") }
    var issue by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var selectedProjectId by remember { mutableStateOf(defaultProjectId ?: "") }
    var selectedAssigneeId by remember { mutableStateOf("") }
    var rootCause by remember { mutableStateOf("") }
    var rootCauseNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New PCB Repair Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = boardName,
                    onValueChange = { boardName = it },
                    label = { Text("Board Name / S/N *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = issue,
                    onValueChange = { issue = it },
                    label = { Text("Issue Description *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("low", "medium", "high", "urgent").forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio.uppercase()) }
                        )
                    }
                }

                if (projects.isNotEmpty()) {
                    Text("Project (Optional)", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedProjectId.isEmpty(),
                            onClick = { selectedProjectId = "" },
                            label = { Text("None") }
                        )
                        projects.forEach { (id, name) ->
                            FilterChip(
                                selected = selectedProjectId == id,
                                onClick = { selectedProjectId = id },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                if (members.isNotEmpty()) {
                    Text("Assignee (Optional)", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedAssigneeId.isEmpty(),
                            onClick = { selectedAssigneeId = "" },
                            label = { Text("Unassigned") }
                        )
                        members.forEach { (id, name) ->
                            FilterChip(
                                selected = selectedAssigneeId == id,
                                onClick = { selectedAssigneeId = id },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                Text("Root Cause (Optional)", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(selected = rootCause.isEmpty(), onClick = { rootCause = "" }, label = { Text("None") })
                    listOf("solder_joint", "component_failure", "esd", "thermal", "design_defect", "other").forEach { cause ->
                        FilterChip(
                            selected = rootCause == cause,
                            onClick = { rootCause = cause },
                            label = { Text(cause.replace('_', ' ').capitalizeFirst()) }
                        )
                    }
                }

                OutlinedTextField(
                    value = rootCauseNotes,
                    onValueChange = { rootCauseNotes = it },
                    label = { Text("Root Cause Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (boardName.isNotBlank() && issue.isNotBlank()) {
                        onSubmit(boardName, issue, priority, selectedProjectId, selectedAssigneeId, rootCause, rootCauseNotes)
                    }
                },
                enabled = boardName.isNotBlank() && issue.isNotBlank()
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UpdatePcbStatusDialog(
    task: PcbTask,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit
) {
    val statuses = listOf(
        "received" to "Received",
        "in_progress" to "In Progress",
        "waiting_parts" to "Waiting on Parts",
        "repaired" to "Repaired",
        "failed" to "Failed",
        "scrapped" to "Scrapped",
        "returned" to "Returned"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Board Status", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Board: ${task.boardName}", style = MaterialTheme.typography.titleSmall)
                Text("Current: ${task.statusLabel}", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                statuses.forEach { (key, label) ->
                    Card(
                        onClick = { onUpdate(key) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (task.status == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (task.status == key) "✓ $label" else label,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (task.status == key) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun String.capitalizeFirst(): String =
    if (isEmpty()) "" else substring(0, 1).uppercase() + substring(1)
