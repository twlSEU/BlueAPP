package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.blue.model.AccountType

@Entity(
    tableName = "account_categories",
    indices = [Index(value = ["type", "name"], unique = true)],
)
data class AccountCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: AccountType,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
