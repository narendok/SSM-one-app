package com.example.ssmapplication3.feature.facility

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
data class FacilityAssetRow(
    val id: String,
    @SerialName("asset_code") val assetCode: String,
    val name: String,
    val status: String,
    val manufacturer: String? = null,
    @SerialName("model_number") val modelNumber: String? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
    @SerialName("facility_location_id") val facilityLocationId: String? = null,
    @SerialName("qr_payload") val qrPayload: String? = null
)

@Serializable
data class CalibrationRecordRow(
    val id: String,
    @SerialName("record_code") val recordCode: String,
    @SerialName("instrument_id") val instrumentId: String,
    @SerialName("calibrated_on") val calibratedOn: String,
    @SerialName("due_date") val dueDate: String,
    val result: String,
    @SerialName("provider_name") val providerName: String? = null
)

data class AssetWithCalibration(
    val asset: FacilityAssetRow,
    val calibration: CalibrationRecordRow?
)

sealed interface FacilityAssetsUiState {
    data object Loading : FacilityAssetsUiState
    data class Content(val items: List<AssetWithCalibration>) : FacilityAssetsUiState
    data class Failure(val message: String) : FacilityAssetsUiState
}

class FacilityAssetsViewModel : ViewModel() {
    private val _state = MutableStateFlow<FacilityAssetsUiState>(FacilityAssetsUiState.Loading)
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = FacilityAssetsUiState.Loading
            suspendResult {
                val assets = SupabaseClientProvider.postgrest.from("assets")
                    .select(Columns.list("id", "asset_code", "name", "status", "manufacturer", "model_number", "serial_number", "facility_location_id", "qr_payload"))
                    .decodeList<FacilityAssetRow>()

                val calibrations = runCatching {
                    SupabaseClientProvider.postgrest.from("calibration_records")
                        .select(Columns.list("id", "record_code", "instrument_id", "calibrated_on", "due_date", "result", "provider_name"))
                        .decodeList<CalibrationRecordRow>()
                }.getOrDefault(emptyList())

                val calibrationByInstrument = calibrations.associateBy { it.instrumentId }

                assets.map { asset ->
                    AssetWithCalibration(asset, calibrationByInstrument[asset.id])
                }.sortedBy { it.asset.assetCode }
            }.onSuccess { items ->
                _state.value = FacilityAssetsUiState.Content(items)
            }.onFailure {
                _state.value = FacilityAssetsUiState.Failure("Unable to load facility assets and machinery. Access is controlled by backend RLS.")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilityAssetsScreen(
    viewModel: FacilityAssetsViewModel,
    onScanClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, OPERATIONAL, MAINTENANCE, BREAKDOWN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assets & Machinery", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = viewModel::refresh) { Text("Refresh") }
                    TextButton(onClick = onScanClick) { Text("Scan QR") }
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            FacilityAssetsUiState.Loading -> LoadingView("Loading assets & machinery registers...")
            is FacilityAssetsUiState.Failure -> ErrorView(current.message, viewModel::refresh)
            is FacilityAssetsUiState.Content -> {
                if (current.items.isEmpty()) {
                    EmptyView("No Assets Visible", "No equipment, machinery, or tools are visible to your account under current RLS.")
                } else {
                    val filtered = current.items.filter { item ->
                        val matchesSearch = listOf(
                            item.asset.assetCode,
                            item.asset.name,
                            item.asset.manufacturer.orEmpty(),
                            item.asset.modelNumber.orEmpty(),
                            item.asset.serialNumber.orEmpty()
                        ).any { it.contains(searchQuery.trim(), ignoreCase = true) }

                        val matchesStatus = when (selectedStatusFilter) {
                            "OPERATIONAL" -> item.asset.status.equals("OPERATIONAL", ignoreCase = true)
                            "MAINTENANCE" -> item.asset.status.contains("MAINTENANCE", ignoreCase = true)
                            "BREAKDOWN" -> item.asset.status.equals("BREAKDOWN", ignoreCase = true)
                            else -> true
                        }

                        matchesSearch && matchesStatus
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Summary metrics
                        item {
                            val operationalCount = current.items.count { it.asset.status.equals("OPERATIONAL", true) }
                            val maintenanceCount = current.items.count { it.asset.status.contains("MAINTENANCE", true) }
                            val breakdownCount = current.items.count { it.asset.status.equals("BREAKDOWN", true) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssetMetricCard(Modifier.weight(1f), "Active", "$operationalCount", SsmGreenSuccess)
                                AssetMetricCard(Modifier.weight(1f), "Maintenance", "$maintenanceCount", SsmAmberWarning)
                                AssetMetricCard(Modifier.weight(1f), "Breakdown", "$breakdownCount", SsmRedError)
                            }
                        }

                        // Search box
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Search asset code, machine name or serial") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Filter chips
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedStatusFilter == "ALL",
                                    onClick = { selectedStatusFilter = "ALL" },
                                    label = { Text("All (${current.items.size})") }
                                )
                                FilterChip(
                                    selected = selectedStatusFilter == "OPERATIONAL",
                                    onClick = { selectedStatusFilter = "OPERATIONAL" },
                                    label = { Text("Operational") }
                                )
                                FilterChip(
                                    selected = selectedStatusFilter == "MAINTENANCE",
                                    onClick = { selectedStatusFilter = "MAINTENANCE" },
                                    label = { Text("PM / Maint") }
                                )
                                FilterChip(
                                    selected = selectedStatusFilter == "BREAKDOWN",
                                    onClick = { selectedStatusFilter = "BREAKDOWN" },
                                    label = { Text("Breakdown") }
                                )
                            }
                        }

                        if (filtered.isEmpty()) {
                            item { Text("No assets match the current filter or search criteria.") }
                        } else {
                            items(filtered, key = { it.asset.id }) { item ->
                                AssetCard(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetMetricCard(modifier: Modifier, label: String, count: String, accent: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssetCard(item: AssetWithCalibration) {
    var expanded by rememberSaveable(item.asset.id) { mutableStateOf(false) }

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
                Text(
                    item.asset.assetCode,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                AssetStatusBadge(item.asset.status)
            }

            Text(item.asset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

            listOfNotNull(
                item.asset.manufacturer?.let { "Mfr: $it" },
                item.asset.modelNumber?.let { "Model: $it" },
                item.asset.serialNumber?.let { "S/N: $it" }
            ).joinToString(" · ").let { info ->
                if (info.isNotBlank()) {
                    Text(info, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Calibration tag if instrument
            item.calibration?.let { cal ->
                CalibrationStatusBadge(cal)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (expanded) "Hide details" else "View details")
                }
            }

            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text("Asset ID: ${item.asset.id}", style = MaterialTheme.typography.labelSmall)
                item.asset.facilityLocationId?.let { Text("Location ID: $it", style = MaterialTheme.typography.labelSmall) }
                item.asset.qrPayload?.let { Text("QR Payload: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
                item.calibration?.let { cal ->
                    Text("Calibration Code: ${cal.recordCode}", style = MaterialTheme.typography.labelSmall)
                    Text("Provider: ${cal.providerName ?: "Internal Lab"}", style = MaterialTheme.typography.labelSmall)
                    Text("Calibrated On: ${cal.calibratedOn}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun AssetStatusBadge(status: String) {
    val (label, bg, fg) = when {
        status.equals("OPERATIONAL", ignoreCase = true) || status.equals("ACTIVE", ignoreCase = true) ->
            Triple(status.uppercase(), SsmGreenSuccess.copy(alpha = 0.15f), SsmGreenSuccess)
        status.contains("MAINTENANCE", ignoreCase = true) ->
            Triple("MAINTENANCE", SsmAmberWarning.copy(alpha = 0.15f), SsmAmberWarning)
        status.equals("BREAKDOWN", ignoreCase = true) ->
            Triple("BREAKDOWN", SsmRedError.copy(alpha = 0.15f), SsmRedError)
        else ->
            Triple(status.uppercase(), Color.Gray.copy(alpha = 0.15f), Color.Gray)
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

@Composable
private fun CalibrationStatusBadge(calibration: CalibrationRecordRow) {
    val isPass = calibration.result.equals("PASS", ignoreCase = true)
    val (label, fg) = if (isPass) {
        Pair("Calibrated until ${calibration.dueDate}", SsmCyanInfo)
    } else {
        Pair("Calibration Out-of-Spec: ${calibration.result}", SsmRedError)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(fg.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = fg)
    }
}
