package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "record_types")
data class RecordTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unit: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "numeric_records",
    foreignKeys = [ForeignKey(
        entity = RecordTypeEntity::class,
        parentColumns = ["id"],
        childColumns = ["typeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["typeId", "recordedAt", "createdAt", "id"])],
)
data class NumericRecordEntity(
    @PrimaryKey val id: String,
    val typeId: String,
    val value: Double,
    val recordedAt: Long,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class RecordTypeOverview(
    val id: String,
    val name: String,
    val unit: String,
    val recordCount: Int,
    val latestValue: Double?,
    val latestRecordedAt: Long?,
)
