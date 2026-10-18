package com.example.ssmapplication3.core.sync

import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persist-before-send journal for explicit online submissions, never a background queue. */
class OnlineTransactionRunner(
    private val store: Store,
    private val identity: () -> String?,
    private val online: () -> Boolean,
    private val send: suspend (String, String, String) -> String,
    private val newKey: () -> String = { UUID.randomUUID().toString() }
) {
    interface Store {
        suspend fun held(action: String): PendingMutation?
        suspend fun save(request: PendingMutation)
        suspend fun complete(key: String)
        suspend fun reject(key: String)
    }

    suspend fun pending(rpc: String): PendingMutation? = identity()?.let { store.held("online:$it:$rpc") }

    suspend fun execute(rpc: String, payload: String): String = lock.withLock {
        val owner = requireNotNull(identity()) { "Sign in before posting." }
        check(online()) { "Connect to the internet before posting." }
        val action = "online:$owner:$rpc"
        val held = store.held(action)
        check(held == null || held.payload == payload) {
            "An earlier request is unresolved. Retry that unchanged request first."
        }
        val request = held ?: PendingMutation(newKey(), action, payload,
            System.currentTimeMillis(), status = "ONLINE_HELD").also { store.save(it) }
        check(identity() == owner) { "The signed-in account changed. Reopen this screen." }
        try {
            val receipt = send(rpc, request.payload, request.requestKey)
            store.complete(request.requestKey)
            receipt
        } catch (rejected: DefinitiveRejection) {
            store.reject(request.requestKey)
            throw rejected
        }
        // Other failures (including cancellation or a lost response) retain the original payload/key.
    }

    class DefinitiveRejection(message: String) : Exception(message)
    companion object { private val lock = Mutex() }
}
