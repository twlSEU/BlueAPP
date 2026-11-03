package com.example.blue.data.repository

import com.example.blue.data.local.entity.NumericRecordEntity
import com.example.blue.data.local.entity.RecordTypeEntity
import org.junit.Assert.assertThrows
import org.junit.Test

class RecordValidationTest {
    @Test
    fun emptyOrOverlongProjectNamesAreRejected() {
        listOf(" ", "名".repeat(25)).forEach { name ->
            assertThrows(IllegalArgumentException::class.java) { validateRecordType(RecordTypeEntity("type", name, "kg", 0, 0)) }
        }
    }

    @Test
    fun unitIsOptionalButCannotOverflowProjectLabels() {
        validateRecordType(RecordTypeEntity("type", "自定义项目", "", 0, 0))
        assertThrows(IllegalArgumentException::class.java) { validateRecordType(RecordTypeEntity("type", "项目", "u".repeat(13), 0, 0)) }
    }

    @Test
    fun nonFiniteAndOutOfRangeMeasurementsAreRejected() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1e13).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { validateNumericRecord(record(value)) }
        }
    }

    @Test
    fun genericProjectsAllowZeroAndNegativeMeasurements() {
        listOf(0.0, -12.5, 65.25).forEach { validateNumericRecord(record(it)) }
        assertThrows(IllegalArgumentException::class.java) { validateNumericRecord(record(1.0).copy(note = "a".repeat(201))) }
    }

    private fun record(value: Double) = NumericRecordEntity("entry", "type", value, 0, "", 0, 0)
}
