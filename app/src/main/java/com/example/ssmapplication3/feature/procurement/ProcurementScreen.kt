package com.example.ssmapplication3.feature.procurement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

/** Read-only Stage 5A projection. Deliberately omits commercial and payment fields. */
@Serializable
private data class VendorRow(
    val id: String,
    val name: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("supplier_status") val supplierStatus: String,
    @SerialName("quality_risk") val qualityRisk: String
)

@Serializable
private data class PurchaseRequestRow(
    val id: String,
    @SerialName("request_number") val requestNumber: String,
    val status: String,
    val urgency: String? = null,
    @SerialName("required_date") val requiredDate: String? = null
)

@Serializable
private data class RfqRow(
    val id: String,
    @SerialName("rfq_number") val rfqNumber: String,
    val status: String,
    @SerialName("response_due_date") val responseDueDate: String? = null,
    @SerialName("invited_vendor_count") val invitedVendorCount: Int? = null
)

@Serializable
private data class PurchaseOrderRow(
    val id: String,
    @SerialName("po_number") val poNumber: String,
    val status: String,
    @SerialName("expected_delivery_date") val expectedDeliveryDate: String? = null
)

@Serializable
internal data class PurchaseOrderItemRow(
    val id: String,
    @SerialName("po_id") val poId: String,
    val mpn: String? = null,
    val description: String? = null,
    @SerialName("quantity_ordered") val quantityOrdered: Double? = null,
    @SerialName("quantity_received") val quantityReceived: Double? = null
)

internal fun receivingItemsForOrders(items: List<PurchaseOrderItemRow>, orderIds: Set<String>) =
    items.filter { it.poId in orderIds }

data class ProcurementItem(val type: String, val number: String, val status: String, val detail: String)
sealed interface ProcurementUiState { data object Loading: ProcurementUiState; data class Content(val items: List<ProcurementItem>): ProcurementUiState; data class Failure(val message: String): ProcurementUiState }

class ProcurementViewModel : ViewModel() {
    private val _state = MutableStateFlow<ProcurementUiState>(ProcurementUiState.Loading)
    val state: StateFlow<ProcurementUiState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = ProcurementUiState.Loading
        runCatching {
            val vendors = SupabaseClientProvider.postgrest.from("operational_vendors")
                .select(Columns.list("id", "name", "is_active", "supplier_status", "quality_risk"))
                .decodeList<VendorRow>()
            val requests = SupabaseClientProvider.postgrest.from("operational_purchase_requests")
                .select(Columns.list("id", "request_number", "status", "urgency", "required_date"))
                .decodeList<PurchaseRequestRow>()
            val rfqs = SupabaseClientProvider.postgrest.from("operational_rfqs")
                .select(Columns.list("id", "rfq_number", "status", "response_due_date", "invited_vendor_count"))
                .decodeList<RfqRow>()
            val orders = SupabaseClientProvider.postgrest.from("operational_receiving_purchase_orders")
                .select(Columns.list("id", "po_number", "status", "expected_delivery_date"))
                .decodeList<PurchaseOrderRow>()
            val orderIds = orders.map { it.id }.toSet()
            val parts = if (orderIds.isEmpty()) emptyList() else receivingItemsForOrders(
                SupabaseClientProvider.postgrest.from("operational_receiving_purchase_order_items")
                    .select(Columns.list("id", "po_id", "mpn", "description", "quantity_ordered", "quantity_received")) {
                        filter { isIn("po_id", orderIds.toList()) }
                    }.decodeList<PurchaseOrderItemRow>(), orderIds)
            requests.map { ProcurementItem("Purchase request", it.requestNumber, it.status, listOfNotNull(it.urgency, it.requiredDate).joinToString(" • ")) } +
                rfqs.map { ProcurementItem("RFQ", it.rfqNumber, it.status, listOfNotNull(it.responseDueDate, it.invitedVendorCount?.let { count -> "$count vendors" }).joinToString(" • ")) } +
                orders.map { ProcurementItem("Purchase order", it.poNumber, it.status, it.expectedDeliveryDate ?: "No expected delivery") } +
                parts.map { ProcurementItem("Part", it.mpn ?: "Part number not recorded", "Receiving line", listOfNotNull(it.description,
                    "${it.quantityReceived ?: "Not recorded"}/${it.quantityOrdered ?: "Not recorded"} received").joinToString(" • ")) } +
                vendors.map { ProcurementItem("Vendor", it.name, if (it.isActive) it.supplierStatus else "INACTIVE", it.qualityRisk) }
        }.onSuccess { _state.value = ProcurementUiState.Content(it) }
            .onFailure { _state.value = ProcurementUiState.Failure("Unable to load procurement records. Access is controlled by the backend.") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ProcurementScreen(
    viewModel: ProcurementViewModel,
    onReceivingClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onReviewClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var query by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var filter by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All") }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Procurement", fontWeight = FontWeight.Bold) },
            actions = {
                TextButton(onClick = onReviewClick) { Text("Review Drafts") }
                TextButton(onClick = onInventoryClick) { Text("Inventory") }
                TextButton(onClick = onReceivingClick) { Text("Receiving") }
            }
        )
    }) { padding ->
        when (val current = state) {
            ProcurementUiState.Loading -> LoadingView("Loading procurement records...")
            is ProcurementUiState.Failure -> ErrorView(current.message, viewModel::refresh)
            is ProcurementUiState.Content -> if (current.items.isEmpty()) EmptyView("No procurement records", "The backend returned no records available to this account.") else {
                val visible = current.items.filter { item ->
                    (filter == "All" || item.type == filter) && (query.isBlank() || listOf(item.type, item.number, item.status, item.detail).any { it.contains(query, ignoreCase = true) })
                }
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, label = { Text("Search part, vendor, PO or request") }, modifier = Modifier.fillMaxWidth()) }
                    item { androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "Part", "Vendor", "Purchase order", "Purchase request", "RFQ").forEach { option -> FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) }) } } }
                    item { Text("${visible.size} matching records", style = MaterialTheme.typography.bodySmall) }
                    items(visible) { item -> Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(item.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(item.number, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text("${item.status} • ${item.detail}", style = MaterialTheme.typography.labelSmall) } } }
                }
            }
        }
    }
}
