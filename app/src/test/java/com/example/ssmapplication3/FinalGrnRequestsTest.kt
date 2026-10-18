package com.example.ssmapplication3

import com.example.ssmapplication3.feature.receiving.FinalGrnRequests
import com.example.ssmapplication3.feature.receiving.LineCapture
import com.example.ssmapplication3.feature.receiving.ReceivingLine
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalGrnRequestsTest {
    private val line = ReceivingLine("po-item", "po", "component", "Test part", 3.0, false)

    @Test fun createsOnlyVerifiedGrnFieldsAndWholePositiveLine() {
        val payload = FinalGrnRequests.payload(
            "po", "INV-12", "2026-09-27", "Rack A",
            listOf(line to LineCapture(quantity = "2", lot = "LOT-1", dateCode = "2420", reel = "R-1"))
        )
        assertEquals(setOf("_po_id", "_vendor_invoice_number", "_vendor_invoice_date", "_storage_notes", "_items"), payload.keys)
        val item = payload["_items"]!!.jsonArray.single().jsonObject
        assertEquals(setOf("po_item_id", "component_id", "quantity", "quantity_rejected", "lot_number", "date_code", "reel_id"), item.keys)
        assertEquals(2, item["quantity"]!!.jsonPrimitive.int)
        assertEquals(0, item["quantity_rejected"]!!.jsonPrimitive.int)
    }

    @Test fun rejectsIncompleteOrFractionalFinalGrn() {
        assertTrue(runCatching { FinalGrnRequests.payload("po", "", "2026-09-27", "", listOf(line to LineCapture(quantity = "1"))) }.isFailure)
        assertTrue(runCatching { FinalGrnRequests.payload("po", "INV", "27-09-2026", "", listOf(line to LineCapture(quantity = "1"))) }.isFailure)
        assertTrue(runCatching { FinalGrnRequests.payload("po", "INV", "2026-09-27", "", listOf(line to LineCapture(quantity = "1.5"))) }.isFailure)
    }
}
