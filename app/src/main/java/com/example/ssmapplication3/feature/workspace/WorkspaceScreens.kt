package com.example.ssmapplication3.feature.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ssmapplication3.domain.model.UserProfile
import com.example.ssmapplication3.navigation.Screen
import com.example.ssmapplication3.navigation.WorkspacePolicy
import com.example.ssmapplication3.feature.myday.*

@Composable
fun WorkspaceMoreScreen(profile: UserProfile, workspace: String, onNavigate: (String) -> Unit, onRefresh: () -> Unit, onLogout: () -> Unit) {
    val modules = listOf(Screen.MyWork, Screen.Notifications, Screen.Projects, Screen.HrEmployees, Screen.Procurement,
        Screen.Inventory, Screen.ReceivingDraft, Screen.PurchaseDraftReview, Screen.Assets,
        Screen.MaterialIssue, Screen.MaterialReturns, Screen.StockAdjustment, Screen.StockTransfer)
        .filter { WorkspacePolicy.allows(profile, workspace, it.route) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("More", style = MaterialTheme.typography.titleLarge); Text(profile.fullName ?: profile.email) }
        items(modules, key = { it.route }) { screen ->
            Card(onClick = { onNavigate(screen.route) }, modifier = Modifier.fillMaxWidth()) {
                Text(screen.title, Modifier.padding(16.dp))
            }
        }
        item { TextButton(onClick = onRefresh) { Text("Refresh account access") }; TextButton(onClick = onLogout) { Text("Sign out") } }
    }
}

@Composable
fun WorkspaceSearchScreen(profile: UserProfile, workspace: String, dashboard: DepartmentDashboardViewModel, departmentId: String?, aliases: List<String>, onNavigate: (String) -> Unit) {
    val data by dashboard.state.collectAsState()
    var query by rememberSaveable(workspace) { mutableStateOf("") }
    var onlyOpen by rememberSaveable { mutableStateOf(true) }
    val tasks = data.tasks.filter { TaskGrouping.inDepartment(it, departmentId, workspace, aliases) }
        .filter { !onlyOpen || TaskGrouping.open(it) }
        .filter { task -> listOf(task.title, data.people[task.assigneeId]?.name.orEmpty(), data.projects[task.projectId].orEmpty(), task.status).any { it.contains(query, true) } }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Search & lookup", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(query, { query = it }, label = { Text("Search tasks, people or project names") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { FilterChip(selected = onlyOpen, onClick = { onlyOpen = !onlyOpen }, label = { Text("Open tasks only") }) }
        item {
            if (WorkspacePolicy.allows(profile, workspace, Screen.Scanner.route)) Button(onClick = { onNavigate(Screen.Scanner.route) }) { Text("Scan / look up stock") }
            if (WorkspacePolicy.allows(profile, workspace, Screen.Procurement.route)) TextButton(onClick = { onNavigate(Screen.Procurement.route) }) { Text("Find parts, vendors and orders") }
            TextButton(onClick = { onNavigate(Screen.Drive.route) }) { Text("Search department files") }
        }
        if (data.loading) item { Text("Loading tasks…") }
        data.error?.let { error -> item { Text(error); TextButton(onClick = dashboard::refresh) { Text("Retry") } } }
        if (!data.loading && data.error == null && tasks.isEmpty()) item { Text("No matching tasks") }
        items(tasks, key = { it.id }) { task -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleSmall)
            Text("${task.status} · ${data.people[task.assigneeId]?.name ?: "Unassigned"}", style = MaterialTheme.typography.bodySmall)
            task.projectId?.let { id -> TextButton(onClick = { onNavigate("project/$id") }) { Text(data.projects[id] ?: "Open project") } }
        } } }
    }
}
