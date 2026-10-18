package com.example.ssmapplication3.feature.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.auth.SessionManager
import com.example.ssmapplication3.core.auth.SessionState
import com.example.ssmapplication3.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class MyDayCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // TASK, APPROVAL, ALERT, CHECKLIST
    val priority: String = "NORMAL"
)

sealed class MyDayUiState {
    object Loading : MyDayUiState()
    data class Success(
        val userProfile: UserProfile,
        val cards: List<MyDayCard>
    ) : MyDayUiState()
    object Empty : MyDayUiState()
    data class Error(val message: String) : MyDayUiState()
}

class MyDayViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyDayUiState>(MyDayUiState.Loading)
    val uiState: StateFlow<MyDayUiState> = _uiState.asStateFlow()

    init {
        loadMyDayData()
    }

    fun loadMyDayData() {
        viewModelScope.launch {
            _uiState.value = MyDayUiState.Loading
            when (val state = sessionManager.sessionState.value) {
                is SessionState.Authenticated -> {
                    val cards = runCatching {
                        SupabaseClientProvider.postgrest.from("project_tasks")
                            .select(Columns.list("id", "title", "status", "priority", "due_date", "assignee_id", "department"))
                            .decodeList<MyDayTaskRow>()
                            .map { task -> MyDayCard(task.id, task.title, listOf(task.status, task.department ?: "General", task.assigneeId?.let { "Assignee $it" }, task.dueDate?.let { "Due $it" }).filterNotNull().joinToString(" · "), "TASK", task.priority) }
                    }.getOrDefault(emptyList())
                    _uiState.value = MyDayUiState.Success(
                        userProfile = state.userProfile,
                        cards = cards
                    )
                }
                else -> {
                    _uiState.value = MyDayUiState.Error("User is not authenticated")
                }
            }
        }
    }

    fun refreshAccess() {
        viewModelScope.launch {
            _uiState.value = MyDayUiState.Loading
            sessionManager.refreshIdentity()
            loadMyDayData()
        }
    }
}

@Serializable
private data class MyDayTaskRow(
    val id: String,
    val title: String,
    val status: String,
    val priority: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("assignee_id") val assigneeId: String? = null,
    val department: String? = null
)

