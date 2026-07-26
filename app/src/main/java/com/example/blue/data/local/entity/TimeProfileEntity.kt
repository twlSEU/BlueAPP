package com.example.blue.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "time_profiles")
data class TimeProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val age: Int = 0,
    val birthday: LocalDate? = null,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
