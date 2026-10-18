package com.example.ssmapplication3.feature.mywork

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.ui.EmptyView
import com.example.ssmapplication3.core.ui.ErrorView
import com.example.ssmapplication3.core.ui.LoadingView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable data class WorkTask(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    @SerialName("due_date") val dueDate: String? = null,
    val description: String? = null,
    val department: String? = null
)
sealed interface MyWorkState { data object Loading : MyWorkState; data class Content(val tasks: List<WorkTask>) : MyWorkState; data class Failure(val message: String) : MyWorkState }

class MyWorkViewModel : ViewModel() {
    private val mutable = MutableStateFlow<MyWorkState>(MyWorkState.Loading)
    val state = mutable.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        mutable.value = MyWorkState.Loading
        try {
            val tasks = SupabaseClientProvider.postgrest.from("project_tasks")
                .select(Columns.list("id", "title", "status", "priority", "due_date", "description", "department"))
                .decodeList<WorkTask>()
            mutable.value = MyWorkState.Content(tasks.sortedWith(compareBy<WorkTask> { it.dueDate ?: "9999-12-31" }.thenBy { it.title }))
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { mutable.value = MyWorkState.Failure("Unable to load work visible to this account. Access is controlled by the backend.") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MyWorkScreen(viewModel: MyWorkViewModel) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("My work") }, actions = { TextButton(onClick = viewModel::refresh) { Text("Refresh") } }) }) { padding ->
        when (val current = state) {
            MyWorkState.Loading -> LoadingView("Loading work…")
            is MyWorkState.Failure -> ErrorView(current.message, viewModel::refresh)
            is MyWorkState.Content -> if (current.tasks.isEmpty()) EmptyView("No work available", "The backend returned no tasks visible to this account.") else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Tasks visible to your signed-in account. Status changes remain controlled by the backend.", style = MaterialTheme.typography.bodySmall) }
                items(current.tasks, key = { it.id }) { task -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(task.title, fontWeight = FontWeight.SemiBold)
                    Text(listOf(task.status, task.priority, task.department, task.dueDate?.let { "Due $it" }).filterNotNull().joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    task.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                } } }
            }
        }
    }
}
