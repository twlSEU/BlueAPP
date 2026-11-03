package com.example.blue.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.blue.data.local.entity.NumericRecordEntity
import com.example.blue.data.local.entity.RecordTypeEntity
import com.example.blue.data.local.entity.RecordTypeOverview
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("""
        SELECT t.id, t.name, t.unit,
            (SELECT COUNT(*) FROM numeric_records WHERE typeId = t.id) AS recordCount,
            (SELECT value FROM numeric_records WHERE typeId = t.id
                ORDER BY recordedAt DESC, createdAt DESC, id DESC LIMIT 1) AS latestValue,
            (SELECT recordedAt FROM numeric_records WHERE typeId = t.id
                ORDER BY recordedAt DESC, createdAt DESC, id DESC LIMIT 1) AS latestRecordedAt
        FROM record_types t ORDER BY t.createdAt DESC, t.id DESC
    """)
    fun observeTypes(): Flow<List<RecordTypeOverview>>

    @Query("SELECT * FROM record_types WHERE id = :id")
    fun observeType(id: String): Flow<RecordTypeEntity?>

    @Query("SELECT * FROM record_types WHERE id = :id")
    suspend fun getType(id: String): RecordTypeEntity?

    @Query("SELECT * FROM numeric_records WHERE id = :id")
    suspend fun getRecord(id: String): NumericRecordEntity?

    @Query("SELECT * FROM numeric_records WHERE typeId = :typeId ORDER BY recordedAt DESC, createdAt DESC, id DESC")
    fun observeRecords(typeId: String): Flow<List<NumericRecordEntity>>

    @Upsert
    suspend fun upsertType(type: RecordTypeEntity)

    @Upsert
    suspend fun upsertRecord(record: NumericRecordEntity)

    @Query("DELETE FROM record_types WHERE id = :id")
    suspend fun deleteType(id: String)

    @Query("DELETE FROM numeric_records WHERE id = :id")
    suspend fun deleteRecord(id: String)

    @Query("SELECT * FROM record_types ORDER BY createdAt, id")
    suspend fun getAllTypes(): List<RecordTypeEntity>

    @Query("SELECT * FROM numeric_records ORDER BY recordedAt, createdAt, id")
    suspend fun getAllRecords(): List<NumericRecordEntity>

    @Query("DELETE FROM record_types")
    suspend fun clearTypes()
}
