package com.example.ssmapplication3.feature.procurement

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcurementReadProjectionTest {
    @Test fun receivingProjectionAllowsFractionalAndUnknownQuantities() {
        val fractional = Json.decodeFromString<PurchaseOrderItemRow>(
            """{"id":"line","po_id":"order","mpn":"wire","quantity_ordered":2.5,"quantity_received":0.75}"""
        )
        assertEquals(2.5, fractional.quantityOrdered!!, 0.0)
        assertEquals(0.75, fractional.quantityReceived!!, 0.0)
        val unknown = Json.decodeFromString<PurchaseOrderItemRow>("""{"id":"line","po_id":"order"}""")
        assertNull(unknown.quantityOrdered)
        assertNull(unknown.quantityReceived)
        assertNull(unknown.mpn)
    }

    @Test fun partsMustBelongToAnOrderInTheVisibleOrderProjection() {
        val lines = listOf(PurchaseOrderItemRow("allowed-line", "allowed"), PurchaseOrderItemRow("other-line", "other"))
        assertEquals(listOf("allowed-line"), receivingItemsForOrders(lines, setOf("allowed")).map { it.id })
        assertTrue(receivingItemsForOrders(lines, emptySet()).isEmpty())
    }
}
