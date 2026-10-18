package com.example.ssmapplication3

import com.example.ssmapplication3.core.sync.*
import com.example.ssmapplication3.feature.stores.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class OnlineTransactionRunnerTest {
    private class Journal : OnlineTransactionRunner.Store {
        val rows = mutableMapOf<String, PendingMutation>()
        override suspend fun held(action: String) = rows.values.find { it.action == action }
        override suspend fun save(request: PendingMutation) { rows[request.requestKey] = request }
        override suspend fun complete(key: String) { rows.remove(key) }
        override suspend fun reject(key: String) { rows.remove(key) }
    }

    @Test fun lostResponseAndRestartReusePersistedKey() = runBlocking {
        val journal = Journal()
        val keys = mutableListOf<String>()
        val first = OnlineTransactionRunner(journal, { "user" }, { true }, { _, _, key ->
            keys += key; throw java.io.IOException("Response lost")
        })
        assertTrue(runCatching { first.execute("issue", "payload") }.isFailure)
        val restored = OnlineTransactionRunner(journal, { "user" }, { true }, { _, payload, key ->
            assertEquals("payload", payload); keys += key; "ISS-1"
        })
        assertTrue(runCatching { restored.execute("issue", "changed") }.isFailure)
        assertEquals("ISS-1", restored.execute("issue", "payload"))
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
        assertNull(restored.pending("issue"))
    }

    @Test fun offlineDoesNotJournalOrSend() = runBlocking {
        val journal = Journal()
        val runner = OnlineTransactionRunner(journal, { "user" }, { false }, { _, _, _ -> error("Must not send") })
        assertTrue(runCatching { runner.execute("issue", "payload") }.isFailure)
        assertTrue(journal.rows.isEmpty())
    }

    @Test fun anotherAccountCannotReadHeldRequest() = runBlocking {
        val journal = Journal()
        var owner = "first"
        val runner = OnlineTransactionRunner(journal, { owner }, { true }, { _, _, _ -> error("Lost response") })
        runCatching { runner.execute("issue", "payload") }
        assertNotNull(runner.pending("issue"))
        owner = "second"
        assertNull(runner.pending("issue"))
        owner = "first"
        assertNotNull(runner.pending("issue"))
    }

    @Test fun requestsMatchContractAndRejectInvalidBalances() {
        val location = StoreLocation("bin", "component", "Test", 2.0)
        val item = StoresRequests.issue("member", location, 2)["_items"]!!.jsonArray.single().jsonObject
        assertEquals(setOf("component_id", "location_id", "quantity"), item.keys)
        assertTrue(runCatching { StoresRequests.issue("member", location, 3) }.isFailure)
        assertTrue(runCatching { StoresRequests.issue("member", location, 0) }.isFailure)
        val assignment = StoreAssignment("assignment", "component", 5, 4)
        assertEquals(1, StoresRequests.returns(assignment, 1)["_quantity"]!!.jsonPrimitive.int)
        assertTrue(runCatching { StoresRequests.returns(assignment, 2) }.isFailure)

        val adjustment = StoresRequests.adjustment(location, -2, "Verified count")
        assertEquals(setOf("_component_id", "_location_id", "_delta", "_reason"), adjustment.keys)
        assertEquals(-2, adjustment["_delta"]!!.jsonPrimitive.int)
        assertEquals("Verified count", adjustment["_reason"]!!.jsonPrimitive.content)
        assertTrue(runCatching { StoresRequests.adjustment(location, 0, "Reason") }.isFailure)
        assertTrue(runCatching { StoresRequests.adjustment(location, -3, "Reason") }.isFailure)
        assertTrue(runCatching { StoresRequests.adjustment(location, 1, " ") }.isFailure)

        val destination = StoreLocation("destination", "component", "Other bin", 0.0)
        val transfer = StoresRequests.transfer(location, destination, 2, "Relocate")
        assertEquals(setOf("_component_id", "_from_location_id", "_to_location_id", "_quantity", "_notes"), transfer.keys)
        assertEquals(2, transfer["_quantity"]!!.jsonPrimitive.int)
        assertTrue(runCatching { StoresRequests.transfer(location, location, 1, "") }.isFailure)
        assertTrue(runCatching { StoresRequests.transfer(location, destination, 3, "") }.isFailure)
    }
}
