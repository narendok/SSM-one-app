package com.example.ssmapplication3.feature.stores.transfers

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

data class TransferState(val loading: Boolean = true, val data: StoresData = StoresData(emptyList()), val pending: PendingMutation? = null, val posting: Boolean = false, val message: String? = null)

class StockTransferViewModel(private val repository: StoresRepository) : ViewModel() {
    private val mutable = MutableStateFlow(TransferState())
    val state = mutable.asStateFlow()
    init { refresh() }

    fun refresh() {
        if (state.value.posting) return
        viewModelScope.launch {
            mutable.value = state.value.copy(loading = true)
            try { mutable.value = state.value.copy(loading = false, data = repository.loadAdjustmentLocations(), pending = repository.pending(StoresRequests.TRANSFER)) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { mutable.value = state.value.copy(loading = false, message = "Could not load stock locations available to this account.") }
        }
    }

    fun submit(payload: String) {
        if (state.value.posting || state.value.loading) return
        mutable.value = state.value.copy(posting = true, message = null)
        viewModelScope.launch {
            try { mutable.value = state.value.copy(posting = false, pending = null, message = "Posted successfully: ${repository.post(StoresRequests.TRANSFER, payload)}"); refresh() }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                val held = try { repository.pending(StoresRequests.TRANSFER) } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { state.value.pending }
                mutable.value = state.value.copy(posting = false, pending = held, message = when {
                    failure is OnlineTransactionRunner.DefinitiveRejection -> "Not posted: ${failure.message}. Refresh and correct the details."
                    held != null && failure is RestException -> "Posting is unconfirmed. Server response (${failure.statusCode}): ${failure.error.take(180)}"
                    held != null -> "Posting is unconfirmed. Retry the saved transfer; do not create another transfer."
                    else -> "Could not confirm submission. Refresh before trying again."
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StockTransferScreen(viewModel: StockTransferViewModel) {
    val state by viewModel.state.collectAsState()
    var sourceId by rememberSaveable { mutableStateOf("") }
    var destinationId by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var review by remember { mutableStateOf<String?>(null) }
    val source = state.data.locations.find { it.id == sourceId }
    val destination = state.data.locations.find { it.id == destinationId }
    val payload = runCatching { StoresRequests.transfer(requireNotNull(source), requireNotNull(destination), requireNotNull(quantityText.toIntOrNull()), notes).toString() }.getOrNull()
    Scaffold(topBar = { TopAppBar(title = { Text("Stock transfer") }, actions = { TextButton(onClick = viewModel::refresh, enabled = !state.loading && !state.posting) { Text("Refresh") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Online-only controlled transfer. The server locks both locations, preserves balance, numbers the transfer and writes the audit record.", style = MaterialTheme.typography.bodySmall) }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.message?.let { item { Text(it) } }
            if (state.pending != null) item { Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Saved transfer awaiting confirmation", style = MaterialTheme.typography.titleSmall)
                Text("Retry uses the original request key and values.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { viewModel.submit(state.pending!!.payload) }, enabled = !state.posting) { Text("Retry saved transfer") }
            } } } else {
                item { LocationPicker("Source location", state.data, sourceId, !state.posting) { sourceId = it; destinationId = ""; quantityText = "" } }
                val destinations = source?.let { selected -> state.data.locations.filter { it.component_id == selected.component_id && it.id != selected.id } }.orEmpty()
                item { LocationPicker("Destination location", state.data.copy(locations = destinations), destinationId, !state.posting && source != null) { destinationId = it } }
                item { OutlinedTextField(quantityText, { quantityText = it }, label = { Text("Transfer quantity") }, supportingText = { Text("Source balance: ${source?.quantity ?: "choose source"}") }, singleLine = true, isError = quantityText.isNotBlank() && payload == null, enabled = !state.posting, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, enabled = !state.posting, modifier = Modifier.fillMaxWidth()) }
                item { Button(onClick = { review = payload }, enabled = payload != null && !state.posting && !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Review transfer") } }
            }
        }
    }
    review?.let { request -> AlertDialog(onDismissRequest = { review = null }, title = { Text("Confirm stock transfer") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(state.data.partLabel(source?.component_id.orEmpty())); Text("From: ${source?.label ?: "—"}"); Text("To: ${destination?.label ?: "—"}"); Text("Quantity: $quantityText"); Text("This moves stock between locations and creates an audit record.")
    } }, confirmButton = { TextButton(onClick = { review = null; viewModel.submit(request) }) { Text("Post now") } }, dismissButton = { TextButton(onClick = { review = null }) { Text("Cancel") } }) }
}

@Composable private fun LocationPicker(label: String, data: StoresData, selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = data.locations.map { it.id to "${data.partLabel(it.component_id)} · ${it.label ?: "Bin"} · ${it.quantity} on hand" }
    Column { Text(label, style = MaterialTheme.typography.labelLarge); OutlinedButton(onClick = { expanded = true }, enabled = enabled && options.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(options.find { it.first == selected }?.second ?: if (options.isEmpty()) "No eligible locations" else "Choose location") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) { options.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(id) }) } }
    }
}
