package com.example.blue.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeDao {
    @Query("SELECT birthday FROM time_profiles WHERE id = 0 LIMIT 1")
    fun observeBirthday(): Flow<java.time.LocalDate?>

    @Query("SELECT * FROM time_profiles WHERE id = 0 LIMIT 1")
    suspend fun getProfile(): TimeProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: TimeProfileEntity)

    @Query("SELECT * FROM time_events ORDER BY eventDate, createdAt")
    fun observeEvents(): Flow<List<TimeEventEntity>>

    @Query("SELECT * FROM time_events WHERE id = :id LIMIT 1")
    fun observeEvent(id: String): Flow<TimeEventEntity?>

    @Query("SELECT * FROM time_events WHERE id = :id LIMIT 1")
    suspend fun getEvent(id: String): TimeEventEntity?

    @Query("SELECT * FROM time_events ORDER BY eventDate, createdAt")
    suspend fun getAllEvents(): List<TimeEventEntity>

    @Upsert
    suspend fun upsertEvent(event: TimeEventEntity)

    @Query("DELETE FROM time_events WHERE id = :id")
    suspend fun deleteEvent(id: String): Int

    @Query("DELETE FROM time_events")
    suspend fun clearEvents()

    @Query("DELETE FROM time_profiles")
    suspend fun clearProfile()
}
