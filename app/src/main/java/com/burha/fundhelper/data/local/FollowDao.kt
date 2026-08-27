package com.burha.fundhelper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE code = :code")
    suspend fun delete(code: String)

    @Query("DELETE FROM follows")
    suspend fun deleteAll()

    @Query("SELECT code FROM follows ORDER BY code")
    suspend fun getCodes(): List<String>

    @Transaction
    @Query("SELECT * FROM follows ORDER BY code")
    fun observeFollowed(): Flow<List<FollowedFund>>
}
