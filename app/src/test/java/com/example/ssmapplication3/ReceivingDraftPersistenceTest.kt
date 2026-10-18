package com.example.ssmapplication3

import com.example.ssmapplication3.feature.receiving.LineCapture
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceivingDraftPersistenceTest {
    @Test fun savedDraftPreservesAllCapturedFieldsByLine() {
        val captures = mapOf("po-line-1" to LineCapture("2", "LOT-1", "B-1", "2639", "R-1", "S-1", "MSL 3", "Sealed"),
            "po-line-2" to LineCapture(quantity = "1"))
        val restored = Json.decodeFromString<Map<String, LineCapture>>(Json.encodeToString(captures))
        assertEquals(captures, restored)
    }
}
