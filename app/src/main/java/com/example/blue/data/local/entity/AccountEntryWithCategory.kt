package com.example.blue.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AccountEntryWithCategory(
    @Embedded val entry: AccountEntryEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id",
    )
    val category: AccountCategoryEntity,
)
