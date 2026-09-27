package com.example.eyewearagentapp.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingItem>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(item: RecordingItem): Long

    @Update
    suspend fun updateRecording(item: RecordingItem)

    @Delete
    suspend fun deleteRecording(item: RecordingItem)
}
