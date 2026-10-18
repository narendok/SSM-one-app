package com.example.ssmapplication3.feature.stores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ssmapplication3.core.sync.PendingMutation
import com.example.ssmapplication3.core.sync.OnlineTransactionRunner
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

data class StoresState(val loading: Boolean = true, val data: StoresData = StoresData(emptyList()),
    val pending: PendingMutation? = null, val posting: Boolean = false, val message: String? = null)

open class StoresViewModel(private val repository: StoresRepository, val issue: Boolean) : ViewModel() {
    private val mutable = MutableStateFlow(StoresState())
    val state = mutable.asStateFlow()
    private val rpc = if (issue) StoresRequests.ISSUE else StoresRequests.RETURN
    init { refresh() }
    fun refresh() {
        if (state.value.posting) return
        viewModelScope.launch {
            mutable.value = state.value.copy(loading = true)
            try {
                val held = repository.pending(rpc)
                mutable.value = state.value.copy(pending = held)
                val data = repository.load(issue)
                mutable.value = state.value.copy(loading = false, data = data)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { mutable.value = state.value.copy(loading = false, message = "Could not refresh. Check your connection and access.") }
        }
    }
    fun submit(payload: String) {
        if (state.value.posting || state.value.loading) return
        mutable.value = state.value.copy(posting = true, message = null)
        viewModelScope.launch {
            try {
                val number = repository.post(rpc, payload)
                mutable.value = state.value.copy(posting = false, pending = null, message = "Posted successfully: $number")
                refresh()
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                val held = try { repository.pending(rpc) } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { state.value.pending }
                mutable.value = state.value.copy(posting = false, pending = held,
                    message = if (failure is OnlineTransactionRunner.DefinitiveRejection) "Not posted: ${failure.message}. Refresh and correct the selection."
                    else if (held != null) "Posting is unconfirmed. Retry the saved request to check its result safely. Do not create another transaction." +
                        if (failure is RestException) " Server response (${failure.statusCode}): ${failure.error.take(180)}" else " Check the connection and retry."
                    else "Could not confirm submission. Refresh before trying again.")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StoresScreen(vm: StoresViewModel) {
    val state by vm.state.collectAsState()
    var member by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf<String?>(null) }
    val title = if (vm.issue) "Material issue" else "Material return"
    val location = state.data.locations.find { it.id == selected }
    val assignment = state.data.assignments.find { it.id == selected }
    val payload = runCatching {
        val amount = requireNotNull(quantity.toIntOrNull())
        if (vm.issue) {
            require(state.data.members.any { it.id == member && it.active })
            StoresRequests.issue(member, requireNotNull(location), amount)
        } else StoresRequests.returns(requireNotNull(assignment), amount)
    }.getOrNull()
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, actions = {
        TextButton(onClick = vm::refresh, enabled = !state.posting && !state.loading) { Text("Refresh") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Review the details before posting. Internet connection required.", style = MaterialTheme.typography.bodySmall) }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.message?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium) } }
            if (state.pending != null) {
                item {
                    Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Saved request awaiting confirmation", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        val saved = Json.parseToJsonElement(state.pending!!.payload).jsonObject
                        val savedQuantity = if (vm.issue) saved["_items"]?.jsonArray?.firstOrNull()?.jsonObject?.get("quantity") else saved["_quantity"]
                        Text("Quantity: ${savedQuantity ?: "See original request"}", style = MaterialTheme.typography.bodySmall)
                        Text("Retry uses the original selection and request key, including after restarting the app.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Button(onClick = { vm.submit(state.pending!!.payload) }, enabled = !state.posting && !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Retry saved request") }
                    } }
                }
            } else {
                if (vm.issue) item {
                    ChoiceField("Assignee", state.data.members.map { it.id to it.name }, member, !state.posting) { member = it }
                }
                item {
                    val options = if (vm.issue) state.data.locations.map { it.id to "${state.data.partLabel(it.component_id)} · ${it.label ?: "Bin"} · ${it.quantity} available" }
                        else state.data.assignments.map { it.id to "${state.data.partLabel(it.component_id)} · ${it.project_name ?: "Assignment"} · ${it.quantity - it.quantity_returned} remaining" }
                    ChoiceField(if (vm.issue) "Part / stock location" else "Returnable assignment", options, selected, !state.posting) { selected = it; quantity = "" }
                }
                item {
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = quantity.isNotEmpty() && payload == null, enabled = !state.posting, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Button(onClick = { confirm = payload?.toString() }, enabled = payload != null && !state.posting && !state.loading, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.posting) "Posting…" else "Review ${if (vm.issue) "issue" else "return"}")
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    confirm?.let { request ->
        AlertDialog(onDismissRequest = { confirm = null }, title = { Text("Confirm $title") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (vm.issue) "${state.data.members.find { it.id == member }?.name}\n${location?.let { state.data.partLabel(it.component_id) }}\nBin: ${location?.label}" else assignment?.let { state.data.partLabel(it.component_id) } ?: "Assignment")
                Text("Quantity: $quantity")
                Text("This changes stock and creates an audit record.")
            }
        }, confirmButton = { TextButton(onClick = { confirm = null; vm.submit(request) }) { Text("Post now") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } })
    }
}

@Composable private fun ChoiceField(label: String, options: List<Pair<String, String>>, selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && options.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(options.find { it.first == selected }?.second ?: if (options.isEmpty()) "No records available to this account" else "Choose $label")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            options.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(id) }) }
        }
    }
}
