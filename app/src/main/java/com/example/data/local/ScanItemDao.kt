package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanItemDao {

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanItemEntity>>

    @Query("SELECT * FROM scan_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteHistory(): Flow<List<ScanItemEntity>>

    @Query("SELECT * FROM scan_history WHERE type = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: String): Flow<List<ScanItemEntity>>

    @Query("""
        SELECT * FROM scan_history 
        WHERE title LIKE '%' || :query || '%' 
           OR subtitle LIKE '%' || :query || '%' 
           OR rawValue LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchHistory(query: String): Flow<List<ScanItemEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScans(limit: Int = 10): Flow<List<ScanItemEntity>>

    @Query("SELECT * FROM scan_history WHERE rawValue = :rawValue ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestByRawValue(rawValue: String): ScanItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScanItemEntity): Long

    @Update
    suspend fun update(item: ScanItemEntity)

    @Query("UPDATE scan_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE scan_history SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long)

    @Delete
    suspend fun delete(item: ScanItemEntity)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}
