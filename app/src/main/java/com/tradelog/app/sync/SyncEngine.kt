package com.tradelog.app.sync

import com.tradelog.app.data.SyncStore
import com.tradelog.app.data.db.AppDatabase
import com.tradelog.app.data.entity.SyncMeta
import com.tradelog.app.network.SupabaseClient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

data class SyncResult(val pulled: Int = 0, val pushed: Int = 0, val error: String? = null) {
    val ok: Boolean get() = error == null
}

/**
 * Two-way sync between Room and Supabase for the 6 web-facing tables.
 * Strategy: last-write-wins via `updated_at`, soft-delete via tombstones in [SyncMeta].
 */
class SyncEngine(
    private val db: AppDatabase,
    private val client: SupabaseClient,
    private val store: SyncStore
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; encodeDefaults = true }

    private val meta = db.syncMetaDao()
    private val accountDao = db.accountDao()
    private val tradeDao = db.tradeDao()
    private val backtestDao = db.backtestDao()
    private val payoutDao = db.payoutDao()
    private val journalDao = db.journalDao()
    private val noteDao = db.notebookDao()
    private val instrumentDao = db.instrumentDao()

    @Volatile private var running = false

    /** Pull then push every table. Accounts go first so trades can resolve their account link. */
    suspend fun syncAll(): SyncResult {
        if (!store.current().isLoggedIn) return SyncResult(error = "Not signed in.")
        if (running) return SyncResult(error = "Sync already running.")
        running = true
        var pulled = 0
        var pushed = 0
        try {
            ensureLegacyRowsPending()

            pulled += pullAccounts()
            pulled += pullInstruments()
            pulled += pullTrades()
            pulled += pullBacktests()
            pulled += pullPayouts()
            pulled += pullJournal()
            pulled += pullNotes()

            pushed += pushAccounts()
            pushed += pushInstruments()
            pushed += pushTrades()
            pushed += pushBacktests()
            pushed += pushPayouts()
            pushed += pushJournal()
            pushed += pushNotes()

            store.setLastSync(System.currentTimeMillis())
            return SyncResult(pulled, pushed)
        } catch (e: Exception) {
            return SyncResult(pulled, pushed, error = e.message ?: "Sync failed.")
        } finally {
            running = false
        }
    }

    // ---------------- shared meta helpers ----------------

    private suspend fun cursorQuery(table: String): String {
        val cursor = store.pullCursor(table)
        return "select=*&updated_at=gt.$cursor&order=updated_at.asc"
    }

    /** Should the incoming remote row be applied, given the local mapping? */
    private fun shouldApply(local: SyncMeta?, remoteUpdated: Long): Boolean {
        if (local == null) return true
        // A local unpushed change that is newer (or same age) wins.
        if (local.pending && local.updatedAt >= remoteUpdated) return false
        return true
    }

    private suspend fun advanceCursor(table: String, remoteUpdated: Long) {
        if (remoteUpdated > store.pullCursor(table)) store.setPullCursor(table, remoteUpdated)
    }

    /** Record/refresh the uid → localId mapping after applying a non-deleted remote row. */
    private suspend fun upsertMeta(table: String, uid: String, localId: Long, remoteUpdated: Long) {
        val existing = meta.byUid(uid)
        if (existing == null) {
            meta.insert(SyncMeta(tableName = table, localId = localId, uid = uid, updatedAt = remoteUpdated, deleted = false, pending = false))
        } else {
            meta.update(existing.copy(localId = localId, updatedAt = remoteUpdated, deleted = false, pending = false))
        }
        advanceCursor(table, remoteUpdated)
    }

    /** Mark an existing local mapping as deleted after the remote row was tombstoned. */
    private suspend fun markMetaDeleted(local: SyncMeta, remoteUpdated: Long) {
        meta.update(local.copy(updatedAt = remoteUpdated, deleted = true, pending = false))
    }

    private fun finalLocalId(local: SyncMeta?, insertedId: Long): Long =
        if (local != null && local.localId != 0L) local.localId else insertedId

    /**
     * Older offline records can predate sync_meta. Give them cloud identities before the
     * first push so existing phone accounts sync, and trades keep their account link.
     */
    private suspend fun ensureLegacyRowsPending() {
        val now = System.currentTimeMillis()
        ensurePendingMeta(SyncTables.ACCOUNTS, accountDao.getAll().map { it.id }, now)
        ensurePendingMeta(SyncTables.TRADES, tradeDao.getAllAsc().map { it.id }, now)
        ensurePendingMeta(SyncTables.BACKTESTS, backtestDao.getAll().map { it.id }, now)
        ensurePendingMeta(SyncTables.PAYOUTS, payoutDao.getAll().map { it.id }, now)
        ensurePendingMeta(SyncTables.JOURNAL, journalDao.getAll().map { it.id }, now)
        ensurePendingMeta(SyncTables.NOTES, noteDao.getAll().map { it.id }, now)
        ensurePendingMeta(SyncTables.INSTRUMENTS, instrumentDao.getAll().map { it.id }, now)
    }

    private suspend fun ensurePendingMeta(table: String, localIds: List<Long>, updatedAt: Long) {
        localIds.filter { it > 0L }.forEach { localId ->
            if (meta.byLocal(table, localId) == null) {
                meta.insert(
                    SyncMeta(
                        tableName = table,
                        localId = localId,
                        uid = UUID.randomUUID().toString(),
                        updatedAt = updatedAt,
                        deleted = false,
                        pending = true
                    )
                )
            }
        }
    }

    // ---------------- PULL ----------------

    private suspend fun pullAccounts(): Int {
        val text = client.getRows(SyncTables.ACCOUNTS, cursorQuery(SyncTables.ACCOUNTS))
        val rows = json.decodeFromString(ListSerializer(AccountDto.serializer()), text)
        var n = 0
        for (r in rows) {
            val local = meta.byUid(r.uid)
            if (!shouldApply(local, r.updatedAt)) continue
            if (r.deleted) {
                if (local != null) {
                    accountDao.getById(local.localId)?.let { accountDao.delete(it) }
                    markMetaDeleted(local, r.updatedAt)
                }
            } else {
                val id = accountDao.upsert(r.toEntity(local?.localId ?: 0))
                upsertMeta(SyncTables.ACCOUNTS, r.uid, finalLocalId(local, id), r.updatedAt)
            }
            advanceCursor(SyncTables.ACCOUNTS, r.updatedAt)
            n++
        }
        return n
    }

    private suspend fun pullTrades(): Int {
        val text = client.getRows(SyncTables.TRADES, cursorQuery(SyncTables.TRADES))
        val rows = json.decodeFromString(ListSerializer(TradeDto.serializer()), text)
        val fallbackAccountId = accountDao.getAll().firstOrNull()?.id
        var n = 0
        for (r in rows) {
            val local = meta.byUid(r.uid)
            if (!shouldApply(local, r.updatedAt)) continue
            if (r.deleted) {
                if (local != null) {
                    tradeDao.getById(local.localId)?.let { tradeDao.delete(it) }
                    markMetaDeleted(local, r.updatedAt)
                }
            } else {
                val accountId = r.accountUid?.let { meta.byUid(it)?.localId } ?: fallbackAccountId
                val entity = r.toEntity(local?.localId ?: 0, accountId)
                val id = if (entity.id == 0L) tradeDao.insert(entity) else { tradeDao.update(entity); entity.id }
                upsertMeta(SyncTables.TRADES, r.uid, id, r.updatedAt)
            }
            advanceCursor(SyncTables.TRADES, r.updatedAt)
            n++
        }
        return n
    }

    private suspend fun pullInstruments(): Int {
        val text = client.getRows(SyncTables.INSTRUMENTS, cursorQuery(SyncTables.INSTRUMENTS))
        val rows = json.decodeFromString(ListSerializer(InstrumentDto.serializer()), text)
        var n = 0
        for (r in rows) {
            val local = meta.byUid(r.uid)
            if (!shouldApply(local, r.updatedAt)) continue
            if (r.deleted) {
                if (local != null) {
                    instrumentDao.getById(local.localId)?.let { instrumentDao.delete(it) }
                    markMetaDeleted(local, r.updatedAt)
                }
            } else {
                val targetId = local?.localId
                    ?: instrumentDao.getByName(r.name)?.takeIf { meta.byLocal(SyncTables.INSTRUMENTS, it.id) == null }?.id
                    ?: 0L
                val id = instrumentDao.upsert(r.toEntity(targetId))
                upsertMeta(SyncTables.INSTRUMENTS, r.uid, if (targetId != 0L) targetId else finalLocalId(local, id), r.updatedAt)
            }
            advanceCursor(SyncTables.INSTRUMENTS, r.updatedAt)
            n++
        }
        return n
    }

    private suspend fun pullBacktests(): Int {
        val text = client.getRows(SyncTables.BACKTESTS, cursorQuery(SyncTables.BACKTESTS))
        val rows = json.decodeFromString(ListSerializer(BacktestDto.serializer()), text)
        var n = 0
        for (r in rows) {
            val local = meta.byUid(r.uid)
            if (!shouldApply(local, r.updatedAt)) continue
            if (r.deleted) {
                if (local != null) {
                    backtestDao.getById(local.localId)?.let { backtestDao.delete(it) }
                    markMetaDeleted(local, r.updatedAt)
                }
            } else {
                val id = backtestDao.upsert(r.toEntity(local?.localId ?: 0))
                upsertMeta(SyncTables.BACKTESTS, r.uid, finalLocalId(local, id), r.updatedAt)
            }
            advanceCursor(SyncTables.BACKTESTS, r.updatedAt)
            n++
        }
        return n
    }

    private suspend fun pullPayouts(): Int {
        val text = client.getRows(SyncTables.PAYOUTS, cursorQuery(SyncTables.PAYOUTS))
        val rows = json.decodeFromString(ListSerializer(PayoutDto.serializer()), text)
        var n = 0
        for (r in rows) {
            val local = meta.byUid(r.uid)
            if (!shouldApply(local, r.updatedAt)) continue
            if (r.deleted) {
                if (local != null) {
                    payoutDao.getById(local.localId)?.let { payoutDao.delete(it) }
                    markMetaDeleted(local, r.updatedAt)
                }
            } else {
                val id = payoutDao.upsert(r.toEntity(local?.localId ?: 0))
                upsertMeta(SyncTables.PAYOUTS, r.uid, finalLocalId(local, id), r.updatedAt)
            }
            advanceCursor(SyncTables.PAYOUTS, r.updatedAt)
            n++
        }
        return n
    }

    private suspend fun pullJournal(): Int {
        val text = client.getRows(SyncTables.JOURNAL, cursorQuery(SyncTables.JOURNAL))
        val rows = json.decodeFromString(ListSerializer(JournalDto.serializer()), text)
        var n = 0
        for (r in rows) {
            val local = meta.byUid(r.uid)
            if (!shouldApply(local, r.updatedAt)) continue
            if (r.deleted) {
                if (local != null) {
                    journalDao.getById(local.localId)?.let { journalDao.delete(it) }
                    markMetaDeleted(local, r.updatedAt)
                }
            } else {
                // Adopt an existing same-date entry that has no cloud identity yet (avoids the unique-date clash).
                val targetId = local?.localId
                    ?: journalDao.getByDate(r.date)?.takeIf { meta.byLocal(SyncTables.JOURNAL, it.id) == null }?.id
                    ?: 0L
                val entity = r.toEntity(targetId)
                val id = if (entity.id == 0L) journalDao.insert(entity) else { journalDao.update(entity); entity.id }
                upsertMeta(SyncTables.JOURNAL, r.uid, id, r.updatedAt)
            }
            advanceCursor(SyncTables.JOURNAL, r.updatedAt)
            n++
        }
        return n
    }

    private suspend fun pullNotes(): Int {
        val text = client.getRows(SyncTables.NOTES, cursorQuery(SyncTables.NOTES))
        val rows = json.decodeFromString(ListSerializer(NoteDto.serializer()), text)
        var n = 0
        for (r in rows) {
            val local = meta.byUid(r.uid)
            if (!shouldApply(local, r.updatedAt)) continue
            if (r.deleted) {
                if (local != null) {
                    noteDao.getById(local.localId)?.let { noteDao.delete(it) }
                    markMetaDeleted(local, r.updatedAt)
                }
            } else {
                val id = noteDao.upsert(r.toEntity(local?.localId ?: 0))
                upsertMeta(SyncTables.NOTES, r.uid, finalLocalId(local, id), r.updatedAt)
            }
            advanceCursor(SyncTables.NOTES, r.updatedAt)
            n++
        }
        return n
    }

    // ---------------- PUSH ----------------

    private fun deletePayload(uid: String, updatedAt: Long): String =
        json.encodeToString(
            ListSerializer(JsonObject.serializer()),
            listOf(buildJsonObject {
                put("uid", JsonPrimitive(uid))
                put("deleted", JsonPrimitive(true))
                put("updated_at", JsonPrimitive(updatedAt))
            })
        )

    private suspend fun <T> pushOne(table: String, m: SyncMeta, serializer: kotlinx.serialization.KSerializer<T>, dto: T) {
        val body = json.encodeToString(ListSerializer(serializer), listOf(dto))
        client.upsert(table, body)
        meta.markSynced(m.id)
    }

    private suspend fun pushDeleted(table: String, m: SyncMeta) {
        client.upsert(table, deletePayload(m.uid, m.updatedAt))
        meta.markSynced(m.id)
    }

    private fun httpOrNull(uri: String?): String? =
        uri?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

    private suspend fun pushAccounts(): Int {
        var n = 0
        for (m in meta.pendingFor(SyncTables.ACCOUNTS)) {
            if (m.deleted) pushDeleted(SyncTables.ACCOUNTS, m)
            else {
                val e = accountDao.getById(m.localId) ?: continue
                pushOne(SyncTables.ACCOUNTS, m, AccountDto.serializer(), e.toDto(m.uid, m.updatedAt, false))
            }
            n++
        }
        return n
    }

    private suspend fun pushTrades(): Int {
        var n = 0
        val fallbackAccountId = accountDao.getAll().firstOrNull()?.id
        for (m in meta.pendingFor(SyncTables.TRADES)) {
            if (m.deleted) pushDeleted(SyncTables.TRADES, m)
            else {
                val e = tradeDao.getById(m.localId) ?: continue
                val accountId = e.accountId ?: fallbackAccountId
                val accountUid = accountId?.let { meta.byLocal(SyncTables.ACCOUNTS, it)?.uid }
                pushOne(SyncTables.TRADES, m, TradeDto.serializer(), e.toDto(m.uid, accountUid, httpOrNull(e.screenshotUri), m.updatedAt, false))
            }
            n++
        }
        return n
    }

    private suspend fun pushInstruments(): Int {
        var n = 0
        for (m in meta.pendingFor(SyncTables.INSTRUMENTS)) {
            if (m.deleted) pushDeleted(SyncTables.INSTRUMENTS, m)
            else {
                val e = instrumentDao.getById(m.localId) ?: continue
                pushOne(SyncTables.INSTRUMENTS, m, InstrumentDto.serializer(), e.toDto(m.uid, m.updatedAt, false))
            }
            n++
        }
        return n
    }

    private suspend fun pushBacktests(): Int {
        var n = 0
        for (m in meta.pendingFor(SyncTables.BACKTESTS)) {
            if (m.deleted) pushDeleted(SyncTables.BACKTESTS, m)
            else {
                val e = backtestDao.getById(m.localId) ?: continue
                pushOne(SyncTables.BACKTESTS, m, BacktestDto.serializer(), e.toDto(m.uid, m.updatedAt, false))
            }
            n++
        }
        return n
    }

    private suspend fun pushPayouts(): Int {
        var n = 0
        for (m in meta.pendingFor(SyncTables.PAYOUTS)) {
            if (m.deleted) pushDeleted(SyncTables.PAYOUTS, m)
            else {
                val e = payoutDao.getById(m.localId) ?: continue
                pushOne(SyncTables.PAYOUTS, m, PayoutDto.serializer(), e.toDto(m.uid, m.updatedAt, false))
            }
            n++
        }
        return n
    }

    private suspend fun pushJournal(): Int {
        var n = 0
        for (m in meta.pendingFor(SyncTables.JOURNAL)) {
            if (m.deleted) pushDeleted(SyncTables.JOURNAL, m)
            else {
                val e = journalDao.getById(m.localId) ?: continue
                pushOne(SyncTables.JOURNAL, m, JournalDto.serializer(), e.toDto(m.uid, m.updatedAt, false))
            }
            n++
        }
        return n
    }

    private suspend fun pushNotes(): Int {
        var n = 0
        for (m in meta.pendingFor(SyncTables.NOTES)) {
            if (m.deleted) pushDeleted(SyncTables.NOTES, m)
            else {
                val e = noteDao.getById(m.localId) ?: continue
                pushOne(SyncTables.NOTES, m, NoteDto.serializer(), e.toDto(m.uid, m.updatedAt, false))
            }
            n++
        }
        return n
    }
}
