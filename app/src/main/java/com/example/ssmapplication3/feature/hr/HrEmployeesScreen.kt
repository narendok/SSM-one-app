package com.example.ssmapplication3.feature.hr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.ui.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class HrEmployee(
    val id: String, @SerialName("display_name") val displayName: String? = null,
    @SerialName("employee_code") val employeeCode: String? = null,
    val designation: String? = null, @SerialName("official_email") val email: String? = null,
    @SerialName("employment_status") val status: String, @SerialName("current_work_location") val location: String? = null
)
sealed interface HrEmployeesState { data object Loading: HrEmployeesState; data class Content(val employees: List<HrEmployee>): HrEmployeesState; data class Failure(val message: String): HrEmployeesState }

class HrEmployeesViewModel : ViewModel() {
    private val mutable = MutableStateFlow<HrEmployeesState>(HrEmployeesState.Loading)
    val state = mutable.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        mutable.value = HrEmployeesState.Loading
        runCatching { SupabaseClientProvider.postgrest.from("employees").select(Columns.list("id", "display_name", "employee_code", "designation", "official_email", "employment_status", "current_work_location")).decodeList<HrEmployee>().sortedBy { it.displayName ?: it.employeeCode.orEmpty() } }
            .onSuccess { mutable.value = HrEmployeesState.Content(it) }
            .onFailure { mutable.value = HrEmployeesState.Failure("Unable to load employees. Access is controlled by the backend.") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HrEmployeesScreen(viewModel: HrEmployeesViewModel) {
    val state by viewModel.state.collectAsState(); var query by remember { mutableStateOf("") }; var selected by remember { mutableStateOf<HrEmployee?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("HR employees", fontWeight = FontWeight.Bold) }, actions = { TextButton(onClick = viewModel::refresh) { Text("Refresh") } }) }) { padding ->
        when (val current = state) {
            HrEmployeesState.Loading -> LoadingView("Loading employees…")
            is HrEmployeesState.Failure -> ErrorView(current.message, viewModel::refresh)
            is HrEmployeesState.Content -> {
                val visible = current.employees.filter { query.isBlank() || listOf(it.displayName, it.employeeCode, it.designation, it.email, it.status).filterNotNull().any { value -> value.contains(query, true) } }
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { OutlinedTextField(query, { query = it }, label = { Text("Search employee") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                    item { Text("${visible.size} employees visible to your account", style = MaterialTheme.typography.bodySmall) }
                    items(visible, key = { it.id }) { employee -> Card(onClick = { selected = employee }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(employee.displayName ?: "Unnamed employee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(listOfNotNull(employee.employeeCode, employee.designation, employee.status).joinToString(" · "), style = MaterialTheme.typography.bodySmall); Text(employee.email ?: "No official email", style = MaterialTheme.typography.bodySmall) } } }
                }
                selected?.let { employee -> AlertDialog(onDismissRequest = { selected = null }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }, title = { Text(employee.displayName ?: "Employee", fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("EMPLOYEE ID CARD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text("Code: ${employee.employeeCode ?: "—"}"); Text("Designation: ${employee.designation ?: "—"}"); Text("Email: ${employee.email ?: "—"}"); Text("Status: ${employee.status}"); Text("Location: ${employee.location ?: "—"}"); Text("Role and department changes are managed in Lovable HR Access.", style = MaterialTheme.typography.bodySmall) } }) }
            }
        }
    }
}
