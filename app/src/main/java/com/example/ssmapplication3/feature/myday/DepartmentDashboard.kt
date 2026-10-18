package com.example.ssmapplication3.feature.myday

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.util.suspendResult
import com.example.ssmapplication3.core.ui.*
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
    val id: String, val title: String, val status: String, val priority: String,
    val department: String? = null,
    @SerialName("department_id") val departmentId: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("assignee_id") val assigneeId: String? = null,
    @SerialName("project_id") val projectId: String? = null
)
@Serializable data class TaskPerson(val id: String, val name: String? = null, val department: String? = null)
@Serializable data class TaskProject(val id: String, val name: String)

object TaskGrouping {
    fun inDepartment(task: DashboardTask, id: String?, name: String, aliases: List<String>): Boolean {
        if (task.departmentId != null) return task.departmentId == id
        val names = (aliases + name).map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        return task.department?.trim()?.lowercase() in names
    }
    fun open(task: DashboardTask) = !task.status.equals("done", true)
    fun today(task: DashboardTask, today: LocalDate) = task.dueDate?.take(10) == today.toString()
}

data class DashboardData(
    val tasks: List<DashboardTask> = emptyList(),
    val people: Map<String, TaskPerson> = emptyMap(),
    val projects: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null
)
class DepartmentDashboardViewModel : ViewModel() {
    private val mutable = MutableStateFlow(DashboardData())
    val state = mutable.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        mutable.value = mutable.value.copy(loading = true, error = null)
        val db = SupabaseClientProvider.postgrest
        suspendResult {
            db.from("project_tasks").select(Columns.list("id", "title", "status", "priority", "department", "department_id", "due_date", "assignee_id", "project_id")).decodeList<DashboardTask>()
        }.onSuccess { tasks ->
            // Optional names must not suppress the task list if their policies deny access.
            val people = suspendResult {
                db.from("rd_members").select(Columns.list("id", "name", "department", "active")) {
                    filter { eq("active", true) }
                }.decodeList<TaskPerson>()
            }.getOrDefault(emptyList())
            val projects = suspendResult { db.from("projects").select(Columns.list("id", "name")).decodeList<TaskProject>() }.getOrDefault(emptyList())
            mutable.value = DashboardData(tasks.sortedBy { it.dueDate ?: "9999" }, people.associateBy { it.id }, projects.associate { it.id to it.name }, loading = false)
        }.onFailure { mutable.value = DashboardData(loading = false, error = "Tasks could not be loaded. Refresh to try again.") }
    }
}

@Composable
fun DepartmentDashboard(
    viewModel: DepartmentDashboardViewModel, departmentId: String?, department: String,
    aliases: List<String>, onProject: (String) -> Unit
) {
    val data by viewModel.state.collectAsState()
    var query by rememberSaveable(departmentId, department) { mutableStateOf("") }
    var person by remember { mutableStateOf<TaskPerson?>(null) }
    var selected by remember { mutableStateOf<DashboardTask?>(null) }
    val today = LocalDate.now()
    when {
        data.loading -> LoadingView("Loading department tasks…")
        data.error != null -> ErrorView(data.error!!, viewModel::refresh)
        else -> {
            val scoped = data.tasks.filter { TaskGrouping.inDepartment(it, departmentId, department, aliases) }
            val open = scoped.filter(TaskGrouping::open)
            val filtered = open.filter { task ->
                listOf(task.title, task.status, task.priority, data.people[task.assigneeId]?.name.orEmpty(), data.projects[task.projectId].orEmpty()).any { it.contains(query, true) }
            }
            val dueToday = filtered.filter { TaskGrouping.today(it, today) }
            val pending = filtered.filterNot { TaskGrouping.today(it, today) }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dashboard", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                } }
                item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text("${open.size} open · ${scoped.count { !TaskGrouping.open(it) }} completed")
                    Text("${open.count { it.dueDate?.take(10)?.let { date -> date < today.toString() } == true }} overdue", style = MaterialTheme.typography.bodySmall)
                } } }
                item { OutlinedTextField(query, { query = it }, label = { Text("Search tasks, assignee or project") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TaskChecklist("Today", dueToday, data, Modifier.weight(1f), { selected = it }, { person = it }, onProject)
                    TaskChecklist("Pending", pending, data, Modifier.weight(1f), { selected = it }, { person = it }, onProject)
                } }
                item { Text("Pending includes overdue, upcoming and undated work. Open a task to review its details.", style = MaterialTheme.typography.bodySmall); Spacer(Modifier.height(12.dp)) }
            }
        }
    }
    person?.let { profile -> AlertDialog(onDismissRequest = { person = null }, confirmButton = { TextButton(onClick = { person = null }) { Text("Close") } }, title = { Text(profile.name ?: "Team member") }, text = { Text(profile.department ?: "Department not provided") }) }
    selected?.let { task -> AlertDialog(onDismissRequest = { selected = null }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }, title = { Text(task.title) }, text = { Column {
        Text("${task.status} · ${task.priority}")
        Text("Due: ${task.dueDate ?: "Not scheduled"}")
        Text("Task completion is managed in the existing web workflow.")
    } }) }
}

@Composable
private fun TaskChecklist(title: String, tasks: List<DashboardTask>, data: DashboardData, modifier: Modifier,
                          onTask: (DashboardTask) -> Unit, onPerson: (TaskPerson) -> Unit, onProject: (String) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$title (${tasks.size})", style = MaterialTheme.typography.titleSmall)
        if (tasks.isEmpty()) Text("No tasks", style = MaterialTheme.typography.bodySmall)
        tasks.forEach { task ->
            Card(onClick = { onTask(task) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("☐ ${task.title}", style = MaterialTheme.typography.bodyMedium)
                    Text(task.status.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
                    task.dueDate?.let { Text(it.take(10), style = MaterialTheme.typography.labelSmall) }
                    val assignee = data.people[task.assigneeId]
                    if (assignee != null) TextButton(onClick = { onPerson(assignee) }, contentPadding = PaddingValues(0.dp)) { Text(assignee.name ?: "Assignee", style = MaterialTheme.typography.labelSmall) }
                    else Text(if (task.assigneeId == null) "Unassigned" else "Assignee unavailable", style = MaterialTheme.typography.labelSmall)
                    task.projectId?.let { id -> TextButton(onClick = { onProject(id) }, contentPadding = PaddingValues(0.dp)) { Text(data.projects[id] ?: "Open project", style = MaterialTheme.typography.labelSmall) } }
                }
            }
        }
    }
}
