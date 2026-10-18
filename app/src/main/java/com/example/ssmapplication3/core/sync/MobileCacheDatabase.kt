package com.example.ssmapplication3.core.sync

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PendingMutation::class], version = 1, exportSchema = false)
abstract class MobileCacheDatabase : RoomDatabase() {
    abstract fun pendingMutationDao(): PendingMutationDao

    companion object {
        @Volatile private var instance: MobileCacheDatabase? = null

        fun get(context: Context): MobileCacheDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MobileCacheDatabase::class.java,
                "ssm-one-mobile-cache"
            ).build().also { instance = it }
        }
    }
}
