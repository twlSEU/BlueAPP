package com.example.blue.data.repository

import com.example.blue.data.local.dao.RecordDao
import com.example.blue.data.local.entity.NumericRecordEntity
import com.example.blue.data.local.entity.RecordTypeEntity
import com.example.blue.data.local.entity.RecordTypeOverview
import kotlinx.coroutines.flow.Flow

interface RecordRepository {
    fun observeTypes(): Flow<List<RecordTypeOverview>>
    fun observeType(id: String): Flow<RecordTypeEntity?>
    fun observeRecords(typeId: String): Flow<List<NumericRecordEntity>>
    suspend fun getType(id: String): RecordTypeEntity?
    suspend fun getRecord(id: String): NumericRecordEntity?
    suspend fun saveType(type: RecordTypeEntity)
    suspend fun saveRecord(record: NumericRecordEntity)
    suspend fun deleteType(id: String)
    suspend fun deleteRecord(id: String)
}

internal fun validateRecordType(type: RecordTypeEntity) {
    require(type.name.isNotBlank() && type.name.trim().length <= 24) { "项目名称请填写 1–24 个字" }
    require(type.unit.trim().length <= 12) { "单位最多填写 12 个字" }
}

internal fun validateNumericRecord(record: NumericRecordEntity) {
    require(record.value.isFinite() && kotlin.math.abs(record.value) <= 1_000_000_000_000.0) { "请填写有效数值，绝对值不超过一万亿" }
    require(record.note.length <= 200) { "备注最多填写 200 个字" }
}

class OfflineRecordRepository(private val dao: RecordDao) : RecordRepository {
    override fun observeTypes() = dao.observeTypes()
    override fun observeType(id: String) = dao.observeType(id)
    override fun observeRecords(typeId: String) = dao.observeRecords(typeId)
    override suspend fun getType(id: String) = dao.getType(id)
    override suspend fun getRecord(id: String) = dao.getRecord(id)

    override suspend fun saveType(type: RecordTypeEntity) {
        validateRecordType(type)
        dao.upsertType(type.copy(name = type.name.trim(), unit = type.unit.trim()))
    }

    override suspend fun saveRecord(record: NumericRecordEntity) {
        validateNumericRecord(record)
        require(dao.getType(record.typeId) != null) { "记录项目已不存在" }
        dao.upsertRecord(record.copy(note = record.note.trim()))
    }

    override suspend fun deleteType(id: String) = dao.deleteType(id)
    override suspend fun deleteRecord(id: String) = dao.deleteRecord(id)
}
