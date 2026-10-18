package com.example.ssmapplication3.feature.stores.adjustments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.sync.OnlineTransactionRunner
import com.example.ssmapplication3.core.sync.PendingMutation
import com.example.ssmapplication3.feature.stores.*
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

data class AdjustmentState(
    val loading: Boolean = true,
    val data: StoresData = StoresData(emptyList()),
    val pending: PendingMutation? = null,
    val posting: Boolean = false,
    val message: String? = null
)

class StockAdjustmentViewModel(private val repository: StoresRepository) : ViewModel() {
    private val mutable = MutableStateFlow(AdjustmentState())
    val state = mutable.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (state.value.posting) return
        viewModelScope.launch {
            mutable.value = state.value.copy(loading = true)
            try {
                val held = repository.pending(StoresRequests.ADJUSTMENT)
                val data = repository.loadAdjustmentLocations()
                mutable.value = state.value.copy(loading = false, data = data, pending = held)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { mutable.value = state.value.copy(loading = false, message = "Could not load stock locations available to this account.") }
        }
    }

    fun submit(payload: String) {
        if (state.value.posting || state.value.loading) return
        mutable.value = state.value.copy(posting = true, message = null)
        viewModelScope.launch {
            try {
                val number = repository.post(StoresRequests.ADJUSTMENT, payload)
                mutable.value = state.value.copy(posting = false, pending = null, message = "Posted successfully: $number")
                refresh()
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                val held = try { repository.pending(StoresRequests.ADJUSTMENT) } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { state.value.pending }
                mutable.value = state.value.copy(
                    posting = false,
                    pending = held,
                    message = when {
                        failure is OnlineTransactionRunner.DefinitiveRejection -> "Not posted: ${failure.message}. Refresh and correct the details."
                        held != null && failure is RestException -> "Posting is unconfirmed. Server response (${failure.statusCode}): ${failure.error.take(180)}"
                        held != null -> "Posting is unconfirmed. Retry the saved request; do not create another adjustment."
                        else -> "Could not confirm submission. Refresh before trying again."
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(viewModel: StockAdjustmentViewModel) {
    val state by viewModel.state.collectAsState()
    var locationId by rememberSaveable { mutableStateOf("") }
    var deltaText by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<String?>(null) }
    val location = state.data.locations.find { it.id == locationId }
    val payload = runCatching {
        StoresRequests.adjustment(requireNotNull(location), requireNotNull(deltaText.toIntOrNull()), reason).toString()
    }.getOrNull()

    Scaffold(topBar = { TopAppBar(title = { Text("Stock adjustment") }, actions = {
        TextButton(onClick = viewModel::refresh, enabled = !state.loading && !state.posting) { Text("Refresh") }
    }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Online-only controlled adjustment. The server validates permission, balance, numbering and audit.", style = MaterialTheme.typography.bodySmall) }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.message?.let { item { Text(it) } }
            if (state.pending != null) item {
                Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val saved = Json.parseToJsonElement(state.pending!!.payload).jsonObject
                    Text("Saved adjustment awaiting confirmation", style = MaterialTheme.typography.titleSmall)
                    Text("Delta: ${saved["_delta"] ?: "unknown"}")
                    Text("Retry uses the original request key and values.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { viewModel.submit(state.pending!!.payload) }, enabled = !state.posting && !state.loading) { Text("Retry saved adjustment") }
                } }
            } else {
                item { AdjustmentLocationPicker(state.data, locationId, !state.posting) { locationId = it; deltaText = "" } }
                item {
                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { deltaText = it },
                        label = { Text("Quantity change (+ add, − remove)") },
                        supportingText = { Text("Current balance: ${location?.quantity ?: "choose a location"}") },
                        singleLine = true,
                        isError = deltaText.isNotBlank() && payload == null,
                        enabled = !state.posting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        supportingText = { Text("Required and included in the server audit record") },
                        enabled = !state.posting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = { confirmation = payload },
                        enabled = payload != null && !state.posting && !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Review adjustment") }
                }
            }
        }
    }
    confirmation?.let { request ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Confirm stock adjustment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(location?.let { state.data.partLabel(it.component_id) } ?: "Stock location")
                    Text("Bin: ${location?.label ?: "—"}")
                    Text("Change: $deltaText")
                    Text("Reason: $reason")
                    Text("This changes stock and creates an audit record.")
                }
            },
            confirmButton = { TextButton(onClick = { confirmation = null; viewModel.submit(request) }) { Text("Post now") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdjustmentLocationPicker(data: StoresData, selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = data.locations.map { it.id to "${data.partLabel(it.component_id)} · ${it.label ?: "Bin"} · ${it.quantity} on hand" }
    Column {
        Text("Part / stock location", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && options.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(options.find { it.first == selected }?.second ?: if (options.isEmpty()) "No stock locations available" else "Choose stock location")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            options.forEach { (id, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onSelect(id) }) }
        }
    }
}
