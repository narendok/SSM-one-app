package com.example.ssmapplication3.feature.procurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.ui.EmptyView
import com.example.ssmapplication3.core.ui.ErrorView
import com.example.ssmapplication3.core.ui.LoadingView
import com.example.ssmapplication3.core.util.suspendResult
import com.example.ssmapplication3.ui.theme.SsmAmberWarning
import com.example.ssmapplication3.ui.theme.SsmCyanInfo
import com.example.ssmapplication3.ui.theme.SsmGreenSuccess
import com.example.ssmapplication3.ui.theme.SsmRedError
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseDraftVoucher(
    val id: String,
    @SerialName("draft_code") val draftCode: String,
    @SerialName("supplier_name") val supplierName: String,
    @SerialName("source_type") val sourceType: String = "INVOICE_IMPORT",
    @SerialName("reference_number") val referenceNumber: String? = null,
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("validation_status") val validationStatus: String = "PENDING_REVIEW",
    @SerialName("created_at") val createdAt: String? = null,
    val notes: String? = null
)

@Serializable
data class PurchaseDraftLine(
    val id: String,
    @SerialName("draft_voucher_id") val voucherId: String,
    val mpn: String,
    val description: String? = null,
    @SerialName("invoice_qty") val invoiceQty: Double = 0.0,
    @SerialName("matched_component_id") val matchedComponentId: String? = null,
    @SerialName("is_duplicate") val isDuplicate: Boolean = false,
    @SerialName("package_condition") val packageCondition: String = "INTACT"
)

sealed interface PurchaseDraftUiState {
    data object Loading : PurchaseDraftUiState
    data class Content(
        val drafts: List<PurchaseDraftVoucher>,
        val selectedDraftId: String?,
        val lines: List<PurchaseDraftLine>,
        val isProcessing: Boolean = false
    ) : PurchaseDraftUiState
    data class Failure(val message: String) : PurchaseDraftUiState
}

class PurchaseDraftReviewViewModel : ViewModel() {
    private val _state = MutableStateFlow<PurchaseDraftUiState>(PurchaseDraftUiState.Loading)
    val state = _state.asStateFlow()

    init {
        loadDrafts()
    }

    fun loadDrafts() {
        viewModelScope.launch {
            _state.value = PurchaseDraftUiState.Loading
            suspendResult {
                // Attempt to load operational drafts from backend operational_purchase_requests / draft queues
                val drafts = runCatching {
                    SupabaseClientProvider.postgrest.from("operational_purchase_requests")
                        .select(Columns.list("id", "pr_number", "status", "created_at"))
                        .decodeList<PurchaseRequestSimple>()
                        .map { pr ->
                            PurchaseDraftVoucher(
                                id = pr.id,
                                draftCode = pr.prNumber,
                                supplierName = "Supplier Review Draft",
                                sourceType = "PO_DRAFT",
                                referenceNumber = pr.prNumber,
                                itemCount = 1,
                                validationStatus = if (pr.status.contains("APPROVED", true)) "VALIDATED" else "PENDING_REVIEW",
                                createdAt = pr.createdAt
                            )
                        }
                }.getOrDefault(emptyList())

                drafts
            }.onSuccess { drafts ->
                val firstId = drafts.firstOrNull()?.id
                _state.value = PurchaseDraftUiState.Content(drafts, firstId, emptyList())
            }.onFailure {
                _state.value = PurchaseDraftUiState.Failure("Unable to load purchase review queue. Backend access controlled.")
            }
        }
    }

    fun selectDraft(id: String) {
        val current = _state.value as? PurchaseDraftUiState.Content ?: return
        _state.value = current.copy(selectedDraftId = id)
    }

    fun approveForReceiving(voucherId: String) {
        val current = _state.value as? PurchaseDraftUiState.Content ?: return
        // Mark locally as validated draft ready for physical receiving inspection
        val updatedDrafts = current.drafts.map {
            if (it.id == voucherId) it.copy(validationStatus = "VERIFIED_FOR_RECEIVING") else it
        }
        _state.value = current.copy(drafts = updatedDrafts)
    }
}

@Serializable
private data class PurchaseRequestSimple(
    val id: String,
    @SerialName("pr_number") val prNumber: String,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDraftReviewScreen(
    viewModel: PurchaseDraftReviewViewModel,
    onNavigateReceiving: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PO / Invoice Import Review", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = viewModel::loadDrafts) { Text("Refresh") }
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            PurchaseDraftUiState.Loading -> LoadingView("Loading import review queue...")
            is PurchaseDraftUiState.Failure -> ErrorView(current.message, viewModel::loadDrafts)
            is PurchaseDraftUiState.Content -> {
                if (current.drafts.isEmpty()) {
                    EmptyView(
                        "No Import Drafts Pending",
                        "All imported supplier invoices, purchase orders, and random manifests have been reviewed. Imported data creates drafts only; inventory stock updates only occur after formal receiving."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Controlled Review Gate", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Importing external POs or supplier invoices creates drafts only. No inventory stock balances are modified until physical receiving and QC approval.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        items(current.drafts, key = { it.id }) { voucher ->
                            DraftVoucherCard(
                                voucher = voucher,
                                isSelected = voucher.id == current.selectedDraftId,
                                onSelect = { viewModel.selectDraft(voucher.id) },
                                onApprove = { viewModel.approveForReceiving(voucher.id) },
                                onReceivingClick = onNavigateReceiving
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftVoucherCard(
    voucher: PurchaseDraftVoucher,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onApprove: () -> Unit,
    onReceivingClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(voucher.draftCode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                DraftStatusBadge(voucher.validationStatus)
            }

            Text("Supplier: ${voucher.supplierName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            voucher.referenceNumber?.let {
                Text("Ref: $it · Source: ${voucher.sourceType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (voucher.validationStatus == "VERIFIED_FOR_RECEIVING") {
                    TextButton(onClick = onReceivingClick) { Text("Proceed to Receiving Draft →") }
                } else {
                    Button(onClick = onApprove) { Text("Verify & Queue for Receiving") }
                }
            }
        }
    }
}

@Composable
private fun DraftStatusBadge(status: String) {
    val (label, bg, fg) = when {
        status.contains("VERIFIED", true) || status.contains("APPROVED", true) ->
            Triple("VERIFIED", SsmGreenSuccess.copy(alpha = 0.15f), SsmGreenSuccess)
        status.contains("DUPLICATE", true) || status.contains("REJECTED", true) ->
            Triple("FLAGGED", SsmRedError.copy(alpha = 0.15f), SsmRedError)
        else ->
            Triple("PENDING REVIEW", SsmAmberWarning.copy(alpha = 0.15f), SsmAmberWarning)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg)
    }
}
