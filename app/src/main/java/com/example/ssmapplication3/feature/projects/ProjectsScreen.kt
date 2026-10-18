package com.example.ssmapplication3.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.util.suspendResult
import com.example.ssmapplication3.core.ui.EmptyView
import com.example.ssmapplication3.core.ui.ErrorView
import com.example.ssmapplication3.core.ui.LoadingView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Read-only project portfolio. The active backend session and RLS decide visibility. */
@Serializable
data class ProjectRow(
    val id: String,
    val code: String,
    val name: String,
    val revision: String? = null,
    val status: String,
    @SerialName("project_stage") val projectStage: String,
    @SerialName("health_status") val healthStatus: String,
    val priority: String,
    @SerialName("target_sop_date") val targetSopDate: String? = null
)

sealed interface ProjectsUiState {
    data object Loading : ProjectsUiState
    data class Content(val projects: List<ProjectRow>) : ProjectsUiState
    data class Failure(val message: String) : ProjectsUiState
}

class ProjectsViewModel : ViewModel() {
    private val _state = MutableStateFlow<ProjectsUiState>(ProjectsUiState.Loading)
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = ProjectsUiState.Loading
        runCatching {
            SupabaseClientProvider.postgrest.from("projects")
                .select(Columns.list("id", "code", "name", "revision", "status", "project_stage", "health_status", "priority", "target_sop_date"))
                .decodeList<ProjectRow>()
                .sortedBy { it.code }
        }.onSuccess { _state.value = ProjectsUiState.Content(it) }
            .onFailure { _state.value = ProjectsUiState.Failure("Unable to load projects. Access is controlled by the backend.") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(viewModel: ProjectsViewModel, onProjectClick: (String) -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    var query by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Hardware projects", fontWeight = FontWeight.Bold) },
            actions = { TextButton(onClick = viewModel::refresh) { Text("Refresh") } }
        )
    }) { padding ->
        when (val current = state) {
            ProjectsUiState.Loading -> LoadingView("Loading project portfolio…")
            is ProjectsUiState.Failure -> ErrorView(current.message, viewModel::refresh)
            is ProjectsUiState.Content -> {
                if (current.projects.isEmpty()) {
                    EmptyView("No projects visible", "The backend returned no projects visible to this account.")
                } else {
                    val visible = current.projects.filter { project ->
                        query.isBlank() || listOf(project.code, project.name, project.revision.orEmpty(), project.status, project.projectStage, project.healthStatus).any { it.contains(query, ignoreCase = true) }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, label = { Text("Search project, revision or stage") }, modifier = Modifier.fillMaxWidth())
                        }
                        item {
                            Text("${visible.size} of ${current.projects.size} projects visible to this account", style = MaterialTheme.typography.bodySmall)
                        }
                        items(visible, key = { it.id }) { project ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                , onClick = { onProjectClick(project.id) }
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(project.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(project.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(listOf(project.status, project.projectStage, project.healthStatus, project.priority).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                    Text(listOfNotNull(project.revision?.let { "Rev $it" }, project.targetSopDate?.let { "SOP $it" }).joinToString(" · "), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(projectId: String, onDocumentsClick: () -> Unit, onProcurementClick: () -> Unit) {
    val details: ProjectDetailViewModel = viewModel(
        key = "project-details-$projectId",
        factory = viewModelFactory { initializer { ProjectDetailViewModel(projectId) } }
    )
    val state by details.state.collectAsState()
    var selectedTab by rememberSaveable(projectId) { mutableStateOf(0) }
    val project = (state as? ProjectDetailState.Content)?.project
    Scaffold(topBar = {
        TopAppBar(title = { Text(project?.name ?: "Project", fontWeight = FontWeight.Bold) },
            actions = { TextButton(onClick = details::refresh) { Text("Refresh") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ProjectDetailState.Loading -> LoadingView("Loading project details…")
                is ProjectDetailState.Failure -> ErrorView(current.message, details::refresh)
                is ProjectDetailState.Content -> {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Lifecycle") })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("BOM") })
                    }
                    when (selectedTab) {
                        1 -> ProjectLifecyclePanel(projectId)
                        2 -> ProjectBomPanel(projectId)
                        else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(current.project.code, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Text(current.project.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("${current.project.status} · ${current.project.projectStage.replace('_', ' ')} · ${current.project.healthStatus}")
                                    Text("Revision: ${current.project.revision ?: "—"}")
                                    Text("Priority: ${current.project.priority}")
                                    Text("Target SOP: ${current.project.targetSopDate ?: "—"}")
                                }
                            }
                            item {
                                androidx.compose.material3.HorizontalDivider()
                            }
                            item {
                                // Common Project Analysis Section (matches web ProjectAnalysisSummary)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Text("COMMON PROJECT ANALYSIS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            Surface(
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "INTERNAL VIEW",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Visible Blockers
                                        val blockers = current.tasks.filter { it.status.equals("blocked", ignoreCase = true) }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("VISIBLE BLOCKERS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (blockers.isNotEmpty()) {
                                                blockers.forEach { b ->
                                                    Text("• ${b.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                                }
                                            } else {
                                                Text("No blocked tasks visible", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text("Task status does not establish lifecycle gate clearance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                        }

                                        // Next Open Work
                                        val nextWork = current.tasks.filter { !it.status.equals("done", ignoreCase = true) }.minByOrNull { it.dueDate ?: "9999" }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("NEXT OPEN WORK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (nextWork != null) {
                                                Text(nextWork.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text("${nextWork.dueDate ?: "No due date"} · ${nextWork.status.replace('_', ' ')} · ${nextWork.priority}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            } else {
                                                Text("No open tasks visible", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }

                                        // Department Workstreams
                                        val deptGroups = current.tasks.groupBy { it.departmentId }
                                        if (deptGroups.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("DEPARTMENT WORKSTREAMS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                deptGroups.forEach { (deptId, dTasks) ->
                                                    val deptName = deptId?.let { current.departmentMap[it] } ?: "General"
                                                    Text("• $deptName: ${dTasks.size} visible tasks (${dTasks.count { it.status == "done" }} done)", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Button(onClick = { selectedTab = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Inspect Bill of Materials (BOM)") }
                            }
                            item {
                                Button(onClick = { selectedTab = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Explore lifecycle & handovers") }
                            }
                            item {
                                androidx.compose.material3.OutlinedButton(onClick = onDocumentsClick, modifier = Modifier.fillMaxWidth()) { Text("Open engineering files") }
                            }
                            item {
                                androidx.compose.material3.OutlinedButton(onClick = onProcurementClick, modifier = Modifier.fillMaxWidth()) { Text("Open procurement") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Serializable
data class ProjectTaskSummary(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("department_id") val departmentId: String? = null
)

sealed interface ProjectDetailState {
    data object Loading : ProjectDetailState
    data class Content(
        val project: ProjectRow,
        val tasks: List<ProjectTaskSummary> = emptyList(),
        val departmentMap: Map<String, String> = emptyMap()
    ) : ProjectDetailState
    data class Failure(val message: String) : ProjectDetailState
}

class ProjectDetailViewModel(private val projectId: String) : ViewModel() {
    private val mutable = MutableStateFlow<ProjectDetailState>(ProjectDetailState.Loading)
    val state = mutable.asStateFlow()
    private var loadJob: kotlinx.coroutines.Job? = null

    init { refresh() }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutable.value = ProjectDetailState.Loading
            if (projectId.isBlank()) {
                mutable.value = ProjectDetailState.Failure("No project was selected.")
                return@launch
            }
            suspendResult {
                val project = SupabaseClientProvider.postgrest.from("projects")
                    .select(Columns.list("id", "code", "name", "revision", "status", "project_stage", "health_status", "priority", "target_sop_date")) {
                        filter { eq("id", projectId) }
                    }.decodeList<ProjectRow>().singleOrNull()

                val tasks = runCatching {
                    SupabaseClientProvider.postgrest.from("tasks")
                        .select(Columns.list("id", "title", "status", "priority", "due_date", "department_id")) {
                            filter { eq("project_id", projectId) }
                        }.decodeList<ProjectTaskSummary>()
                }.getOrDefault(emptyList())

                val departments = runCatching {
                    SupabaseClientProvider.postgrest.from("departments")
                        .select(Columns.list("id", "name"))
                        .decodeList<com.example.ssmapplication3.navigation.WorkspaceDepartment>()
                        .associate { it.id to it.name }
                }.getOrDefault(emptyMap())

                project?.let { ProjectDetailState.Content(it, tasks, departments) }
            }.onSuccess { content ->
                mutable.value = content
                    ?: ProjectDetailState.Failure("This project is unavailable or is not visible to your account.")
            }.onFailure {
                mutable.value = ProjectDetailState.Failure("Project details could not be loaded. Refresh to try again.")
            }
        }
    }
}
