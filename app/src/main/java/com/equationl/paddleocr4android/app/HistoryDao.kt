package com.equationl.paddleocr4android.app

import androidx.room.*

@Dao
interface HistoryDao {
    @Query("SELECT * FROM ocr_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Insert
    suspend fun insert(entity: HistoryEntity)

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM ocr_history")
    suspend fun deleteAll()
}
