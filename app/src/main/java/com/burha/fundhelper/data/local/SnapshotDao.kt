package com.burha.fundhelper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SnapshotEntity>)

    @Query("SELECT * FROM snapshots WHERE code = :code")
    fun observe(code: String): Flow<SnapshotEntity?>

    @Query("SELECT * FROM snapshots WHERE code = :code")
    suspend fun get(code: String): SnapshotEntity?
}
