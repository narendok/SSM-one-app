package com.example.ssmapplication3.core.sync

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending_mutations")
data class PendingMutation(
    @PrimaryKey val requestKey: String,
    val action: String,
    val payload: String,
    val createdAtEpochMillis: Long,
    val status: String = Status.PENDING.name,
    val attempts: Int = 0,
    val lastError: String? = null
) {
    enum class Status { PENDING, PROCESSING, COMPLETED, FAILED }
}

@Dao
interface PendingMutationDao {
    @Query("SELECT * FROM pending_mutations WHERE action = :action AND status = 'DRAFT' LIMIT 1")
    suspend fun localDraft(action: String): PendingMutation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalDraft(draft: PendingMutation)

    @Query("SELECT * FROM pending_mutations WHERE action = :action AND status = 'ONLINE_HELD' ORDER BY createdAtEpochMillis LIMIT 1")
    suspend fun heldOnline(action: String): PendingMutation?

    @Query("UPDATE pending_mutations SET status = 'REJECTED', lastError = :message WHERE requestKey = :requestKey")
    suspend fun markRejected(requestKey: String, message: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mutation: PendingMutation): Long

    @Query("SELECT * FROM pending_mutations WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAtEpochMillis")
    suspend fun pending(): List<PendingMutation>

    @Query("SELECT * FROM pending_mutations WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAtEpochMillis")
    fun observePending(): Flow<List<PendingMutation>>

    @Query("UPDATE pending_mutations SET status = 'PROCESSING', attempts = attempts + 1, lastError = NULL WHERE requestKey = :requestKey")
    suspend fun markProcessing(requestKey: String)

    @Query("UPDATE pending_mutations SET status = 'COMPLETED', lastError = NULL WHERE requestKey = :requestKey")
    suspend fun markCompleted(requestKey: String)

    @Query("UPDATE pending_mutations SET status = 'FAILED', lastError = :message WHERE requestKey = :requestKey")
    suspend fun markFailed(requestKey: String, message: String)
}
