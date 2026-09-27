package com.example.eyewearagentapp.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val createdAt: Long,
    val durationSeconds: Int,
    val mode: String, // "MEETING" or "MEMO"
    val transcription: String? = null,
    val translation: String? = null
)
