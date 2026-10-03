package com.mika.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY id DESC")
    fun getAllMemoriesFlow(): Flow<List<Memory>>

    @Query("SELECT * FROM memories ORDER BY id DESC")
    suspend fun getAllMemories(): List<Memory>

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemory(memory: Memory): Long

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memories WHERE id IN (SELECT id FROM memories ORDER BY id ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
