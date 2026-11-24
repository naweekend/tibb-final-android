package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val accountId: String,
    val displayName: String,
    val workspace: String,
    val pinSaltBase64: String,
    val pinVerifierBase64: String,
    val failedAttempts: Int = 0,
    val lockedUntilEpochMs: Long = 0L,
    val createdAtIso: String,
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val storageKey: String, // accountId::kind::id
    val accountId: String,
    val kind: String, // PROFILE, EPISODE, ALARM, LAB, WELLNESS, CHAT_THREAD, REPORT
    val itemId: String,
    val updatedAtIso: String,
    val ivBase64: String,
    val cipherBase64: String,
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val action: String,
    val detail: String,
    val timestampIso: String,
)
