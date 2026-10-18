package com.example.ssmapplication3.feature.myday

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.ssmapplication3.core.ui.*
import com.example.ssmapplication3.core.util.suspendResult
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DashboardTask(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    val department: String? = null,
    @SerialName("department_id") val departmentId: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("assignee_id") val assigneeId: String? = null,
    @SerialName("project_id") val projectId: String? = null
)

@Serializable
data class TaskPerson(
    val id: String,
    val name: String? = null,
    val department: String? = null
)

@Serializable
data class TaskProject(
    val id: String,
    val name: String
)

data class TeamMemberWorkload(
    val person: TaskPerson,
    val openCount: Int,
    val overdueCount: Int
)

object TaskGrouping {
    fun inDepartment(task: DashboardTask, id: String?, name: String, aliases: List<String>): Boolean {
        if (task.departmentId != null) return task.departmentId == id
        val names = (aliases + name).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        return task.department?.trim()?.lowercase() in names
    }
    fun open(task: DashboardTask) = !task.status.equals("done", true)
    fun today(task: DashboardTask, today: LocalDate) =
        task.dueDate?.take(10) == today.toString() ||
        task.priority.equals("urgent", true) ||
        task.priority.equals("high", true) ||
        task.status.equals("blocked", true)
    fun isOverdue(task: DashboardTask, today: LocalDate) =
        task.dueDate?.take(10)?.let { it < today.toString() } == true
    fun isUnassigned(task: DashboardTask) = task.assigneeId.isNullOrBlank()
}

data class DashboardData(
    val tasks: List<DashboardTask> = emptyList(),
    val people: Map<String, TaskPerson> = emptyMap(),
    val projects: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
    val assigning: Boolean = false,
    val message: String? = null
)

class DepartmentDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val mutable = MutableStateFlow(DashboardData())
    val state = mutable.asStateFlow()

    init {
        refresh()
    }

    fun clearMessage() {
        mutable.value = mutable.value.copy(message = null)
    }

    fun refresh() = viewModelScope.launch {
        mutable.value = mutable.value.copy(loading = true, error = null, message = null)
        val db = SupabaseClientProvider.postgrest
        suspendResult {
            db.from("project_tasks").select(
                Columns.list("id", "title", "status", "priority", "department", "department_id", "due_date", "assignee_id", "project_id")
            ).decodeList<DashboardTask>()
        }.onSuccess { tasks ->
            // Optional names must not suppress the task list if their policies deny access.
            val people = suspendResult {
                db.from("rd_members").select(Columns.list("id", "name", "department", "active")) {
                    filter { eq("active", true) }
                }.decodeList<TaskPerson>()
            }.getOrDefault(emptyList())

            val projects = suspendResult {
                db.from("projects").select(Columns.list("id", "name")).decodeList<TaskProject>()
            }.getOrDefault(emptyList())

            mutable.value = DashboardData(
                tasks = tasks.sortedBy { it.dueDate ?: "9999" },
                people = people.associateBy { it.id },
                projects = projects.associate { it.id to it.name },
                loading = false
            )
        }.onFailure {
            mutable.value = DashboardData(
                loading = false,
                error = "Tasks could not be loaded. Refresh to try again."
            )
        }
    }

    fun assignTask(taskId: String, assigneeId: String) = viewModelScope.launch {
        mutable.value = mutable.value.copy(assigning = true, message = null)
        val db = SupabaseClientProvider.postgrest
        val result = suspendResult {
            db.from("project_tasks").update(mapOf("assignee_id" to assigneeId)) {
                filter { eq("id", taskId) }
            }
        }
        val personName = mutable.value.people[assigneeId]?.name ?: "team member"
        result.onSuccess {
            val updated = mutable.value.tasks.map {
                if (it.id == taskId) it.copy(assigneeId = assigneeId) else it
            }
            mutable.value = mutable.value.copy(
                tasks = updated,
                assigning = false,
                message = "Task assigned to $personName"
            )
        }.onFailure {
            // Offline fallback: save local draft so it will be posted when online
            try {
                val sync = SyncCoordinator.get(getApplication())
                sync.saveLocalDraft("assign_task:$taskId", assigneeId)
                val updated = mutable.value.tasks.map {
                    if (it.id == taskId) it.copy(assigneeId = assigneeId) else it
                }
                mutable.value = mutable.value.copy(
                    tasks = updated,
                    assigning = false,
                    message = "Assignment to $personName saved as local draft. Will sync when online."
                )
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    assigning = false,
                    message = "Could not assign task. Check your network connection."
                )
            }
        }
    }
}

@Composable
fun DepartmentDashboard(
    viewModel: DepartmentDashboardViewModel,
    departmentId: String?,
    department: String,
    aliases: List<String>,
    onProject: (String) -> Unit
) {
    val data by viewModel.state.collectAsState()
    var query by rememberSaveable(departmentId, department) { mutableStateOf("") }
    var personDialog by remember { mutableStateOf<TaskPerson?>(null) }
    var selectedTask by remember { mutableStateOf<DashboardTask?>(null) }
    var taskToAssign by remember { mutableStateOf<DashboardTask?>(null) }
    var activeFilter by rememberSaveable { mutableStateOf("All") }
    val today = LocalDate.now()

    when {
        data.loading -> LoadingView("Loading department tasks…")
        data.error != null -> ErrorView(data.error!!, viewModel::refresh)
        else -> {
            val scoped = data.tasks.filter { TaskGrouping.inDepartment(it, departmentId, department, aliases) }
            val open = scoped.filter(TaskGrouping::open)
            val overdue = open.filter { TaskGrouping.isOverdue(it, today) }
            val unassigned = open.filter(TaskGrouping::isUnassigned)
            val dueToday = open.filter { TaskGrouping.today(it, today) }
            val pending = open.filterNot { TaskGrouping.today(it, today) }

            // Team workloads
            val teamMembers = data.people.values.toList().sortedBy { it.name ?: "" }
            val workloads = teamMembers.map { member ->
                val memberTasks = open.filter { it.assigneeId == member.id }
                TeamMemberWorkload(
                    person = member,
                    openCount = memberTasks.size,
                    overdueCount = memberTasks.count { TaskGrouping.isOverdue(it, today) }
                )
            }

            // Filtered tasks by query and active tab
            val baseList = when (activeFilter) {
                "Today" -> dueToday
                "Pending" -> pending
                "Triage" -> unassigned
                "Overdue" -> overdue
                else -> open
            }
            val filtered = baseList.filter { task ->
                listOf(
                    task.title,
                    task.status,
                    task.priority,
                    data.people[task.assigneeId]?.name.orEmpty(),
                    data.projects[task.projectId].orEmpty()
                ).any { it.contains(query, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$department Dashboard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daily management, task triage & team capacity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                    }
                }

                // Status message notification banner
                data.message?.let { msg ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = viewModel::clearMessage) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }

                // Lead Task Pulse Summary Grid
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Task Pulse & Triage Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text("${open.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Open", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text("${dueToday.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Today / High", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (overdue.isNotEmpty()) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text(
                                            "${overdue.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (overdue.isNotEmpty()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text("Overdue", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (unassigned.isNotEmpty()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text(
                                            "${unassigned.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (unassigned.isNotEmpty()) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text("Unassigned", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // Inbound / Unassigned Queue (Lead Triage Section)
                if (unassigned.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Inbound & Unassigned Queue (${unassigned.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Tasks from other departments or unassigned work needing lead delegation",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                unassigned.take(3).forEach { task ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(task.priority.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                    task.dueDate?.let { Text("Due ${it.take(10)}", style = MaterialTheme.typography.labelSmall) }
                                                    task.projectId?.let { id ->
                                                        data.projects[id]?.let { name -> Text("· $name", style = MaterialTheme.typography.labelSmall) }
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { taskToAssign = task },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("Assign", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Team Summary & Capacity Section
                if (workloads.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Team Summary & Workload",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active team member capacity and workload distribution",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    workloads.forEach { wl ->
                                        Card(
                                            onClick = { query = wl.person.name ?: "" },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            modifier = Modifier.widthIn(min = 140.dp)
                                        ) {
                                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = wl.person.name ?: "Member",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${wl.openCount} active tasks",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (wl.overdueCount > 0) {
                                                    Text(
                                                        text = "${wl.overdueCount} overdue",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Department Projects & Deliverables Section
                if (data.projects.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Department Projects (${data.projects.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Active project deliverables, tasks & hardware tracking",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    data.projects.forEach { (projId, projName) ->
                                        val pTasks = open.count { it.projectId == projId }
                                        val pOverdue = overdue.count { it.projectId == projId }
                                        Card(
                                            onClick = { onProject(projId) },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            modifier = Modifier.widthIn(min = 160.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = projName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "$pTasks tasks",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    if (pOverdue > 0) {
                                                        Text(
                                                            text = "· $pOverdue overdue",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Open details →",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter & Search bar
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search tasks, assignee or project") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Today", "Pending", "Triage", "Overdue").forEach { filterName ->
                            FilterChip(
                                selected = activeFilter == filterName,
                                onClick = { activeFilter = filterName },
                                label = {
                                    val count = when (filterName) {
                                        "Today" -> dueToday.size
                                        "Pending" -> pending.size
                                        "Triage" -> unassigned.size
                                        "Overdue" -> overdue.size
                                        else -> open.size
                                    }
                                    Text("$filterName ($count)")
                                }
                            )
                        }
                    }
                }

                // Task Items List
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks found in this view",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { task ->
                        Card(
                            onClick = { selectedTask = task },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Badge(
                                        containerColor = when (task.priority.lowercase()) {
                                            "urgent" -> MaterialTheme.colorScheme.error
                                            "high" -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        }
                                    ) {
                                        Text(task.priority.uppercase(), style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = task.status.replace('_', ' '),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    task.dueDate?.let {
                                        val isPast = it.take(10) < today.toString()
                                        Text(
                                            text = "Due: ${it.take(10)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val assignee = data.people[task.assigneeId]
                                    if (assignee != null) {
                                        TextButton(
                                            onClick = { personDialog = assignee },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "Assigned: ${assignee.name ?: "Member"}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    } else {
                                        TextButton(
                                            onClick = { taskToAssign = task },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "Unassigned · Tap to assign",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    task.projectId?.let { id ->
                                        TextButton(
                                            onClick = { onProject(id) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = data.projects[id] ?: "Open project",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // Person profile dialog
    personDialog?.let { profile ->
        AlertDialog(
            onDismissRequest = { personDialog = null },
            confirmButton = { TextButton(onClick = { personDialog = null }) { Text("Close") } },
            title = { Text(profile.name ?: "Team member") },
            text = { Text(profile.department ?: "Department not provided") }
        )
    }

    // Task details dialog
    selectedTask?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTask = null },
            confirmButton = {
                Row {
                    if (task.assigneeId.isNullOrBlank()) {
                        Button(onClick = {
                            val t = task
                            selectedTask = null
                            taskToAssign = t
                        }) {
                            Text("Assign Task")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(onClick = { selectedTask = null }) { Text("Close") }
                }
            },
            title = { Text(task.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Status: ${task.status.replace('_', ' ')}")
                    Text("Priority: ${task.priority.uppercase()}")
                    Text("Due date: ${task.dueDate ?: "Not scheduled"}")
                    val assignee = data.people[task.assigneeId]
                    Text("Assignee: ${assignee?.name ?: "Unassigned"}")
                }
            }
        )
    }

    // Assign Task Dialog (Lead Triage action)
    taskToAssign?.let { task ->
        var selectedMemberId by remember { mutableStateOf(data.people.keys.firstOrNull().orEmpty()) }
        AlertDialog(
            onDismissRequest = { if (!data.assigning) taskToAssign = null },
            title = { Text("Assign Task to Team Member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select active department member to take responsibility:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (data.assigning) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }

                    val members = data.people.values.toList().sortedBy { it.name ?: "" }
                    if (members.isEmpty()) {
                        Text("No active team members available to assign.")
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            members.forEach { member ->
                                val count = data.tasks.count { it.assigneeId == member.id && TaskGrouping.open(it) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedMemberId == member.id,
                                        onClick = { selectedMemberId = member.id },
                                        enabled = !data.assigning
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = member.name ?: "Member",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "$count active tasks · ${member.department ?: department}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.assignTask(task.id, selectedMemberId)
                        taskToAssign = null
                    },
                    enabled = selectedMemberId.isNotBlank() && !data.assigning
                ) {
                    Text("Confirm Assignment")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { taskToAssign = null },
                    enabled = !data.assigning
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
