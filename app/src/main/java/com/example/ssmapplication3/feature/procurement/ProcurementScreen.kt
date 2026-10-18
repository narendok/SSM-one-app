package com.example.ssmapplication3.feature.procurement

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.sync.SyncCoordinator
import com.example.ssmapplication3.core.ui.EmptyView
import com.example.ssmapplication3.core.ui.ErrorView
import com.example.ssmapplication3.core.ui.LoadingView
import com.example.ssmapplication3.core.util.suspendResult
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

@Serializable
data class VendorRow(
    val id: String,
    val name: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("supplier_status") val supplierStatus: String,
    @SerialName("quality_risk") val qualityRisk: String
)

@Serializable
data class PurchaseRequestRow(
    val id: String,
    @SerialName("request_number") val requestNumber: String,
    val status: String,
    val urgency: String? = null,
    @SerialName("required_date") val requiredDate: String? = null
)

@Serializable
data class RfqRow(
    val id: String,
    @SerialName("rfq_number") val rfqNumber: String,
    val status: String,
    @SerialName("response_due_date") val responseDueDate: String? = null,
    @SerialName("invited_vendor_count") val invitedVendorCount: Int? = null
)

@Serializable
data class PurchaseOrderRow(
    val id: String,
    @SerialName("po_number") val poNumber: String,
    val status: String,
    @SerialName("expected_delivery_date") val expectedDeliveryDate: String? = null,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    val currency: String = "INR",
    @SerialName("vendor_id") val vendorId: String? = null
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

@Serializable
data class QuickExpenseRow(
    val id: String,
    @SerialName("expense_number") val expenseNumber: String,
    val category: String,
    val description: String,
    val amount: Double,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_rd") val isRd: Boolean = false
)

@Serializable
data class PoDraftLine(
    val mpn: String = "",
    val description: String = "",
    val quantity: String = "1",
    val unitCost: String = "0"
)

@Serializable
data class CreatePoLineArg(
    @SerialName("component_id") val componentId: String? = null,
    val mpn: String,
    val description: String? = null,
    @SerialName("quantity_ordered") val quantityOrdered: Double,
    @SerialName("unit_cost") val unitCost: Double
)

@Serializable
data class CreatePoRpcResponse(
    @SerialName("po_id") val poId: String,
    @SerialName("po_number") val poNumber: String,
    @SerialName("total_amount") val totalAmount: Double
)

internal fun receivingItemsForOrders(items: List<PurchaseOrderItemRow>, orderIds: Set<String>) =
    items.filter { it.poId in orderIds }

data class ProcurementItem(val type: String, val number: String, val status: String, val detail: String)

data class SpendSummary(
    val committedSpend: Double = 0.0,
    val quickExpensesSpend: Double = 0.0,
    val openOrdersCount: Int = 0,
    val pendingRequestsCount: Int = 0
)

data class ProcurementContentData(
    val items: List<ProcurementItem> = emptyList(),
    val vendors: List<VendorRow> = emptyList(),
    val spendSummary: SpendSummary = SpendSummary(),
    val message: String? = null
)

sealed interface ProcurementUiState {
    data object Loading : ProcurementUiState
    data class Content(val data: ProcurementContentData) : ProcurementUiState
    data class Failure(val message: String) : ProcurementUiState
}

class ProcurementViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<ProcurementUiState>(ProcurementUiState.Loading)
    val state: StateFlow<ProcurementUiState> = _state.asStateFlow()
    private val syncCoordinator = SyncCoordinator.get(application)

    init {
        refresh()
    }

    fun clearMessage() {
        val current = _state.value
        if (current is ProcurementUiState.Content) {
            _state.value = current.copy(data = current.data.copy(message = null))
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.value = ProcurementUiState.Loading
        runCatching {
            val db = SupabaseClientProvider.postgrest
            val vendors = db.from("operational_vendors")
                .select(Columns.list("id", "name", "is_active", "supplier_status", "quality_risk"))
                .decodeList<VendorRow>()

            val requests = db.from("operational_purchase_requests")
                .select(Columns.list("id", "request_number", "status", "urgency", "required_date"))
                .decodeList<PurchaseRequestRow>()

            val rfqs = db.from("operational_rfqs")
                .select(Columns.list("id", "rfq_number", "status", "response_due_date", "invited_vendor_count"))
                .decodeList<RfqRow>()

            val orders = db.from("purchase_orders")
                .select(Columns.list("id", "po_number", "status", "expected_delivery_date", "total_amount", "tax_amount", "currency", "vendor_id"))
                .decodeList<PurchaseOrderRow>()

            val expenses = suspendResult {
                db.from("quick_expenses")
                    .select(Columns.list("id", "expense_number", "category", "description", "amount", "status", "created_at", "is_rd"))
                    .decodeList<QuickExpenseRow>()
            }.getOrDefault(emptyList())

            val orderIds = orders.map { it.id }.toSet()
            val parts = if (orderIds.isEmpty()) emptyList() else receivingItemsForOrders(
                db.from("operational_receiving_purchase_order_items")
                    .select(Columns.list("id", "po_id", "mpn", "description", "quantity_ordered", "quantity_received")) {
                        filter { isIn("po_id", orderIds.toList()) }
                    }.decodeList<PurchaseOrderItemRow>(), orderIds
            )

            val committedSpend = orders
                .filter { it.status.uppercase() in setOf("DRAFT", "SENT", "PARTIALLY_RECEIVED", "CONFIRMED", "APPROVED") }
                .sumOf { it.totalAmount }
            val quickExpensesSpend = expenses.sumOf { it.amount }
            val openOrdersCount = orders.count { it.status.uppercase() in setOf("DRAFT", "SENT", "PARTIALLY_RECEIVED") }
            val pendingRequestsCount = requests.count { it.status.uppercase() in setOf("DRAFT", "SUBMITTED", "PENDING_APPROVAL") }

            val spendSummary = SpendSummary(
                committedSpend = committedSpend,
                quickExpensesSpend = quickExpensesSpend,
                openOrdersCount = openOrdersCount,
                pendingRequestsCount = pendingRequestsCount
            )

            val items = requests.map {
                ProcurementItem("Purchase request", it.requestNumber, it.status, listOfNotNull(it.urgency, it.requiredDate).joinToString(" • "))
            } + rfqs.map {
                ProcurementItem("RFQ", it.rfqNumber, it.status, listOfNotNull(it.responseDueDate, it.invitedVendorCount?.let { count -> "$count vendors" }).joinToString(" • "))
            } + orders.map {
                ProcurementItem(
                    "Purchase order",
                    it.poNumber,
                    it.status,
                    "₹${String.format("%,.0f", it.totalAmount)} • ${it.expectedDeliveryDate ?: "No expected delivery"}"
                )
            } + expenses.map {
                ProcurementItem(
                    "Expense",
                    it.expenseNumber,
                    it.status.uppercase(),
                    "₹${String.format("%,.0f", it.amount)} • ${it.category} • ${it.description.take(45)}"
                )
            } + parts.map {
                ProcurementItem(
                    "Part",
                    it.mpn ?: "Part number not recorded",
                    "Receiving line",
                    listOfNotNull(it.description, "${it.quantityReceived ?: "Not recorded"}/${it.quantityOrdered ?: "Not recorded"} received").joinToString(" • ")
                )
            } + vendors.map {
                ProcurementItem("Vendor", it.name, if (it.isActive) it.supplierStatus else "INACTIVE", it.qualityRisk)
            }

            ProcurementContentData(
                items = items,
                vendors = vendors,
                spendSummary = spendSummary
            )
        }.onSuccess {
            _state.value = ProcurementUiState.Content(it)
        }.onFailure {
            _state.value = ProcurementUiState.Failure("Unable to load procurement records. Access is controlled by the backend.")
        }
    }

    fun createPurchaseOrder(
        vendorId: String,
        expectedDeliveryDate: String?,
        notes: String?,
        lines: List<PoDraftLine>,
        taxRate: Double = 0.18,
        onSuccess: (String) -> Unit
    ) = viewModelScope.launch {
        val valid = lines.filter { it.mpn.isNotBlank() && (it.quantity.toDoubleOrNull() ?: 0.0) > 0.0 }
        if (valid.isEmpty()) return@launch

        val subtotal = valid.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitCost.toDoubleOrNull() ?: 0.0) }
        val taxAmount = subtotal * taxRate

        val rpcItems = valid.map {
            CreatePoLineArg(
                componentId = null,
                mpn = it.mpn.trim(),
                description = it.description.ifBlank { null },
                quantityOrdered = it.quantity.toDoubleOrNull() ?: 1.0,
                unitCost = it.unitCost.toDoubleOrNull() ?: 0.0
            )
        }

        val db = SupabaseClientProvider.postgrest
        val result = suspendResult {
            db.rpc("create_purchase_order", buildJsonObject {
                put("_vendor_id", vendorId)
                put("_expected_delivery_date", expectedDeliveryDate ?: "")
                put("_notes", notes ?: "")
                put("_tax_amount", taxAmount)
                put("_items", Json.encodeToJsonElement(rpcItems))
            }).decodeAs<CreatePoRpcResponse>()
        }

        result.onSuccess { res ->
            onSuccess(res.poNumber)
            syncCoordinator.saveLocalDraft("create_po_draft", "")
            refresh()
        }.onFailure { err ->
            // Save local draft so user on the go never loses input
            val draftJson = Json.encodeToString(valid)
            syncCoordinator.saveLocalDraft("create_po_draft", draftJson)
            refresh()
        }
    }

    fun submitQuickExpense(
        category: String,
        description: String,
        amount: Double,
        isRd: Boolean,
        onSuccess: (String) -> Unit
    ) = viewModelScope.launch {
        if (category.isBlank() || description.isBlank() || amount <= 0.0) return@launch
        val db = SupabaseClientProvider.postgrest

        val numResult = suspendResult {
            db.rpc("next_business_number", buildJsonObject {
                put("_entity_type", "expense")
                put("_department_code", "FIN")
            }).decodeAs<String>()
        }

        val expenseNumber = numResult.getOrDefault("EXP-${System.currentTimeMillis() % 100000}")
        val insertResult = suspendResult {
            db.from("quick_expenses").insert(
                buildJsonObject {
                    put("expense_number", expenseNumber)
                    put("category", category.trim())
                    put("description", description.trim())
                    put("amount", amount)
                    put("is_rd", isRd)
                    put("status", "pending")
                }
            )
        }

        insertResult.onSuccess {
            onSuccess(expenseNumber)
            refresh()
        }.onFailure {
            val payload = buildJsonObject {
                put("category", category)
                put("description", description)
                put("amount", amount)
                put("is_rd", isRd)
            }.toString()
            syncCoordinator.saveLocalDraft("quick_expense_draft", payload)
            refresh()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcurementScreen(
    viewModel: ProcurementViewModel,
    onReceivingClick: () -> Unit = {},
    onInventoryClick: () -> Unit = {},
    onIssueClick: () -> Unit = {},
    onReviewClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var showCreatePoDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Procurement & Spend", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onReviewClick) { Text("Review Drafts") }
                    TextButton(onClick = onInventoryClick) { Text("Inventory") }
                    TextButton(onClick = onReceivingClick) { Text("Receiving") }
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            ProcurementUiState.Loading -> LoadingView("Loading procurement and expense records...")
            is ProcurementUiState.Failure -> ErrorView(current.message, viewModel::refresh)
            is ProcurementUiState.Content -> {
                val data = current.data
                val visible = data.items.filter { item ->
                    (filter == "All" || item.type == filter) &&
                    (query.isBlank() || listOf(item.type, item.number, item.status, item.detail).any { it.contains(query, ignoreCase = true) })
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Spend & Financial Progress Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Expense & Procurement Spend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text("₹${String.format("%,.0f", data.spendSummary.committedSpend)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Committed PO Spend", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text("₹${String.format("%,.0f", data.spendSummary.quickExpensesSpend)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Quick Expenses", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${data.spendSummary.openOrdersCount} active orders · ${data.spendSummary.pendingRequestsCount} pending requests",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // On-the-Go Management Actions
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { showCreatePoDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New PO")
                            }
                            OutlinedButton(onClick = { showExpenseDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Expense")
                            }
                            OutlinedButton(onClick = onIssueClick) {
                                Text("Assign Parts")
                            }
                            OutlinedButton(onClick = onReceivingClick) {
                                Text("Inwarding (GRN)")
                            }
                        }
                    }

                    // Search & Filter
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            label = { Text("Search part, vendor, PO, request or expense") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Purchase order", "Expense", "Purchase request", "RFQ", "Part", "Vendor").forEach { option ->
                                FilterChip(
                                    selected = filter == option,
                                    onClick = { filter = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }

                    item {
                        Text("${visible.size} matching records", style = MaterialTheme.typography.bodySmall)
                    }

                    if (visible.isEmpty()) {
                        item {
                            EmptyView("No records", "No matching procurement or expense records found.")
                        }
                    } else {
                        items(visible) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text(item.status, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Text(item.number, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(item.detail, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Create PO Dialog
                if (showCreatePoDialog) {
                    CreatePoDialog(
                        vendors = data.vendors,
                        onDismiss = { showCreatePoDialog = false },
                        onSubmit = { vendorId, expected, notes, lines ->
                            viewModel.createPurchaseOrder(
                                vendorId = vendorId,
                                expectedDeliveryDate = expected,
                                notes = notes,
                                lines = lines,
                                onSuccess = {
                                    showCreatePoDialog = false
                                }
                            )
                        }
                    )
                }

                // Quick Expense Dialog
                if (showExpenseDialog) {
                    CreateExpenseDialog(
                        onDismiss = { showExpenseDialog = false },
                        onSubmit = { cat, desc, amt, isRd ->
                            viewModel.submitQuickExpense(
                                category = cat,
                                description = desc,
                                amount = amt,
                                isRd = isRd,
                                onSuccess = {
                                    showExpenseDialog = false
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePoDialog(
    vendors: List<VendorRow>,
    onDismiss: () -> Unit,
    onSubmit: (vendorId: String, expectedDeliveryDate: String?, notes: String?, lines: List<PoDraftLine>) -> Unit
) {
    var selectedVendorId by remember { mutableStateOf(vendors.firstOrNull { it.isActive }?.id.orEmpty()) }
    var expectedDelivery by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var lines by remember {
        mutableStateOf(listOf(PoDraftLine(mpn = "", description = "", quantity = "1", unitCost = "0")))
    }
    val activeVendors = vendors.filter { it.isActive }

    val subtotal = lines.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitCost.toDoubleOrNull() ?: 0.0) }
    val tax = subtotal * 0.18
    val total = subtotal + tax

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Purchase Order") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Vendor", style = MaterialTheme.typography.labelMedium)
                if (activeVendors.isEmpty()) {
                    Text("No active vendors available", style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeVendors.forEach { v ->
                            FilterChip(
                                selected = selectedVendorId == v.id,
                                onClick = { selectedVendorId = v.id },
                                label = { Text(v.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = expectedDelivery,
                    onValueChange = { expectedDelivery = it },
                    label = { Text("Expected delivery (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Order notes / instructions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Order Line Items", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                lines.forEachIndexed { index, line ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Line #${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                if (lines.size > 1) {
                                    IconButton(
                                        onClick = { lines = lines.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = line.mpn,
                                onValueChange = { v -> lines = lines.mapIndexed { i, l -> if (i == index) l.copy(mpn = v) else l } },
                                label = { Text("MPN / Part Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = line.quantity,
                                    onValueChange = { v -> lines = lines.mapIndexed { i, l -> if (i == index) l.copy(quantity = v) else l } },
                                    label = { Text("Qty") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = line.unitCost,
                                    onValueChange = { v -> lines = lines.mapIndexed { i, l -> if (i == index) l.copy(unitCost = v) else l } },
                                    label = { Text("Rate (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        lines = lines + PoDraftLine()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Line")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Subtotal: ₹${String.format("%,.2f", subtotal)}", style = MaterialTheme.typography.bodySmall)
                        Text("GST (18%): ₹${String.format("%,.2f", tax)}", style = MaterialTheme.typography.bodySmall)
                        Text("Grand Total: ₹${String.format("%,.2f", total)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(selectedVendorId, expectedDelivery.ifBlank { null }, notes.ifBlank { null }, lines)
                },
                enabled = selectedVendorId.isNotBlank() && lines.any { it.mpn.isNotBlank() && (it.quantity.toDoubleOrNull() ?: 0.0) > 0.0 }
            ) {
                Text("Submit PO")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateExpenseDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, description: String, amount: Double, isRd: Boolean) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isRd by remember { mutableStateOf(true) }

    val categories = listOf("Prototype material", "Lab consumable", "Tooling", "Logistics", "Office & Ops", "Travel")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Quick Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Category", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) }
                        )
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Custom Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Expense Description") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (INR, tax-inclusive)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isRd, onCheckedChange = { isRd = it })
                    Text("R&D / Project Expense", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(category, description, amt, isRd)
                },
                enabled = category.isNotBlank() && description.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Submit Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
