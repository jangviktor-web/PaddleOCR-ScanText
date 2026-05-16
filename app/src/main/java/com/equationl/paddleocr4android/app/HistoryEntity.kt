package com.equationl.paddleocr4android.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ocr_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    val text: String,
    val language: String,
    val timestamp: Long = System.currentTimeMillis()
)
