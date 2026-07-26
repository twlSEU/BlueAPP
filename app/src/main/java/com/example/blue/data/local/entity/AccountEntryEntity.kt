package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.blue.model.AccountType
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "account_entries",
    foreignKeys = [
        ForeignKey(
            entity = AccountCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["entryDate", "entryTime"]),
        Index(value = ["type"]),
    ],
)
data class AccountEntryEntity(
    @PrimaryKey val id: String,
    val entryDate: LocalDate,
    val entryTime: LocalTime,
    val type: AccountType,
    val amountInCents: Long,
    val name: String,
    val categoryId: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
