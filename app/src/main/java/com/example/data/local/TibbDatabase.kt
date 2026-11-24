package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AccountEntity::class, VaultItemEntity::class, AuditLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TibbDatabase : RoomDatabase() {
    abstract fun tibbDao(): TibbDao

    companion object {
        @Volatile
        private var INSTANCE: TibbDatabase? = null

        fun getInstance(context: Context): TibbDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TibbDatabase::class.java,
                    "tibb_vault.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
