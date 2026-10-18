package com.example.ssmapplication3.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class InventoryComponentRow(val id: String, @SerialName("part_number") val partNumber: String, val name: String, val manufacturer: String? = null, @SerialName("package_case") val packageCase: String? = null)
@Serializable data class LocationRow(@SerialName("component_id") val componentId: String, val label: String, val quantity: Double)
@Serializable data class LotRow(@SerialName("component_id") val componentId: String, @SerialName("lot_number") val lotNumber: String? = null, @SerialName("date_code") val dateCode: String? = null, @SerialName("reel_id") val reelId: String? = null, @SerialName("quality_status") val qualityStatus: String, @SerialName("quantity_available") val quantityAvailable: Double)
@Serializable data class ReservationRow(@SerialName("component_id") val componentId: String, val quantity: Double, val status: String)

data class InventoryItem(val component: InventoryComponentRow, val locations: List<LocationRow>, val lots: List<LotRow>, val reservations: List<ReservationRow>)
sealed interface InventoryUiState { data object Loading : InventoryUiState; data class Content(val items: List<InventoryItem>) : InventoryUiState; data class Failure(val message: String) : InventoryUiState }

class InventoryViewModel : ViewModel() {
    private val _state = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = InventoryUiState.Loading
        runCatching {
            val components = SupabaseClientProvider.postgrest.from("components").select(Columns.list("id", "part_number", "name", "manufacturer", "package_case")).decodeList<InventoryComponentRow>()
            val locations = SupabaseClientProvider.postgrest.from("locations").select(Columns.list("component_id", "label", "quantity")).decodeList<LocationRow>()
            val lots = SupabaseClientProvider.postgrest.from("inventory_lots").select(Columns.list("component_id", "lot_number", "date_code", "reel_id", "quality_status", "quantity_available")).decodeList<LotRow>()
            val reservations = SupabaseClientProvider.postgrest.from("inventory_reservations").select(Columns.list("component_id", "quantity", "status")).decodeList<ReservationRow>()
            components.map { component -> InventoryItem(component, locations.filter { it.componentId == component.id }, lots.filter { it.componentId == component.id }, reservations.filter { it.componentId == component.id }) }
        }.onSuccess { _state.value = InventoryUiState.Content(it) }
            .onFailure { _state.value = InventoryUiState.Failure("Unable to load live inventory. Access is controlled by the backend.") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InventoryScreen(viewModel: InventoryViewModel, onReturnsClick: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Inventory", fontWeight = FontWeight.Bold) }, actions = { TextButton(onClick = { viewModel.refresh() }) { Text("Refresh") }; TextButton(onClick = onReturnsClick) { Text("Returns") } }) }) { padding -> when (val current = state) {
        InventoryUiState.Loading -> LoadingView("Loading live inventory...")
        is InventoryUiState.Failure -> ErrorView(current.message, viewModel::refresh)
        is InventoryUiState.Content -> if (current.items.isEmpty()) EmptyView("No inventory available", "The backend returned no components available to this account.") else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { OutlinedTextField(query, { query = it }, label = { Text("Search part, name or bin") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { Text("Latest fetched stock · Refresh for current balances", style = MaterialTheme.typography.bodySmall) }
            val matches = current.items.filter { item ->
                listOf(item.component.partNumber, item.component.name, item.component.manufacturer.orEmpty(), item.locations.joinToString { it.label }).any { it.contains(query.trim(), ignoreCase = true) }
            }
            if (matches.isEmpty()) item { Text("No matching parts") }
            items(matches, key = { it.component.id }) { item -> InventoryCard(item) }
        }
    } }
}

@Composable private fun InventoryCard(item: InventoryItem) {
    var expanded by rememberSaveable(item.component.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) { 
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.component.partNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(item.component.name, style = MaterialTheme.typography.bodySmall)
            val total = item.locations.sumOf { it.quantity }
            Text("On hand: $total", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) { Text(if (expanded) "Hide details" else "Bins, lots & reservations", style = MaterialTheme.typography.labelSmall) }
            if (expanded) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                Text(listOfNotNull(item.component.manufacturer, item.component.packageCase).joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                if (item.locations.isNotEmpty()) Text("Bins: " + item.locations.joinToString { "${it.label} (${it.quantity})" }, style = MaterialTheme.typography.labelSmall)
                if (item.lots.isNotEmpty()) Text("Lots: " + item.lots.joinToString { listOfNotNull(it.lotNumber, it.dateCode, it.reelId, it.qualityStatus, "qty ${it.quantityAvailable}").joinToString(" • ") }, style = MaterialTheme.typography.labelSmall)
                if (item.reservations.isNotEmpty()) Text("Reservations: " + item.reservations.joinToString { "${it.status} ${it.quantity}" }, style = MaterialTheme.typography.labelSmall)
            }
        } 
    }
}
