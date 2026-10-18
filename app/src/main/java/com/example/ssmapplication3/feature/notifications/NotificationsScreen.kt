package com.example.ssmapplication3.feature.notifications

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
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable data class MobileNotification(
    val id: String, val title: String, val body: String? = null, val category: String,
    @SerialName("created_at") val createdAt: String, @SerialName("is_read") val isRead: Boolean,
    @SerialName("target_url") val targetUrl: String? = null
)
@Serializable data class NotificationInboxState(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("unread_count") val unreadCount: Int = 0
)
sealed interface NotificationsState { data object Loading : NotificationsState; data class Content(val rows: List<MobileNotification>) : NotificationsState; data class Failure(val message: String) : NotificationsState }

class NotificationsViewModel : ViewModel() {
    private val mutable = MutableStateFlow<NotificationsState>(NotificationsState.Loading)
    val state = mutable.asStateFlow()
    private val mutableUnreadCount = MutableStateFlow(0)
    val unreadCount = mutableUnreadCount.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        mutable.value = NotificationsState.Loading
        try {
            val rows = SupabaseClientProvider.postgrest.from("notifications")
                .select(Columns.list("id", "title", "body", "category", "created_at", "is_read", "target_url"))
                .decodeList<MobileNotification>()
            mutableUnreadCount.value = SupabaseClientProvider.postgrest
                .rpc("get_notification_inbox_state").decodeAs<NotificationInboxState>().unreadCount
            mutable.value = NotificationsState.Content(rows.sortedByDescending { it.createdAt })
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { mutable.value = NotificationsState.Failure("Unable to load notifications visible to this account. Access is controlled by the backend.") }
    }
    fun markRead(id: String) = viewModelScope.launch {
        try {
            SupabaseClientProvider.postgrest.rpc("mark_notification_read", buildJsonObject { put("_notification_id", id) })
            refresh()
        } catch (_: Exception) { refresh() }
    }
    fun markAllRead() = viewModelScope.launch {
        try { SupabaseClientProvider.postgrest.rpc("mark_all_notifications_read"); refresh() }
        catch (_: Exception) { refresh() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NotificationsScreen(viewModel: NotificationsViewModel) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Notifications") }, actions = {
        TextButton(onClick = viewModel::markAllRead) { Text("Mark all read") }
        TextButton(onClick = viewModel::refresh) { Text("Refresh") }
    }) }) { padding -> when (val current = state) {
        NotificationsState.Loading -> LoadingView("Loading notifications…")
        is NotificationsState.Failure -> ErrorView(current.message, viewModel::refresh)
        is NotificationsState.Content -> if (current.rows.isEmpty()) EmptyView("No notifications", "The backend returned no notifications visible to this account.") else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Updates are delivered by the authorized backend inbox.", style = MaterialTheme.typography.bodySmall) }
            items(current.rows, key = { it.id }) { row -> Card(onClick = { if (!row.isRead) viewModel.markRead(row.id) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (row.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.title, fontWeight = FontWeight.SemiBold); Text(listOf(row.category, row.createdAt).joinToString(" · "), style = MaterialTheme.typography.bodySmall); row.body?.takeIf { it.isNotBlank() }?.let { Text(it) }
            } } }
        }
    } }
}
