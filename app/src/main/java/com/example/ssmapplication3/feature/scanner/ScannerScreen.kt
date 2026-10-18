package com.example.ssmapplication3.feature.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ssmapplication3.core.ui.EmptyView
import com.example.ssmapplication3.core.ui.ErrorView
import com.example.ssmapplication3.core.ui.LoadingView
import com.example.ssmapplication3.feature.inventory.InventoryItem
import com.example.ssmapplication3.feature.inventory.InventoryUiState
import com.example.ssmapplication3.feature.inventory.InventoryViewModel

/** Barcode-wedge and manual lookup surface. Camera decoding is intentionally not assumed without a verified scanner dependency. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ScannerScreen(viewModel: InventoryViewModel) {
    val state by viewModel.state.collectAsState()
    var code by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Scan lookup") }, actions = { TextButton(onClick = viewModel::refresh) { Text("Refresh") } }) }) { padding ->
        when (val current = state) {
            InventoryUiState.Loading -> LoadingView("Loading components…")
            is InventoryUiState.Failure -> ErrorView(current.message, viewModel::refresh)
            is InventoryUiState.Content -> {
                val matches = current.items.filter { it.matches(code) }
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Use a hardware barcode scanner or enter a part, MPN, or bin.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(code, { code = it }, label = { Text("Scan or enter code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    if (code.isBlank()) item { Text("Enter a code to search the inventory visible to your account.") }
                    else if (matches.isEmpty()) item { EmptyView("No matching component", "No RLS-visible component or stock location matches this code.") }
                    else items(matches, key = { it.component.id }) { item -> ScanResult(item) }
                }
            }
        }
    }
}

private fun InventoryItem.matches(query: String): Boolean {
    val value = query.trim()
    return value.isBlank() || listOf(component.partNumber, component.name, component.manufacturer.orEmpty(), component.packageCase.orEmpty())
        .plus(locations.map { it.label }).any { it.contains(value, ignoreCase = true) }
}

@Composable private fun ScanResult(item: InventoryItem) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(item.component.partNumber, fontWeight = FontWeight.SemiBold)
        Text(item.component.name)
        Text("On hand: ${item.locations.sumOf { it.quantity }}", fontWeight = FontWeight.Medium)
        if (item.locations.isNotEmpty()) Text("Bins: " + item.locations.joinToString { "${it.label} (${it.quantity})" }, style = MaterialTheme.typography.bodySmall)
        if (item.lots.isNotEmpty()) Text("Lots: " + item.lots.joinToString { listOfNotNull(it.lotNumber, it.dateCode, it.reelId, it.qualityStatus).joinToString(" • ") }, style = MaterialTheme.typography.bodySmall)
    } }
}
