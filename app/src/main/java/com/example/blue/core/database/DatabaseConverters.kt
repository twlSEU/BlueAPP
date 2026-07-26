package com.example.blue.core.database

import androidx.room.TypeConverter
import com.example.blue.data.local.entity.SleepSource
import com.example.blue.data.local.entity.TimeEventType
import com.example.blue.model.AccountType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DatabaseConverters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun localDateTimeToString(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun accountTypeToString(value: AccountType?): String? = value?.name

    @TypeConverter
    fun stringToAccountType(value: String?): AccountType? = value?.let(AccountType::valueOf)

    @TypeConverter
    fun sleepSourceToString(value: SleepSource?): String? = value?.name

    @TypeConverter
    fun stringToSleepSource(value: String?): SleepSource? = value?.let(SleepSource::valueOf)

    @TypeConverter
    fun timeEventTypeToString(value: TimeEventType?): String? = value?.name

    @TypeConverter
    fun stringToTimeEventType(value: String?): TimeEventType? = value?.let(TimeEventType::valueOf)
}
