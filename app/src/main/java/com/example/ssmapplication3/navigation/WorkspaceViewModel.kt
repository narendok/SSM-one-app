package com.example.ssmapplication3.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.util.suspendResult
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceDepartment(val id: String, val name: String, val aliases: List<String> = emptyList())

class WorkspaceViewModel : ViewModel() {
    private val _departments = MutableStateFlow<List<WorkspaceDepartment>>(emptyList())
    val departments = _departments.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _error.value = null
            suspendResult {
                SupabaseClientProvider.postgrest.from("departments")
                    .select(Columns.list("id", "name", "aliases")) { filter { eq("is_active", true) } }
                    .decodeList<WorkspaceDepartment>().sortedBy { it.name }
            }.onSuccess { _departments.value = it }
                .onFailure { _error.value = "Departments could not be loaded. Try again." }
        }
    }
}
