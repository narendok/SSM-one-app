package com.example.ssmapplication3.feature.receiving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ssmapplication3.core.sync.SyncCoordinator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
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
import io.github.jan.supabase.exceptions.RestException
import com.example.ssmapplication3.core.sync.OnlineTransactionRunner

@Serializable data class ReceivingOrderRow(
    val id: String,
    @SerialName("po_number") val poNumber: String? = null,
    val status: String? = null,
    @SerialName("expected_delivery_date") val expectedDeliveryDate: String? = null,
    @SerialName("vendor_name") val vendorName: String? = null
)

@Serializable private data class ReceivingOrderItemRow(
    val id: String,
    @SerialName("po_id") val poId: String,
    @SerialName("component_id") val componentId: String,
    val mpn: String? = null,
    val description: String? = null,
    @SerialName("quantity_ordered") val quantityOrdered: Double? = null,
    @SerialName("quantity_received") val quantityReceived: Double? = null,
    @SerialName("quantity_rejected") val quantityRejected: Double? = null,
    @SerialName("short_closed") val shortClosed: Boolean? = null
)

data class ReceivingLine(
    val id: String,
    val poId: String,
    val componentId: String,
    val label: String,
    val remaining: Double,
    val shortClosed: Boolean
)

@Serializable data class LineCapture(
    val quantity: String = "",
    val lot: String = "",
    val batch: String = "",
    val dateCode: String = "",
    val reel: String = "",
    val serial: String = "",
    val mslEsd: String = "",
    val packageCondition: String = ""
)

sealed interface ReceivingUiState {
    data object Loading : ReceivingUiState
    data class Content(val orders: List<ReceivingOrderRow>, val lines: List<ReceivingLine>, val selectedOrderId: String?) : ReceivingUiState
    data class Failure(val message: String) : ReceivingUiState
}

class ReceivingDraftViewModel(
    private val drafts: SyncCoordinator,
    private val owner: String,
    private val finalGrn: FinalGrnRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ReceivingUiState>(ReceivingUiState.Loading)
    val state: StateFlow<ReceivingUiState> = _state.asStateFlow()
    private val captures = mutableStateMapOf<String, LineCapture>()
    private val draftAction = "receiving:$owner"
    private var restored = false
    private val _saveStatus = MutableStateFlow("Edits stay on this device. Tap Save draft to keep them.")
    val saveStatus = _saveStatus.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _submission = MutableStateFlow(GrnSubmissionState())
    val submission = _submission.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = ReceivingUiState.Loading
        runCatching {
            check(SupabaseClientProvider.auth.currentUserOrNull()?.id == owner)
            if (!restored) {
                drafts.loadLocalDraft(draftAction)?.let { captures.putAll(Json.decodeFromString<Map<String, LineCapture>>(it)) }
                restored = true
            }
            val orders = SupabaseClientProvider.postgrest.from("operational_receiving_purchase_orders")
                .select(Columns.list("id", "po_number", "status", "expected_delivery_date", "vendor_name"))
                .decodeList<ReceivingOrderRow>()
            val items = SupabaseClientProvider.postgrest.from("operational_receiving_purchase_order_items")
                .select(Columns.list("id", "po_id", "component_id", "mpn", "description", "quantity_ordered", "quantity_received", "quantity_rejected", "short_closed"))
                .decodeList<ReceivingOrderItemRow>()
            val lines = items.map { item ->
                ReceivingLine(item.id, item.poId, item.componentId, item.mpn ?: item.description ?: "Unlabelled PO item",
                    ReceivingDraftRules.remainingQuantity(item.quantityOrdered, item.quantityReceived, item.quantityRejected), item.shortClosed == true)
            }
            ReceivingUiState.Content(orders, lines, orders.firstOrNull()?.id)
        }.onSuccess { _state.value = it }
            .onFailure { if (it is CancellationException) throw it; _state.value = ReceivingUiState.Failure("Unable to load receiving records. Access is controlled by the backend.") }
    }

    fun selectOrder(id: String) { (state.value as? ReceivingUiState.Content)?.let { _state.value = it.copy(selectedOrderId = id) } }
    fun capture(lineId: String, update: (LineCapture) -> LineCapture) { captures[lineId] = update(captures[lineId] ?: LineCapture()); _saveStatus.value = "Unsaved changes" }
    fun captureFor(lineId: String): LineCapture = captures[lineId] ?: LineCapture()
    fun saveDraft() {
        if (_saving.value || !restored) return
        val snapshot = captures.toMap()
        _saving.value = true
        viewModelScope.launch {
            try {
                check(SupabaseClientProvider.auth.currentUserOrNull()?.id == owner)
                drafts.saveLocalDraft(draftAction, Json.encodeToString(snapshot))
                _saveStatus.value = if (snapshot == captures.toMap()) "Saved on this device. Stock has not changed." else "Earlier edits saved. Save your latest changes too."
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { _saveStatus.value = "Draft could not be saved. Keep this screen open and retry." }
            finally { _saving.value = false }
        }
    }

    fun grnPayload(invoiceNumber: String, invoiceDate: String, storageNotes: String): String? =
        (state.value as? ReceivingUiState.Content)?.let { content ->
            val selected = content.selectedOrderId ?: return null
            runCatching {
                FinalGrnRequests.payload(
                    selected, invoiceNumber, invoiceDate, storageNotes,
                    content.lines.filter { it.poId == selected }.map { it to captureFor(it.id) }
                ).toString()
            }.getOrNull()
        }

    fun submitFinalGrn(payload: String) {
        if (_submission.value.posting) return
        _submission.value = _submission.value.copy(posting = true, message = null)
        viewModelScope.launch {
            try {
                val number = finalGrn.post(payload)
                _submission.value = GrnSubmissionState(message = "Posted successfully: $number")
                refresh()
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                val held = try { finalGrn.pending() } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { _submission.value.pending }
                _submission.value = GrnSubmissionState(
                    pending = held,
                    message = when {
                        failure is OnlineTransactionRunner.DefinitiveRejection -> "Not posted: ${failure.message}. Refresh and correct the receipt."
                        held != null && failure is RestException -> "Posting is unconfirmed. Server response (${failure.statusCode}): ${failure.error.take(180)}"
                        held != null -> "Posting is unconfirmed. Retry the saved receipt; do not create another GRN."
                        else -> "Could not confirm submission. Refresh before trying again."
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReceivingDraftScreen(viewModel: ReceivingDraftViewModel) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Receiving draft", fontWeight = FontWeight.Bold) }) }) { padding ->
        when (val current = state) {
            ReceivingUiState.Loading -> LoadingView("Loading receiving purchase orders...")
            is ReceivingUiState.Failure -> ErrorView(current.message, viewModel::refresh)
            is ReceivingUiState.Content -> ReceivingContent(current, viewModel, Modifier.padding(padding))
        }
    }
}

@Composable private fun ReceivingContent(state: ReceivingUiState.Content, viewModel: ReceivingDraftViewModel, modifier: Modifier) {
    val saveStatus by viewModel.saveStatus.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val submission by viewModel.submission.collectAsState()
    var invoiceNumber by rememberSaveable { mutableStateOf("") }
    var invoiceDate by rememberSaveable { mutableStateOf("") }
    var storageNotes by rememberSaveable { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<String?>(null) }
    if (state.orders.isEmpty()) { EmptyView("No purchase orders available", "The backend returned no receiving purchase orders available to this account."); return }
    val selected = state.selectedOrderId ?: return
    val lines = state.lines.filter { it.poId == selected }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Button(onClick = viewModel::saveDraft, enabled = !saving) { Text(if (saving) "Saving…" else "Save draft") }
            Text(saveStatus, style = MaterialTheme.typography.bodySmall)
            Text("Select purchase order", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.orders.forEach { order -> Button(onClick = { viewModel.selectOrder(order.id) }) { Text(order.poNumber ?: "Purchase order") } }
            }
            val order = state.orders.first { it.id == selected }
            Text(listOfNotNull(order.status, order.vendorName, order.expectedDeliveryDate).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
            Text("Save draft keeps edits on this device. Final posting is online-only and server-controlled.", style = MaterialTheme.typography.bodySmall)
        }
        if (submission.pending != null) item {
            Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Saved GRN awaiting confirmation", style = MaterialTheme.typography.titleSmall)
                Text("Retry uses the original request key and receipt values.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { viewModel.submitFinalGrn(submission.pending!!.payload) }, enabled = !submission.posting) { Text("Retry saved GRN") }
            } }
        }
        if (lines.isEmpty()) item { EmptyView("No receivable lines", "No receiving line items are available for this purchase order.") }
        items(lines, key = { it.id }) { line -> ReceivingLineCard(line, viewModel) }
        if (submission.pending == null) item {
            Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Final GRN", style = MaterialTheme.typography.titleMedium)
                Text("This posts the received quantities above through the protected server transaction.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(invoiceNumber, { invoiceNumber = it }, label = { Text("Vendor invoice number") }, singleLine = true, enabled = !submission.posting, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(invoiceDate, { invoiceDate = it }, label = { Text("Vendor invoice date (YYYY-MM-DD)") }, singleLine = true, enabled = !submission.posting, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(storageNotes, { storageNotes = it }, label = { Text("Storage notes") }, enabled = !submission.posting, modifier = Modifier.fillMaxWidth())
                submission.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                val payload = viewModel.grnPayload(invoiceNumber, invoiceDate, storageNotes)
                Button(onClick = { confirmation = payload }, enabled = payload != null && !submission.posting, modifier = Modifier.fillMaxWidth()) { Text(if (submission.posting) "Posting…" else "Review final GRN") }
            } }
        }
        item { Text("Evidence upload is unavailable until a signed mobile upload contract is provided by the backend.", style = MaterialTheme.typography.bodySmall) }
    }
    confirmation?.let { request ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Confirm final GRN") },
            text = { Text("This posts the entered receipt quantities, creates the server GRN number, updates stock in quality hold, and creates the required audit records.") },
            confirmButton = { TextButton(onClick = { confirmation = null; viewModel.submitFinalGrn(request) }) { Text("Post now") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } }
        )
    }
}

@Composable private fun ReceivingLineCard(line: ReceivingLine, viewModel: ReceivingDraftViewModel) {
    val capture = viewModel.captureFor(line.id)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) { 
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(line.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (line.shortClosed) "Short closed" else "Remaining: ${line.remaining}",
                style = MaterialTheme.typography.labelSmall,
                color = if (line.shortClosed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = capture.quantity,
                    onValueChange = { value -> viewModel.capture(line.id) { it.copy(quantity = value) } },
                    label = { Text("Recv Qty", style = MaterialTheme.typography.labelSmall) },
                    isError = capture.quantity.isNotBlank() && !ReceivingDraftRules.receiptQuantityIsValid(capture.quantity, line.remaining),
                    modifier = Modifier.weight(1f),
                    enabled = !line.shortClosed,
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
                OutlinedTextField(
                    value = capture.lot,
                    onValueChange = { value -> viewModel.capture(line.id) { it.copy(lot = value) } },
                    label = { Text("Lot", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = capture.dateCode,
                    onValueChange = { value -> viewModel.capture(line.id) { it.copy(dateCode = value) } },
                    label = { Text("Date code", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
                OutlinedTextField(
                    value = capture.reel,
                    onValueChange = { value -> viewModel.capture(line.id) { it.copy(reel = value) } },
                    label = { Text("Reel", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
            }
        } 
    }
}
