package com.tradelog.app.sync

import com.tradelog.app.data.entity.Account
import com.tradelog.app.data.entity.Backtest
import com.tradelog.app.data.entity.Direction
import com.tradelog.app.data.entity.JournalEntry
import com.tradelog.app.data.entity.NotebookNote
import com.tradelog.app.data.entity.PayoutRecord
import com.tradelog.app.data.entity.PayoutStatus
import com.tradelog.app.data.entity.Trade
import com.tradelog.app.data.entity.TradeResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models matching the Supabase tables (snake_case). They are the bridge the web
 * app also writes to, so the column names must match `supabase/schema.sql` exactly.
 * `user_id` is intentionally omitted — Postgres defaults it to auth.uid() on insert.
 */

private fun directionOf(value: String?): Direction =
    runCatching { Direction.valueOf((value ?: "LONG").trim().uppercase()) }.getOrDefault(Direction.LONG)

private fun resultOf(value: String?): TradeResult =
    runCatching { TradeResult.valueOf((value ?: "BREAKEVEN").trim().uppercase()) }.getOrDefault(TradeResult.BREAKEVEN)

private fun payoutStatusOf(value: String?): PayoutStatus =
    if ((value ?: "").trim().uppercase() == "PAID") PayoutStatus.PAID else PayoutStatus.PENDING

@Serializable
data class AccountDto(
    val uid: String,
    val name: String = "",
    val broker: String = "",
    val balance: Double = 0.0,
    val currency: String = "USD",
    @SerialName("is_prop_firm") val isPropFirm: Boolean = false,
    @SerialName("challenge_phase") val challengePhase: String = "",
    val status: String = "",
    val website: String = "",
    @SerialName("starting_balance") val startingBalance: Double? = null,
    @SerialName("split_percent") val splitPercent: Double? = null,
    @SerialName("drawdown_percent") val drawdownPercent: Double? = null,
    @SerialName("target_percent") val targetPercent: Double? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    val deleted: Boolean = false
) {
    fun toEntity(localId: Long) = Account(
        id = localId, name = name, broker = broker, balance = balance, currency = currency,
        isPropFirm = isPropFirm, challengePhase = challengePhase, status = status, website = website,
        startingBalance = startingBalance, splitPercent = splitPercent, drawdownPercent = drawdownPercent,
        targetPercent = targetPercent, createdAt = createdAt
    )
}

fun Account.toDto(uid: String, updatedAt: Long, deleted: Boolean) = AccountDto(
    uid = uid, name = name, broker = broker, balance = balance, currency = currency,
    isPropFirm = isPropFirm, challengePhase = challengePhase, status = status, website = website,
    startingBalance = startingBalance, splitPercent = splitPercent, drawdownPercent = drawdownPercent,
    targetPercent = targetPercent, createdAt = createdAt, updatedAt = updatedAt, deleted = deleted
)

@Serializable
data class TradeDto(
    val uid: String,
    @SerialName("account_uid") val accountUid: String? = null,
    val instrument: String = "",
    val direction: String = "LONG",
    @SerialName("entry_price") val entryPrice: Double = 0.0,
    @SerialName("exit_price") val exitPrice: Double? = null,
    @SerialName("lot_size") val lotSize: Double = 0.0,
    @SerialName("risk_percent") val riskPercent: Double? = null,
    @SerialName("r_multiple") val rMultiple: Double? = null,
    val result: String = "BREAKEVEN",
    val pnl: Double = 0.0,
    @SerialName("setup_tag") val setupTag: String? = null,
    val session: String = "",
    @SerialName("sl_pips") val slPips: Double? = null,
    @SerialName("tp_pips") val tpPips: Double? = null,
    val psychology: String = "",
    @SerialName("checked_rules") val checkedRules: String = "",
    @SerialName("image_urls") val imageUrls: String = "",
    val notes: String = "",
    @SerialName("screenshot_url") val screenshotUrl: String? = null,
    @SerialName("opened_at") val openedAt: Long = 0,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    val deleted: Boolean = false
) {
    fun toEntity(localId: Long, accountId: Long?) = Trade(
        id = localId, accountId = accountId, instrument = instrument, direction = directionOf(direction),
        entryPrice = entryPrice, exitPrice = exitPrice, lotSize = lotSize, riskPercent = riskPercent,
        rMultiple = rMultiple, result = resultOf(result), pnl = pnl, setupTag = setupTag, session = session,
        slPips = slPips, tpPips = tpPips, psychology = psychology, checkedRules = checkedRules,
        imageUrls = imageUrls, notes = notes, screenshotUri = screenshotUrl, openedAt = openedAt,
        createdAt = createdAt
    )
}

fun Trade.toDto(uid: String, accountUid: String?, screenshotUrl: String?, updatedAt: Long, deleted: Boolean) = TradeDto(
    uid = uid, accountUid = accountUid, instrument = instrument, direction = direction.name,
    entryPrice = entryPrice, exitPrice = exitPrice, lotSize = lotSize, riskPercent = riskPercent,
    rMultiple = rMultiple, result = result.name, pnl = pnl, setupTag = setupTag, session = session,
    slPips = slPips, tpPips = tpPips, psychology = psychology, checkedRules = checkedRules,
    imageUrls = imageUrls, notes = notes, screenshotUrl = screenshotUrl, openedAt = openedAt,
    createdAt = createdAt, updatedAt = updatedAt, deleted = deleted
)

@Serializable
data class BacktestDto(
    val uid: String,
    val title: String = "",
    val instrument: String = "",
    @SerialName("date_millis") val dateMillis: Long = 0,
    val bias: String = "",
    val direction: String = "",
    val result: String = "",
    val session: String = "",
    @SerialName("sl_pips") val slPips: Double? = null,
    @SerialName("tp_pips") val tpPips: Double? = null,
    @SerialName("checked_rules") val checkedRules: String = "",
    @SerialName("image_urls") val imageUrls: String = "",
    @SerialName("chart5_url") val chart5Url: String = "",
    @SerialName("chart15_url") val chart15Url: String = "",
    val notes: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    val deleted: Boolean = false
) {
    fun toEntity(localId: Long) = Backtest(
        id = localId, title = title, instrument = instrument, dateMillis = dateMillis, bias = bias,
        direction = direction, result = result, session = session, slPips = slPips, tpPips = tpPips,
        checkedRules = checkedRules, imageUrls = imageUrls, chart5Url = chart5Url, chart15Url = chart15Url,
        notes = notes, createdAt = createdAt
    )
}

fun Backtest.toDto(uid: String, updatedAt: Long, deleted: Boolean) = BacktestDto(
    uid = uid, title = title, instrument = instrument, dateMillis = dateMillis, bias = bias,
    direction = direction, result = result, session = session, slPips = slPips, tpPips = tpPips,
    checkedRules = checkedRules, imageUrls = imageUrls, chart5Url = chart5Url, chart15Url = chart15Url,
    notes = notes, createdAt = createdAt, updatedAt = updatedAt, deleted = deleted
)

@Serializable
data class PayoutDto(
    val uid: String,
    val date: String = "",
    @SerialName("account_name") val accountName: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val status: String = "PENDING",
    val notes: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    val deleted: Boolean = false
) {
    fun toEntity(localId: Long) = PayoutRecord(
        id = localId, date = date, accountName = accountName, amount = amount, currency = currency,
        status = payoutStatusOf(status), notes = notes, createdAt = createdAt
    )
}

fun PayoutRecord.toDto(uid: String, updatedAt: Long, deleted: Boolean) = PayoutDto(
    uid = uid, date = date, accountName = accountName, amount = amount, currency = currency,
    status = status.name, notes = notes, createdAt = createdAt, updatedAt = updatedAt, deleted = deleted
)

@Serializable
data class JournalDto(
    val uid: String,
    val date: String = "",
    val mindset: String = "",
    val routine: String = "",
    val reflection: String = "",
    val mood: Int = 3,
    val discipline: Int = 3,
    val title: String = "",
    val gratitude: String = "",
    @SerialName("battle_plan") val battlePlan: String = "",
    val affirmation: String = "",
    val tags: String = "",
    @SerialName("mood_label") val moodLabel: String = "",
    @SerialName("focus_tasks") val focusTasks: String = "",
    @SerialName("account_balance") val accountBalance: Double? = null,
    @SerialName("trades_target") val tradesTarget: Int? = null,
    @SerialName("pips_target") val pipsTarget: Double? = null,
    @SerialName("risk_percent") val riskPercent: Double? = null,
    @SerialName("risk_amount") val riskAmount: Double? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    val deleted: Boolean = false
) {
    fun toEntity(localId: Long) = JournalEntry(
        id = localId, date = date, mindset = mindset, routine = routine, reflection = reflection,
        mood = mood, discipline = discipline, title = title, gratitude = gratitude, battlePlan = battlePlan,
        affirmation = affirmation, tags = tags, moodLabel = moodLabel, focusTasks = focusTasks,
        accountBalance = accountBalance, tradesTarget = tradesTarget, pipsTarget = pipsTarget,
        riskPercent = riskPercent, riskAmount = riskAmount, createdAt = createdAt
    )
}

fun JournalEntry.toDto(uid: String, updatedAt: Long, deleted: Boolean) = JournalDto(
    uid = uid, date = date, mindset = mindset, routine = routine, reflection = reflection,
    mood = mood, discipline = discipline, title = title, gratitude = gratitude, battlePlan = battlePlan,
    affirmation = affirmation, tags = tags, moodLabel = moodLabel, focusTasks = focusTasks,
    accountBalance = accountBalance, tradesTarget = tradesTarget, pipsTarget = pipsTarget,
    riskPercent = riskPercent, riskAmount = riskAmount, createdAt = createdAt, updatedAt = updatedAt,
    deleted = deleted
)

@Serializable
data class NoteDto(
    val uid: String,
    val title: String = "",
    val body: String = "",
    val tags: String = "",
    @SerialName("updated_at") val updatedAt: Long = 0,
    val deleted: Boolean = false
) {
    fun toEntity(localId: Long) = NotebookNote(
        id = localId, title = title, body = body, tags = tags, updatedAt = updatedAt
    )
}

fun NotebookNote.toDto(uid: String, updatedAt: Long, deleted: Boolean) = NoteDto(
    uid = uid, title = title, body = body, tags = tags, updatedAt = updatedAt, deleted = deleted
)

/** Table name constants shared by the engine and repository hooks. */
object SyncTables {
    const val ACCOUNTS = "accounts"
    const val TRADES = "trades"
    const val BACKTESTS = "backtests"
    const val PAYOUTS = "payouts"
    const val JOURNAL = "journal_entries"
    const val NOTES = "notes"
}
