package com.example.blue.data.repository

import com.example.blue.data.local.entity.TimeEventEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TimeRepository {
    fun observeBirthday(): Flow<LocalDate?>

    suspend fun saveBirthday(birthday: LocalDate)

    fun observeEvents(): Flow<List<TimeEventEntity>>

    fun observeEvent(id: String): Flow<TimeEventEntity?>

    suspend fun getEvent(id: String): TimeEventEntity?

    suspend fun saveEvent(event: TimeEventEntity)

    suspend fun deleteEvent(id: String)
}
