package com.tradelog.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tradelog.app.data.entity.SyncMeta

@Dao
interface SyncMetaDao {

    @Query("SELECT * FROM sync_meta WHERE tableName = :table AND localId = :localId LIMIT 1")
    suspend fun byLocal(table: String, localId: Long): SyncMeta?

    @Query("SELECT * FROM sync_meta WHERE uid = :uid LIMIT 1")
    suspend fun byUid(uid: String): SyncMeta?

    @Query("SELECT * FROM sync_meta WHERE tableName = :table AND pending = 1")
    suspend fun pendingFor(table: String): List<SyncMeta>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meta: SyncMeta): Long

    @Update
    suspend fun update(meta: SyncMeta)

    @Query("UPDATE sync_meta SET pending = 0 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM sync_meta WHERE tableName = :table AND localId = :localId")
    suspend fun deleteByLocal(table: String, localId: Long)
}
