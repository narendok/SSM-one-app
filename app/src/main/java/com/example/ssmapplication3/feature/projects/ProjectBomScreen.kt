package com.example.ssmapplication3.feature.projects

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ssmapplication3.core.network.SupabaseClientProvider
import com.example.ssmapplication3.core.ui.EmptyView
import com.example.ssmapplication3.core.ui.ErrorView
import com.example.ssmapplication3.core.ui.LoadingView
import com.example.ssmapplication3.core.util.suspendResult
import com.example.ssmapplication3.ui.theme.SsmAmberWarning
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
data class ProjectBomRow(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("bom_number") val bomNumber: String,
    val name: String,
    val revision: String? = null,
    @SerialName("line_count") val lineCount: Int = 0,
    @SerialName("source_filename") val sourceFilename: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val isReleased: Boolean
        get() = !revision.isNullOrBlank() && !revision.equals("draft", ignoreCase = true) && !revision.equals("tentative", ignoreCase = true)
}

@Serializable
data class ProjectBomItemDetail(
    val id: String,
    @SerialName("bom_id") val bomId: String,
    @SerialName("line_index") val lineIndex: Int = 0,
    val mpn: String? = null,
    val description: String? = null,
    val manufacturer: String? = null,
    val footprint: String? = null,
    val value: String? = null,
    val refs: String? = null,
    val quantity: Double = 0.0,
    val shortage: Double = 0.0,
    @SerialName("match_status") val matchStatus: String? = null,
    @SerialName("matched_component_id") val matchedComponentId: String? = null,
    val remark: String? = null
)

sealed interface ProjectBomUiState {
    data object Loading : ProjectBomUiState
    data class Content(
        val boms: List<ProjectBomRow>,
        val selectedBomId: String?,
        val items: List<ProjectBomItemDetail>,
        val loadingItems: Boolean = false
    ) : ProjectBomUiState
    data class Failure(val message: String) : ProjectBomUiState
}

class ProjectBomViewModel(private val projectId: String) : ViewModel() {
    private val _state = MutableStateFlow<ProjectBomUiState>(ProjectBomUiState.Loading)
    val state = _state.asStateFlow()

    init {
        loadBoms()
    }

    fun loadBoms() {
        viewModelScope.launch {
            _state.value = ProjectBomUiState.Loading
            suspendResult {
                SupabaseClientProvider.postgrest.from("project_boms")
                    .select(Columns.list("id", "project_id", "bom_number", "name", "revision", "line_count", "source_filename", "notes", "created_at")) {
                        filter { eq("project_id", projectId) }
                    }
                    .decodeList<ProjectBomRow>()
                    .sortedByDescending { it.createdAt }
            }.onSuccess { boms ->
                if (boms.isEmpty()) {
                    _state.value = ProjectBomUiState.Content(emptyList(), null, emptyList())
                } else {
                    val firstBom = boms.first()
                    _state.value = ProjectBomUiState.Content(boms, firstBom.id, emptyList(), loadingItems = true)
                    loadItems(firstBom.id, boms)
                }
            }.onFailure {
                _state.value = ProjectBomUiState.Failure("Unable to load project BOMs. Verify backend connection.")
            }
        }
    }

    fun selectBom(bomId: String) {
        val current = _state.value as? ProjectBomUiState.Content ?: return
        if (current.selectedBomId == bomId) return
        _state.value = current.copy(selectedBomId = bomId, loadingItems = true)
        loadItems(bomId, current.boms)
    }

    private fun loadItems(bomId: String, boms: List<ProjectBomRow>) {
        viewModelScope.launch {
            suspendResult {
                SupabaseClientProvider.postgrest.from("project_bom_items")
                    .select(Columns.list("id", "bom_id", "line_index", "mpn", "description", "manufacturer", "footprint", "value", "refs", "quantity", "shortage", "match_status", "matched_component_id", "remark")) {
                        filter { eq("bom_id", bomId) }
                    }
                    .decodeList<ProjectBomItemDetail>()
                    .sortedBy { it.lineIndex }
            }.onSuccess { items ->
                val current = _state.value as? ProjectBomUiState.Content
                if (current != null && current.selectedBomId == bomId) {
                    _state.value = current.copy(items = items, loadingItems = false)
                }
            }.onFailure {
                val current = _state.value as? ProjectBomUiState.Content
                if (current != null && current.selectedBomId == bomId) {
                    _state.value = current.copy(items = emptyList(), loadingItems = false)
                }
            }
        }
    }
}

@Composable
fun ProjectBomPanel(projectId: String) {
    val viewModel: ProjectBomViewModel = viewModel(
        key = "project-bom-$projectId",
        factory = viewModelFactory { initializer { ProjectBomViewModel(projectId) } }
    )
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        ProjectBomUiState.Loading -> LoadingView("Loading Bill of Materials...")
        is ProjectBomUiState.Failure -> ErrorView(current.message, viewModel::loadBoms)
        is ProjectBomUiState.Content -> {
            if (current.boms.isEmpty()) {
                EmptyView("No BOM Available", "No Bill of Materials has been registered or linked to this project yet.")
            } else {
                ProjectBomContent(current, onSelectBom = viewModel::selectBom)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectBomContent(
    content: ProjectBomUiState.Content,
    onSelectBom: (String) -> Unit
) {
    val selectedBom = content.boms.firstOrNull { it.id == content.selectedBomId }
    var filterMode by rememberSaveable(content.selectedBomId) { mutableStateOf(0) } // 0: All, 1: Shortages, 2: Matched

    val filteredItems = when (filterMode) {
        1 -> content.items.filter { it.shortage > 0 }
        2 -> content.items.filter { it.shortage == 0.0 && it.quantity > 0 }
        else -> content.items
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // BOM Selector if multiple exist
        if (content.boms.size > 1) {
            item {
                Text("Select BOM Version / Revision:", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    content.boms.forEach { bom ->
                        FilterChip(
                            selected = bom.id == content.selectedBomId,
                            onClick = { onSelectBom(bom.id) },
                            label = { Text("${bom.bomNumber} (Rev ${bom.revision ?: "Draft"})") }
                        )
                    }
                }
            }
        }

        // BOM Header Card
        selectedBom?.let { bom ->
            item {
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
                            Text(bom.bomNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            BomLifecycleBadge(isReleased = bom.isReleased, revision = bom.revision)
                        }
                        Text(bom.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        bom.sourceFilename?.let {
                            Text("Source file: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${bom.lineCount} total lines", style = MaterialTheme.typography.labelSmall)
                            val shortages = content.items.count { it.shortage > 0 }
                            Text(
                                if (shortages > 0) "$shortages lines with shortages" else "100% Stock Matched",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (shortages > 0) SsmAmberWarning else SsmGreenSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Filter chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filterMode == 0, onClick = { filterMode = 0 }, label = { Text("All (${content.items.size})") })
                FilterChip(selected = filterMode == 1, onClick = { filterMode = 1 }, label = { Text("Shortages (${content.items.count { it.shortage > 0 }})") })
                FilterChip(selected = filterMode == 2, onClick = { filterMode = 2 }, label = { Text("Available (${content.items.count { it.shortage == 0.0 && it.quantity > 0 }})") })
            }
        }

        if (content.loadingItems) {
            item { LoadingView("Loading line item stock match...") }
        } else if (filteredItems.isEmpty()) {
            item {
                Text(
                    if (filterMode == 1) "No shortages found for this BOM." else "No lines match the selected filter.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                BomLineItemCard(item)
            }
        }
    }
}

@Composable
private fun BomLifecycleBadge(isReleased: Boolean, revision: String?) {
    val (label, bg, fg) = if (isReleased) {
        Triple("RELEASED (Rev ${revision ?: "1"})", SsmGreenSuccess.copy(alpha = 0.2f), SsmGreenSuccess)
    } else {
        Triple("TENTATIVE DRAFT", SsmAmberWarning.copy(alpha = 0.2f), SsmAmberWarning)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun BomLineItemCard(item: ProjectBomItemDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.mpn ?: "No MPN",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                StockMatchBadge(item.matchStatus, item.shortage)
            }

            item.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOfNotNull(
                    item.manufacturer?.let { "Mfr: $it" },
                    item.footprint?.let { "Pkg: $it" },
                    item.value?.let { "Val: $it" }
                ).joinToString(" · ").let { info ->
                    if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item.refs?.let {
                Text("Designators: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Required: ${item.quantity.toInt()}", style = MaterialTheme.typography.labelMedium)
                if (item.shortage > 0) {
                    Text("Shortage: ${item.shortage.toInt()}", style = MaterialTheme.typography.labelMedium, color = SsmRedError, fontWeight = FontWeight.Bold)
                } else {
                    Text("In Stock: Covered", style = MaterialTheme.typography.labelMedium, color = SsmGreenSuccess, fontWeight = FontWeight.SemiBold)
                }
            }

            item.remark?.let {
                Text("Note: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun StockMatchBadge(status: String?, shortage: Double) {
    val (label, bg, fg) = when {
        status.equals("EXACT_MATCH", ignoreCase = true) || shortage == 0.0 ->
            Triple("EXACT MATCH", SsmGreenSuccess.copy(alpha = 0.15f), SsmGreenSuccess)
        status.equals("SUBSTITUTE_AVAILABLE", ignoreCase = true) ->
            Triple("SUBSTITUTE", SsmAmberWarning.copy(alpha = 0.15f), SsmAmberWarning)
        else ->
            Triple("SHORTAGE", SsmRedError.copy(alpha = 0.15f), SsmRedError)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = fg)
    }
}
