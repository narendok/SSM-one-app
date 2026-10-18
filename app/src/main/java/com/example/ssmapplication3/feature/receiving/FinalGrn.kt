package com.example.ssmapplication3.feature.receiving

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ssmapplication3.core.network.SupabaseClientProvider as Backend
import com.example.ssmapplication3.core.sync.OnlineTransactionRunner
import com.example.ssmapplication3.core.sync.PendingMutation
import com.example.ssmapplication3.core.sync.SyncCoordinator
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class GrnSubmissionState(
    val posting: Boolean = false,
    val pending: PendingMutation? = null,
    val message: String? = null
)

object FinalGrnRequests {
    const val RPC = "create_grn"

    fun payload(
        poId: String,
        invoiceNumber: String,
        invoiceDate: String,
        storageNotes: String,
        lines: List<Pair<ReceivingLine, LineCapture>>
    ): JsonObject {
        require(poId.isNotBlank()) { "Choose a purchase order." }
        require(invoiceNumber.trim().isNotEmpty()) { "Vendor invoice number is required." }
        require(invoiceDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Invoice date must use YYYY-MM-DD." }
        val items = buildJsonArray {
            lines.forEach { (line, capture) ->
                val quantity = capture.quantity.toIntOrNull() ?: 0
                if (quantity > 0) add(buildJsonObject {
                    put("po_item_id", JsonPrimitive(line.id))
                    put("component_id", JsonPrimitive(line.componentId))
                    put("quantity", JsonPrimitive(quantity))
                    put("quantity_rejected", JsonPrimitive(0))
                    if (capture.lot.isNotBlank()) put("lot_number", JsonPrimitive(capture.lot.trim()))
                    if (capture.dateCode.isNotBlank()) put("date_code", JsonPrimitive(capture.dateCode.trim()))
                    if (capture.reel.isNotBlank()) put("reel_id", JsonPrimitive(capture.reel.trim()))
                })
            }
        }
        require(items.isNotEmpty()) { "Enter a valid received quantity for at least one line." }
        return buildJsonObject {
            put("_po_id", JsonPrimitive(poId))
            put("_vendor_invoice_number", JsonPrimitive(invoiceNumber.trim()))
            put("_vendor_invoice_date", JsonPrimitive(invoiceDate))
            put("_storage_notes", JsonPrimitive(storageNotes.trim()))
            put("_items", items)
        }
    }
}

class FinalGrnRepository(context: Context) {
    private val app = context.applicationContext
    private val runner = OnlineTransactionRunner(
        SyncCoordinator.get(app).onlineStore(),
        { Backend.auth.currentUserOrNull()?.id },
        {
            val manager = app.getSystemService(ConnectivityManager::class.java)
            manager.getNetworkCapabilities(manager.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        },
        { rpc, payload, key ->
            val parameters = Json.parseToJsonElement(payload).jsonObject
            val result = try {
                Backend.postgrest.rpc(rpc, JsonObject(parameters + ("_idempotency_key" to JsonPrimitive(key))))
            } catch (failure: RestException) {
                val definitive = listOf(
                    "Not allowed to record goods receipts",
                    "No received items provided",
                    "Purchase order not found",
                    "Received item does not belong to this purchase order",
                    "Received quantity exceeds the outstanding purchase order quantity"
                ).firstOrNull { it == failure.error || it == failure.description }
                if (definitive != null) throw OnlineTransactionRunner.DefinitiveRejection(definitive)
                throw failure
            }
            val receipt = Json.parseToJsonElement(result.data).jsonObject
            receipt["grn_number"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: error("Server response is unconfirmed. Retry the saved request.")
        }
    )

    suspend fun pending() = runner.pending(FinalGrnRequests.RPC)
    suspend fun post(payload: String) = runner.execute(FinalGrnRequests.RPC, payload)
}
