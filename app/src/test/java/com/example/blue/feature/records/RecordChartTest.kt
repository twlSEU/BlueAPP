package com.example.blue.feature.records

import com.example.blue.data.local.entity.NumericRecordEntity
import org.junit.Assert.*
import org.junit.Test

class RecordChartTest {
    @Test
    fun chartUsesLatestSevenMeasurementsInTimeOrder() {
        val records = (1..10).map { record("$it", it.toLong(), it.toDouble()) }.shuffled()
        assertEquals((4..10).map { it.toDouble() }, recentRecordPoints(records).map { it.value })
    }

    @Test
    fun backfilledOldMeasurementDoesNotDisplaceRecentData() {
        val recent = (1..7).map { record("$it", it.toLong(), it.toDouble()) }
        val backfill = record("backfill", 0, 100.0).copy(createdAt = 1000)
        assertEquals(recent, recentRecordPoints(recent + backfill))
    }

    @Test
    fun measurementsWithSameTimestampRemainSeparateAndHaveStableOrder() {
        val points = listOf(record("b", 1, 2.0), record("a", 1, 1.0), record("c", 1, 3.0).copy(createdAt = 2))
        assertEquals(listOf("a", "b", "c"), recentRecordPoints(points).map { it.id })
    }

    @Test
    fun emptyAndSingleMeasurementDoNotInventPoints() {
        assertTrue(recentRecordPoints(emptyList()).isEmpty())
        val one = record("single", 1, 65.0)
        assertEquals(listOf(one), recentRecordPoints(listOf(one)))
    }

    @Test
    fun constantAndZeroValuesHaveUsableVerticalRange() {
        listOf(listOf(0.0), listOf(65.0, 65.0), listOf(-10.0, -10.0)).forEach { values ->
            val range = recordChartRange(values)
            assertTrue(range.minimum < values.min())
            assertTrue(range.maximum > values.max())
            assertTrue(range.minimum.isFinite() && range.maximum.isFinite())
        }
    }

    @Test
    fun negativeValuesAndSmallChangesFitInsideChart() {
        listOf(listOf(-8.0, -5.0, -7.0), listOf(65.001, 65.002), listOf(-1e12, 1e12)).forEach { values ->
            val range = recordChartRange(values)
            assertTrue(range.minimum < values.min())
            assertTrue(range.maximum > values.max())
        }
    }

    @Test
    fun smallChangesHaveDistinctReadableAxisLabels() {
        val range = recordChartRange(listOf(65.001, 65.002))
        val span = range.maximum - range.minimum
        val labels = listOf(range.minimum, (range.minimum + range.maximum) / 2, range.maximum).map { recordAxisLabel(it, span) }
        assertEquals(3, labels.distinct().size)
        assertTrue(labels.none { it.contains('E') })
    }

    @Test
    fun numericInputAcceptsDecimalsSignsAndChineseDecimalMarks() {
        assertEquals(65.25, parseRecordValue(" 65.25 ")!!, 0.0)
        assertEquals(-2.5, parseRecordValue("-2.5")!!, 0.0)
        assertEquals(0.5, parseRecordValue(".5")!!, 0.0)
        assertEquals(65.5, parseRecordValue("65。5")!!, 0.0)
        assertEquals(0.0, parseRecordValue("0")!!, 0.0)
    }

    @Test
    fun malformedOrUnboundedValuesCannotBeSaved() {
        listOf("", " ", ".", "-", "NaN", "Infinity", "1e309", "1,000", "1.2.3", "1000000000001").forEach {
            assertNull("Input: $it", parseRecordValue(it))
        }
        assertEquals(1e12, parseRecordValue("1000000000000")!!, 0.0)
    }

    @Test
    fun valuesKeepSignAndDecimalsWithoutScientificNotation() {
        assertEquals("65.25", formatRecordValue(65.25))
        assertEquals("-0.001", formatRecordValue(-0.001))
        assertEquals("1000000000000", formatRecordValue(1e12))
    }

    private fun record(id: String, time: Long, value: Double) = NumericRecordEntity(id, "weight", value, time, "", time, time)
}
