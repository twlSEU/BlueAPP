package com.example.blue.data.repository

import com.example.blue.data.local.dao.TimeDao
import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeProfileEntity
import java.time.LocalDate
import java.time.Period
import kotlinx.coroutines.flow.Flow

class OfflineTimeRepository(
    private val timeDao: TimeDao,
) : TimeRepository {
    override fun observeBirthday(): Flow<LocalDate?> = timeDao.observeBirthday()

    override suspend fun saveBirthday(birthday: LocalDate) {
        require(!birthday.isAfter(LocalDate.now())) { "生日不能晚于今天" }
        val age = Period.between(birthday, LocalDate.now()).years.coerceIn(0, 80)
        timeDao.upsertProfile(TimeProfileEntity(age = age, birthday = birthday))
    }

    override fun observeEvents(): Flow<List<TimeEventEntity>> = timeDao.observeEvents()

    override fun observeEvent(id: String): Flow<TimeEventEntity?> = timeDao.observeEvent(id)

    override suspend fun getEvent(id: String): TimeEventEntity? = timeDao.getEvent(id)

    override suspend fun saveEvent(event: TimeEventEntity) {
        require(event.title.isNotBlank()) { "请填写事件名称" }
        timeDao.upsertEvent(event.copy(title = event.title.trim()))
    }

    override suspend fun deleteEvent(id: String) {
        timeDao.deleteEvent(id)
    }
}
