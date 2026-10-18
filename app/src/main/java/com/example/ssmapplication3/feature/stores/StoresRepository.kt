package com.example.ssmapplication3.feature.stores

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ssmapplication3.core.network.SupabaseClientProvider as Backend
import com.example.ssmapplication3.core.sync.OnlineTransactionRunner
import com.example.ssmapplication3.core.sync.SyncCoordinator
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class StoreMember(val id: String, val name: String, val active: Boolean)
@Serializable data class StorePart(val id: String, val part_number: String, val name: String)
@Serializable data class StoreLocation(val id: String, val component_id: String, val label: String? = null, val quantity: Double)
@Serializable data class StoreAssignment(val id: String, val component_id: String, val quantity: Int, val quantity_returned: Int = 0, val project_name: String? = null)
data class StoresData(val parts: List<StorePart>, val members: List<StoreMember> = emptyList(),
    val locations: List<StoreLocation> = emptyList(), val assignments: List<StoreAssignment> = emptyList()) {
    fun partLabel(id: String) = parts.find { it.id == id }?.let { "${it.part_number} · ${it.name}" } ?: "Component ${id.take(8)}"
}

object StoresRequests {
    const val ISSUE = "post_material_issue"
    const val RETURN = "post_material_return"
    const val ADJUSTMENT = "post_stock_adjustment"
    const val TRANSFER = "post_stock_transfer"
    fun issue(member: String, location: StoreLocation, quantity: Int): JsonObject {
        require(member.isNotBlank() && quantity > 0 && quantity <= location.quantity) { "Enter a whole quantity within the available balance." }
        return buildJsonObject {
            put("_assignee_id", member)
            put("_project_id", JsonNull); put("_project_name", JsonNull); put("_notes", JsonNull)
            put("_items", buildJsonArray { add(buildJsonObject {
                put("component_id", location.component_id); put("location_id", location.id); put("quantity", quantity)
            }) })
        }
    }
    fun returns(row: StoreAssignment, quantity: Int): JsonObject {
        require(quantity > 0 && quantity.toLong() <= row.quantity.toLong() - row.quantity_returned) { "Enter a whole quantity within the remaining balance." }
        return buildJsonObject { put("_assignment_id", row.id); put("_quantity", quantity) }
    }
    fun adjustment(location: StoreLocation, delta: Int, reason: String): JsonObject {
        require(delta != 0 && location.quantity + delta >= 0) { "Enter a non-zero quantity that does not make stock negative." }
        require(reason.trim().isNotEmpty()) { "A reason is required." }
        return buildJsonObject {
            put("_component_id", location.component_id)
            put("_location_id", location.id)
            put("_delta", delta)
            put("_reason", reason.trim())
        }
    }
    fun transfer(source: StoreLocation, destination: StoreLocation, quantity: Int, notes: String): JsonObject {
        require(source.component_id == destination.component_id && source.id != destination.id) { "Choose different source and destination locations for the same part." }
        require(quantity > 0 && quantity <= source.quantity) { "Enter a whole quantity within the source balance." }
        return buildJsonObject {
            put("_component_id", source.component_id)
            put("_from_location_id", source.id)
            put("_to_location_id", destination.id)
            put("_quantity", quantity)
            put("_notes", notes.trim())
        }
    }
}

class StoresRepository(context: Context) {
    private val app = context.applicationContext
    private val runner = OnlineTransactionRunner(
        SyncCoordinator.get(app).onlineStore(),
        { Backend.auth.currentUserOrNull()?.id },
        {
            val cm = app.getSystemService(ConnectivityManager::class.java)
            cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        },
        { rpc, payload, key ->
            val parameters = Json.parseToJsonElement(payload).jsonObject
            val result = try {
                Backend.postgrest.rpc(rpc, JsonObject(parameters + ("_idempotency_key" to JsonPrimitive(key))))
            } catch (failure: RestException) {
                // Only documented explicit server rejections release a held request.
                val known = listOf("Not allowed to issue material", "Active R&D member not found",
                    "Invalid issue location", "Insufficient stock for material issue",
                    "Not allowed to return material", "Assignment not found", "Return quantity exceeds issued quantity",
                    "Not allowed to adjust stock", "Adjustment reason, quantity, and request key are required",
                    "Invalid adjustment location or insufficient stock",
                    "Not allowed to transfer stock", "Valid transfer details and request key are required",
                    "Invalid transfer location or insufficient stock")
                val reason = known.firstOrNull { failure.error == it || failure.description == it }
                if (reason != null) throw OnlineTransactionRunner.DefinitiveRejection(reason)
                throw failure
            }
            val receipt = Json.parseToJsonElement(result.data).jsonObject
            val numberField = when (rpc) {
                StoresRequests.ISSUE -> "issue_number"
                StoresRequests.RETURN -> "return_number"
                StoresRequests.ADJUSTMENT -> "adjustment_number"
                StoresRequests.TRANSFER -> "transfer_number"
                else -> error("Unsupported controlled stock transaction")
            }
            val number = receipt[numberField]?.jsonPrimitive?.content
            check(!number.isNullOrBlank()) { "Server response is unconfirmed. Retry the saved request." }
            number
        }
    )

    suspend fun load(issue: Boolean): StoresData {
        val parts = Backend.postgrest.from("components").select(Columns.list("id", "part_number", "name")).decodeList<StorePart>()
        return if (issue) {
            val members = Backend.postgrest.from("rd_members").select(Columns.list("id", "name", "active")) { filter { eq("active", true) } }.decodeList<StoreMember>()
            val locations = Backend.postgrest.from("locations").select(Columns.list("id", "component_id", "label", "quantity")) { filter { gt("quantity", 0) } }.decodeList<StoreLocation>()
            StoresData(parts, members.sortedBy { it.name }, locations = locations)
        } else {
            val rows = Backend.postgrest.from("assignments").select(Columns.list("id", "component_id", "quantity", "quantity_returned", "project_name")).decodeList<StoreAssignment>()
            StoresData(parts, assignments = rows.filter { it.quantity > it.quantity_returned })
        }
    }
    suspend fun loadAdjustmentLocations(): StoresData {
        val parts = Backend.postgrest.from("components").select(Columns.list("id", "part_number", "name")).decodeList<StorePart>()
        val locations = Backend.postgrest.from("locations")
            .select(Columns.list("id", "component_id", "label", "quantity")).decodeList<StoreLocation>()
        return StoresData(parts, locations = locations.sortedBy { partLabelFor(parts, it.component_id, it.label) })
    }
    suspend fun pending(rpc: String) = runner.pending(rpc)
    suspend fun post(rpc: String, payload: String) = runner.execute(rpc, payload)
}

private fun partLabelFor(parts: List<StorePart>, componentId: String, label: String?) =
    parts.find { it.id == componentId }?.let { "${it.part_number} · ${it.name} · ${label ?: "Bin"}" } ?: componentId
