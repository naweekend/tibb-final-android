package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TibbDao {
    @Query("SELECT * FROM accounts ORDER BY createdAtIso ASC")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE accountId = :accountId LIMIT 1")
    suspend fun getAccount(accountId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE accountId = :accountId")
    suspend fun deleteAccount(accountId: String)

    @Query("SELECT * FROM vault_items WHERE accountId = :accountId AND kind = :kind")
    suspend fun getVaultItemsByKind(accountId: String, kind: String): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items WHERE accountId = :accountId")
    suspend fun getAllVaultItemsForAccount(accountId: String): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVaultItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE accountId = :accountId")
    suspend fun deleteAllVaultItemsForAccount(accountId: String)

    @Query("SELECT * FROM audit_logs WHERE accountId = :accountId ORDER BY timestampIso DESC LIMIT 50")
    suspend fun getAuditLogs(accountId: String): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs WHERE accountId = :accountId")
    suspend fun deleteAuditLogsForAccount(accountId: String)
}
