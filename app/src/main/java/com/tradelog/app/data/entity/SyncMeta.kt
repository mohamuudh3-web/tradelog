package com.tradelog.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Side-table that maps a local Room row to its cloud identity, without having to
 * alter every entity. One row per (table, localId).
 *
 *  - [uid]        : stable cross-device id shared with Supabase (text UUID).
 *  - [updatedAt]  : epoch millis of the last local change (used for last-write-wins).
 *  - [deleted]    : tombstone — the local row was deleted; the deletion still needs
 *                   to propagate to the cloud (and stays so we don't re-pull it).
 *  - [pending]    : there is a local change not yet pushed to the cloud.
 */
@Entity(
    tableName = "sync_meta",
    indices = [
        Index(value = ["tableName", "localId"], unique = true),
        Index(value = ["uid"], unique = true)
    ]
)
data class SyncMeta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    /** Local Room row id. 0 when the row was deleted and only the tombstone remains. */
    val localId: Long,
    val uid: String,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false,
    val pending: Boolean = true
)
