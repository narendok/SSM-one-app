package com.example.ssmapplication3.feature.myday

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ssmapplication3.core.security.CapabilitiesManager
import com.example.ssmapplication3.core.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MyDayScreen(
    viewModel: MyDayViewModel,
    workspace: String,
    onLogoutClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onProcurementClick: () -> Unit,
    onReceivingClick: () -> Unit,
    onIssueClick: () -> Unit,
    onReturnsClick: () -> Unit,
    onAdjustmentClick: () -> Unit,
    onTransferClick: () -> Unit,
    onScannerClick: () -> Unit,
    onMyWorkClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProjectsClick: () -> Unit,
    onDocumentsClick: () -> Unit,
    onHrEmployeesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(modifier = modifier) { padding ->
        when (val current = state) {
            MyDayUiState.Loading -> LoadingView("Loading workspace…")
            MyDayUiState.Empty -> EmptyView("Workspace", "No workspace is available.")
            is MyDayUiState.Error -> ErrorView(current.message, viewModel::loadMyDayData)
            is MyDayUiState.Success -> {
                val profile = current.userProfile
                var actionSearch by rememberSaveable(profile.id) { mutableStateOf("") }
                val openActions = current.cards.filter { !it.subtitle.contains("DONE", ignoreCase = true) }
                val filteredActions = openActions.filter { actionSearch.isBlank() || listOf(it.title, it.subtitle, it.category, it.priority).any { value -> value.contains(actionSearch, ignoreCase = true) } }
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(profile.fullName ?: profile.email, style = MaterialTheme.typography.titleMedium)
                        Text(profile.roles.joinToString(" · ").ifBlank { "Team member" }, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = viewModel::refreshAccess) { Text("Sync access") }
                            TextButton(onClick = onLogoutClick) { Text("Sign out") }
                        }
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("$workspace workspace", style = MaterialTheme.typography.titleLarge)
                                Text("Quick access to your shared operations", style = MaterialTheme.typography.bodySmall)

                            }
                        }
                    }
                    item { Text("Quick actions", style = MaterialTheme.typography.titleMedium) }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Action summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${openActions.size} open actions · ${openActions.count { it.priority.equals("HIGH", true) || it.priority.equals("URGENT", true) }} high priority", style = MaterialTheme.typography.bodySmall)
                                Text("Tasks are live from the backend; assignee and due-date context stays attached to each action.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { OutlinedTextField(value = actionSearch, onValueChange = { actionSearch = it }, singleLine = true, label = { Text("Search open actions, assignee or reminder") }, modifier = Modifier.fillMaxWidth()) }
                    if (filteredActions.isNotEmpty()) {
                        item { Text("Open actions", style = MaterialTheme.typography.titleMedium) }
                        items(filteredActions, key = { it.id }) { card ->
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(card.title, fontWeight = FontWeight.SemiBold)
                                    Text(card.subtitle, style = MaterialTheme.typography.bodySmall)
                                    Text("${card.category} · ${card.priority}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    val commonActions = listOf(
                        Triple("My work", "Tasks visible to you", onMyWorkClick),
                        Triple("Notifications", "Updates for your account", onNotificationsClick)
                    )
                    val actions = listOf(
                        Triple("Projects", "Project portfolio", onProjectsClick) to "projects",
                        Triple("Engineering files", "Documents, drawings & CAD", onDocumentsClick) to "engineering_documents",
                        Triple("Stock", "Parts, bins & lots", onInventoryClick) to "inventory",
                        Triple("Scan lookup", "Find a part or bin", onScannerClick) to "scanner",
                        Triple("Purchasing", "Orders & suppliers", onProcurementClick) to "procurement",
                        Triple("Receiving", "Capture receipt details", onReceivingClick) to "receiving_draft",
                        Triple("Material issue", "Assign stock", onIssueClick) to "material_issue",
                        Triple("Material return", "Return issued stock", onReturnsClick) to "material_returns",
                        Triple("Stock adjustment", "Reason-required correction", onAdjustmentClick) to "stock_adjustment",
                        Triple("Stock transfer", "Move stock between bins", onTransferClick) to "stock_transfer",
                        Triple("Employees", "Employee ID cards", onHrEmployeesClick) to "hr_employees"
                    )
                    val visibleActions = actions.filter { (_, route) ->
                        com.example.ssmapplication3.navigation.WorkspacePolicy.allows(profile, workspace, route)
                    }.map { it.first } + commonActions
                    visibleActions.chunked(2).forEach { row ->
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                row.forEach { (title, subtitle, action) ->
                                    Card(onClick = action, modifier = Modifier.weight(1f).heightIn(min = 96.dp)) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(title, style = MaterialTheme.typography.titleSmall)
                                            Text(subtitle, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    item {
                        Text("Connected workspace", style = MaterialTheme.typography.titleSmall)
                        Text("Open a module to load records available to your account. Refresh stock before making decisions.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
