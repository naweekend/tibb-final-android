package com.example.data.local

import android.content.Context
import android.os.Build
import com.example.data.model.AccountRecord
import com.example.data.model.AuditLogEntry
import com.example.data.model.BackupPayloadItem
import com.example.data.model.ChatThread
import com.example.data.model.DailyWellnessLog
import com.example.data.model.EncryptedBackupFile
import com.example.data.model.EncryptedEnvelope
import com.example.data.model.PatientProfile
import com.example.data.model.SavedPersonReport
import com.example.data.model.StoredAlarm
import com.example.data.model.StoredEpisode
import com.example.data.model.StoredLabRecord
import com.example.data.model.TriageTier
import com.example.data.model.WorkspaceMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.crypto.SecretKey

data class UnlockedVaultSession(
    val accountId: String,
    val displayName: String,
    val workspace: WorkspaceMode,
    val isDefaultUnpinned: Boolean,
    val aesKey: SecretKey,
)

class VaultRepository(
    private val context: Context,
    private val dao: TibbDao,
    private val moshi: Moshi,
) {
    companion object {
        const val DEFAULT_ACCOUNT_ID = "acc_local_family_default"
        private const val DEFAULT_PASSKEY = "tibb_local_default_vault_key_v1"
        private const val KIND_PROFILE = "PROFILE"
        private const val KIND_EPISODE = "EPISODE"
        private const val KIND_ALARM = "ALARM"
        private const val KIND_LAB = "LAB"
        private const val KIND_WELLNESS = "WELLNESS"
        private const val KIND_CHAT = "CHAT_THREAD"
        private const val KIND_REPORT = "REPORT"
    }

    private fun nowIso(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.now().toString()
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date())
        }
    }

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(10)}"

    suspend fun listCustomAccounts(): List<AccountRecord> = withContext(Dispatchers.IO) {
        dao.getAllAccounts()
            .filter { it.accountId != DEFAULT_ACCOUNT_ID }
            .map {
                AccountRecord(
                    accountId = it.accountId,
                    displayName = it.displayName,
                    workspace = if (it.workspace == WorkspaceMode.BHU.name) WorkspaceMode.BHU else WorkspaceMode.GHARANA,
                    pinSaltBase64 = it.pinSaltBase64,
                    pinVerifierBase64 = it.pinVerifierBase64,
                    failedAttempts = it.failedAttempts,
                    lockedUntilEpochMs = it.lockedUntilEpochMs,
                    createdAtIso = it.createdAtIso,
                )
            }
    }

    suspend fun openOrCreateDefaultSession(workspace: WorkspaceMode): UnlockedVaultSession =
        withContext(Dispatchers.IO) {
            val existing = dao.getAccount(DEFAULT_ACCOUNT_ID)
            val entity = if (existing == null) {
                val salt = VaultCrypto.generateSaltBase64()
                val created = AccountEntity(
                    accountId = DEFAULT_ACCOUNT_ID,
                    displayName = if (workspace == WorkspaceMode.BHU) "Local BHU Station" else "My Family Health",
                    workspace = workspace.name,
                    pinSaltBase64 = salt,
                    pinVerifierBase64 = VaultCrypto.deriveVerifierBase64("0000", salt),
                    createdAtIso = nowIso(),
                )
                dao.upsertAccount(created)
                created
            } else {
                existing
            }
            val key = VaultCrypto.deriveAesKey(DEFAULT_PASSKEY, entity.pinSaltBase64, iterations = 30_000)
            UnlockedVaultSession(
                accountId = entity.accountId,
                displayName = entity.displayName,
                workspace = if (entity.workspace == WorkspaceMode.BHU.name) WorkspaceMode.BHU else WorkspaceMode.GHARANA,
                isDefaultUnpinned = true,
                aesKey = key,
            )
        }

    suspend fun createPinAccount(
        displayName: String,
        pin: String,
        workspace: WorkspaceMode,
    ): Result<UnlockedVaultSession> = withContext(Dispatchers.IO) {
        if (!VaultCrypto.validatePinFormat(pin)) {
            return@withContext Result.failure(IllegalArgumentException("PIN must be 4 to 8 digits."))
        }
        val cleanName = displayName.trim().ifEmpty {
            if (workspace == WorkspaceMode.BHU) "BHU Station" else "Family Vault"
        }.take(60)
        val salt = VaultCrypto.generateSaltBase64()
        val verifier = VaultCrypto.deriveVerifierBase64(pin, salt)
        val key = VaultCrypto.deriveAesKey(pin, salt)
        val accountId = newId("acc")
        val entity = AccountEntity(
            accountId = accountId,
            displayName = cleanName,
            workspace = workspace.name,
            pinSaltBase64 = salt,
            pinVerifierBase64 = verifier,
            createdAtIso = nowIso(),
        )
        dao.upsertAccount(entity)
        val session = UnlockedVaultSession(
            accountId = accountId,
            displayName = cleanName,
            workspace = workspace,
            isDefaultUnpinned = false,
            aesKey = key,
        )
        logAudit(session.accountId, "VAULT_CREATED", "Created PIN-protected $workspace vault")
        Result.success(session)
    }

    suspend fun unlockPinAccount(accountId: String, pin: String): Result<UnlockedVaultSession> =
        withContext(Dispatchers.IO) {
            val entity = dao.getAccount(accountId)
                ?: return@withContext Result.failure(IllegalStateException("Account not found."))
            val nowMs = System.currentTimeMillis()
            if (entity.lockedUntilEpochMs > nowMs) {
                val secs = ((entity.lockedUntilEpochMs - nowMs) / 1000L).coerceAtLeast(1L)
                return@withContext Result.failure(
                    IllegalStateException("Too many wrong PIN attempts. Wait $secs seconds.")
                )
            }
            if (!VaultCrypto.validatePinFormat(pin)) {
                return@withContext Result.failure(IllegalArgumentException("Enter a 4 to 8 digit PIN."))
            }
            val candidate = VaultCrypto.deriveVerifierBase64(pin, entity.pinSaltBase64)
            if (candidate != entity.pinVerifierBase64) {
                val nextFailures = entity.failedAttempts + 1
                val lockMs = if (nextFailures >= 5) {
                    val backoffSec = (15L shl (nextFailures - 5).coerceAtMost(4))
                    nowMs + backoffSec * 1000L
                } else {
                    0L
                }
                dao.upsertAccount(entity.copy(failedAttempts = nextFailures, lockedUntilEpochMs = lockMs))
                return@withContext Result.failure(
                    IllegalArgumentException(
                        if (lockMs > 0L) "Incorrect PIN. Account temporarily locked."
                        else "Incorrect PIN (${5 - nextFailures.coerceAtMost(5)} tries remaining)."
                    )
                )
            }
            dao.upsertAccount(entity.copy(failedAttempts = 0, lockedUntilEpochMs = 0L))
            val key = VaultCrypto.deriveAesKey(pin, entity.pinSaltBase64)
            logAudit(entity.accountId, "VAULT_UNLOCKED", "PIN vault unlocked")
            Result.success(
                UnlockedVaultSession(
                    accountId = entity.accountId,
                    displayName = entity.displayName,
                    workspace = if (entity.workspace == WorkspaceMode.BHU.name) WorkspaceMode.BHU else WorkspaceMode.GHARANA,
                    isDefaultUnpinned = false,
                    aesKey = key,
                )
            )
        }

    suspend fun upgradeDefaultToPin(
        session: UnlockedVaultSession,
        displayName: String,
        pin: String,
        workspace: WorkspaceMode,
    ): Result<UnlockedVaultSession> = withContext(Dispatchers.IO) {
        val created = createPinAccount(displayName, pin, workspace)
        val newSession = created.getOrElse { return@withContext Result.failure(it) }
        // Migrate any existing items from default vault into the newly PIN-protected vault
        if (session.isDefaultUnpinned) {
            val profiles = loadProfiles(session)
            profiles.forEach { saveProfile(newSession, it.copy(accountId = newSession.accountId)) }
            loadEpisodes(session).forEach { saveEpisode(newSession, it) }
            loadAlarms(session).forEach { saveAlarm(newSession, it) }
            loadLabs(session).forEach { saveLab(newSession, it) }
            loadWellnessLogs(session).forEach { saveWellnessLog(newSession, it) }
            loadChatThreads(session).forEach { saveChatThread(newSession, it) }
            loadSavedReports(session).forEach { savePersonReport(newSession, it) }
        }
        Result.success(newSession)
    }

    private suspend inline fun <reified T> saveEncryptedItem(
        session: UnlockedVaultSession,
        kind: String,
        itemId: String,
        value: T,
    ) = withContext(Dispatchers.IO) {
        val adapter = moshi.adapter(T::class.java)
        val json = adapter.toJson(value)
        val env = VaultCrypto.encrypt(json, session.aesKey)
        dao.upsertVaultItem(
            VaultItemEntity(
                storageKey = "${session.accountId}::$kind::$itemId",
                accountId = session.accountId,
                kind = kind,
                itemId = itemId,
                updatedAtIso = nowIso(),
                ivBase64 = env.ivBase64,
                cipherBase64 = env.cipherBase64,
            )
        )
    }

    private suspend inline fun <reified T> loadEncryptedItems(
        session: UnlockedVaultSession,
        kind: String,
    ): List<T> = withContext(Dispatchers.IO) {
        val adapter = moshi.adapter(T::class.java)
        dao.getVaultItemsByKind(session.accountId, kind).mapNotNull { row ->
            runCatching {
                val plain = VaultCrypto.decrypt(
                    EncryptedEnvelope(row.ivBase64, row.cipherBase64),
                    session.aesKey,
                )
                adapter.fromJson(plain)
            }.getOrNull()
        }
    }

    suspend fun saveProfile(session: UnlockedVaultSession, profile: PatientProfile) {
        saveEncryptedItem(session, KIND_PROFILE, profile.id, profile)
        logAudit(session.accountId, "PROFILE_SAVED", "Updated profile ${profile.name}")
    }

    suspend fun loadProfiles(session: UnlockedVaultSession): List<PatientProfile> =
        loadEncryptedItems<PatientProfile>(session, KIND_PROFILE).sortedBy { it.createdAtIso }

    suspend fun saveEpisode(session: UnlockedVaultSession, episode: StoredEpisode) {
        saveEncryptedItem(session, KIND_EPISODE, episode.id, episode)
        logAudit(session.accountId, "EPISODE_LOGGED", "${episode.triageTier} · ${episode.symptomCategory}")
    }

    suspend fun loadEpisodes(session: UnlockedVaultSession): List<StoredEpisode> {
        val tierWeight = { t: TriageTier ->
            when (t) {
                TriageTier.RED -> 0
                TriageTier.AMBER -> 1
                TriageTier.GREEN -> 2
                TriageTier.BLUE -> 3
            }
        }
        return loadEncryptedItems<StoredEpisode>(session, KIND_EPISODE)
            .sortedWith(compareBy<StoredEpisode> { tierWeight(it.triageTier) }.thenByDescending { it.createdAtIso })
    }

    suspend fun saveAlarm(session: UnlockedVaultSession, alarm: StoredAlarm) {
        saveEncryptedItem(session, KIND_ALARM, alarm.id, alarm)
    }

    suspend fun loadAlarms(session: UnlockedVaultSession): List<StoredAlarm> =
        loadEncryptedItems<StoredAlarm>(session, KIND_ALARM).sortedBy { it.time24h }

    suspend fun saveLab(session: UnlockedVaultSession, lab: StoredLabRecord) {
        saveEncryptedItem(session, KIND_LAB, lab.id, lab)
        logAudit(session.accountId, "LAB_SAVED", "Lab scan (${lab.sourceFileType})")
    }

    suspend fun loadLabs(session: UnlockedVaultSession): List<StoredLabRecord> =
        loadEncryptedItems<StoredLabRecord>(session, KIND_LAB).sortedByDescending { it.scannedAtIso }

    suspend fun saveWellnessLog(session: UnlockedVaultSession, log: DailyWellnessLog) {
        saveEncryptedItem(session, KIND_WELLNESS, log.id, log)
    }

    suspend fun loadWellnessLogs(session: UnlockedVaultSession): List<DailyWellnessLog> =
        loadEncryptedItems<DailyWellnessLog>(session, KIND_WELLNESS).sortedByDescending { it.dateKey }

    suspend fun saveChatThread(session: UnlockedVaultSession, thread: ChatThread) {
        saveEncryptedItem(session, KIND_CHAT, thread.id, thread)
    }

    suspend fun loadChatThreads(session: UnlockedVaultSession): List<ChatThread> =
        loadEncryptedItems<ChatThread>(session, KIND_CHAT).sortedByDescending { it.updatedAtIso }

    suspend fun savePersonReport(session: UnlockedVaultSession, report: SavedPersonReport) {
        saveEncryptedItem(session, KIND_REPORT, report.id, report)
        logAudit(session.accountId, "REPORT_SAVED", "${report.kind}: ${report.title}")
    }

    suspend fun loadSavedReports(session: UnlockedVaultSession): List<SavedPersonReport> =
        loadEncryptedItems<SavedPersonReport>(session, KIND_REPORT).sortedByDescending { it.createdAtIso }

    suspend fun logAudit(accountId: String, action: String, detail: String) = withContext(Dispatchers.IO) {
        dao.insertAuditLog(
            AuditLogEntity(
                id = newId("aud"),
                accountId = accountId,
                action = action,
                detail = detail,
                timestampIso = nowIso(),
            )
        )
    }

    suspend fun getAuditLogs(accountId: String): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        dao.getAuditLogs(accountId).map {
            AuditLogEntry(
                id = it.id,
                accountId = it.accountId,
                action = it.action,
                detail = it.detail,
                timestampIso = it.timestampIso,
            )
        }
    }

    suspend fun exportEncryptedBackup(session: UnlockedVaultSession, passphrase: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (passphrase.trim().length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Backup passphrase must be at least 6 characters."))
            }
            val rows = dao.getAllVaultItemsForAccount(session.accountId)
            val payloadItems = rows.mapNotNull { row ->
                runCatching {
                    val plain = VaultCrypto.decrypt(
                        EncryptedEnvelope(row.ivBase64, row.cipherBase64),
                        session.aesKey,
                    )
                    BackupPayloadItem(
                        kind = row.kind,
                        id = row.itemId,
                        updatedAtIso = row.updatedAtIso,
                        payloadJson = plain,
                    )
                }.getOrNull()
            }
            val listType = Types.newParameterizedType(List::class.java, BackupPayloadItem::class.java)
            val rawPayloadJson = moshi.adapter<List<BackupPayloadItem>>(listType).toJson(payloadItems)
            val salt = VaultCrypto.generateSaltBase64()
            val backupKey = VaultCrypto.deriveAesKey(passphrase.trim(), salt)
            val env = VaultCrypto.encrypt(rawPayloadJson, backupKey)
            val wrapper = EncryptedBackupFile(
                accountDisplayName = session.displayName,
                workspace = session.workspace,
                exportedAtIso = nowIso(),
                saltBase64 = salt,
                ivBase64 = env.ivBase64,
                cipherBase64 = env.cipherBase64,
            )
            logAudit(session.accountId, "BACKUP_EXPORTED", "Exported ${payloadItems.size} encrypted records")
            Result.success(moshi.adapter(EncryptedBackupFile::class.java).toJson(wrapper))
        }

    suspend fun restoreEncryptedBackup(
        session: UnlockedVaultSession,
        backupJson: String,
        passphrase: String,
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (passphrase.trim().length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Enter the 6+ character backup passphrase."))
        }
        val wrapper = runCatching {
            moshi.adapter(EncryptedBackupFile::class.java).fromJson(backupJson)
        }.getOrNull() ?: return@withContext Result.failure(IllegalArgumentException("Invalid .tibb-backup file format."))

        val backupKey = VaultCrypto.deriveAesKey(passphrase.trim(), wrapper.saltBase64)
        val plain = runCatching {
            VaultCrypto.decrypt(EncryptedEnvelope(wrapper.ivBase64, wrapper.cipherBase64), backupKey)
        }.getOrElse {
            return@withContext Result.failure(IllegalArgumentException("Wrong backup passphrase or corrupted file."))
        }
        val listType = Types.newParameterizedType(List::class.java, BackupPayloadItem::class.java)
        val items = runCatching {
            moshi.adapter<List<BackupPayloadItem>>(listType).fromJson(plain)
        }.getOrNull() ?: return@withContext Result.failure(IllegalArgumentException("Could not parse backup records."))

        for (item in items) {
            val env = VaultCrypto.encrypt(item.payloadJson, session.aesKey)
            dao.upsertVaultItem(
                VaultItemEntity(
                    storageKey = "${session.accountId}::${item.kind}::${item.id}",
                    accountId = session.accountId,
                    kind = item.kind,
                    itemId = item.id,
                    updatedAtIso = item.updatedAtIso,
                    ivBase64 = env.ivBase64,
                    cipherBase64 = env.cipherBase64,
                )
            )
        }
        logAudit(session.accountId, "BACKUP_RESTORED", "Restored ${items.size} encrypted items")
        Result.success(items.size)
    }

    suspend fun wipeAccountData(accountId: String) = withContext(Dispatchers.IO) {
        dao.deleteAllVaultItemsForAccount(accountId)
        dao.deleteAuditLogsForAccount(accountId)
        dao.deleteAccount(accountId)
        val mediaDir = File(context.filesDir, "tibb_media/$accountId")
        if (mediaDir.exists()) mediaDir.deleteRecursively()
    }
}
