package com.example.blue.feature.time

import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeEventType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeEventStatusTest {
    private val today = LocalDate.of(2026, 7, 17)

    @Test
    fun countdownBeforeTargetShowsDaysAfter() {
        val status = timeEventStatus(event(today.plusDays(3), TimeEventType.COUNTDOWN), today)

        assertEquals(3L, status.days)
        assertEquals("天后", status.unit)
        assertFalse(status.expired)
    }

    @Test
    fun expiredCountdownStaysVisibleAtZero() {
        val status = timeEventStatus(event(today.minusDays(30), TimeEventType.COUNTDOWN), today)

        assertEquals(0L, status.days)
        assertEquals("天了", status.unit)
        assertTrue(status.expired)
    }

    @Test
    fun countdownTodayIsNotExpired() {
        val status = timeEventStatus(event(today, TimeEventType.COUNTDOWN), today)

        assertEquals(0L, status.days)
        assertEquals("天后", status.unit)
        assertFalse(status.expired)
    }

    @Test
    fun anniversaryShowsElapsedDays() {
        val status = timeEventStatus(event(today.minusDays(310), TimeEventType.ANNIVERSARY), today)

        assertEquals(310L, status.days)
        assertEquals("天了", status.unit)
        assertFalse(status.expired)
    }

    @Test
    fun expiredCountdownsMoveBehindActiveEvents() {
        val expired = event(today.minusDays(1), TimeEventType.COUNTDOWN).copy(id = "expired")
        val anniversary = event(today.minusDays(20), TimeEventType.ANNIVERSARY).copy(id = "anniversary")
        val upcoming = event(today.plusDays(2), TimeEventType.COUNTDOWN).copy(id = "upcoming")

        val result = moveExpiredEventsLast(listOf(expired, anniversary, upcoming), today)

        assertEquals(listOf("anniversary", "upcoming", "expired"), result.map { it.id })
    }

    private fun event(date: LocalDate, type: TimeEventType) = TimeEventEntity(
        id = "event",
        title = "测试事件",
        eventDate = date,
        type = type,
        imagePath = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
