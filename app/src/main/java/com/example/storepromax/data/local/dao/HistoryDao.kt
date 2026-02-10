package com.example.storepromax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.storepromax.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM view_history ORDER BY viewedAt DESC")
    fun getViewHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Query("DELETE FROM view_history")
    suspend fun clearHistory()

    @Query("DELETE FROM view_history WHERE id NOT IN (SELECT id FROM view_history ORDER BY viewedAt DESC LIMIT 20)")
    suspend fun deleteOldHistory()
}