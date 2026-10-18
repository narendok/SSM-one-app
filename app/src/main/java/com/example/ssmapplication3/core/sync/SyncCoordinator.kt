package com.example.ssmapplication3.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Common durable queue for server-authoritative mobile mutations. It stores the
 * request UUID and canonical payload before a protected RPC can be attempted.
 */
class SyncCoordinator private constructor(private val context: Context) {
    private val dao = MobileCacheDatabase.get(context).pendingMutationDao()

    suspend fun loadLocalDraft(action: String): String? = dao.localDraft(action)?.payload

    suspend fun saveLocalDraft(action: String, payload: String) {
        dao.saveLocalDraft(PendingMutation("draft:$action", action, payload, System.currentTimeMillis(), status = "DRAFT"))
    }

    fun onlineStore(): OnlineTransactionRunner.Store = object : OnlineTransactionRunner.Store {
        override suspend fun held(action: String) = dao.heldOnline(action)
        override suspend fun save(request: PendingMutation) {
            check(dao.insert(request) != -1L) { "Could not reserve transaction request." }
        }
        override suspend fun complete(key: String) = dao.markCompleted(key)
        override suspend fun reject(key: String) = dao.markRejected(key, "Rejected by server")
    }

    suspend fun enqueue(action: String, payload: String, requestKey: String = UUID.randomUUID().toString()): String {
        val mutation = PendingMutation(
            requestKey = requestKey,
            action = action,
            payload = payload,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        dao.insert(mutation)
        schedule()
        return requestKey
    }

    suspend fun processPending() {
        dao.pending().forEach { mutation ->
            val executor = executors[mutation.action] ?: return@forEach
            dao.markProcessing(mutation.requestKey)
            runCatching { executor.execute(mutation) }
                .onSuccess { dao.markCompleted(mutation.requestKey) }
                .onFailure { dao.markFailed(mutation.requestKey, it.message ?: "Server operation failed") }
        }
    }

    fun schedule() {
        val request = OneTimeWorkRequestBuilder<PendingMutationWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    interface Executor { suspend fun execute(mutation: PendingMutation) }

    companion object {
        private const val WORK_NAME = "ssm-one-pending-mutations"
        private val executors = mutableMapOf<String, Executor>()
        @Volatile private var instance: SyncCoordinator? = null

        fun initialize(context: Context) { get(context) }
        fun get(context: Context): SyncCoordinator = instance ?: synchronized(this) {
            instance ?: SyncCoordinator(context.applicationContext).also { instance = it }
        }
        fun registerExecutor(action: String, executor: Executor) { executors[action] = executor }
    }
}

class PendingMutationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        SyncCoordinator.get(applicationContext).processPending()
        return Result.success()
    }
}
