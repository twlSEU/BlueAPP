package com.example.blue.feature.sleep

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.blue.core.util.SleepDateRules
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SleepEstimateResult {
    data class Available(val dateTime: LocalDateTime) : SleepEstimateResult
    data object UsageAccessRequired : SleepEstimateResult
    data class Unavailable(val message: String = "暂无系统推测数据，请选择入睡时间") : SleepEstimateResult
}

/**
 * Best-effort, on-demand inference using public UsageStats APIs only.
 * It never schedules background work and never treats the result as confirmed data.
 */
class SleepTimeEstimator(context: Context) {
    private val appContext = context.applicationContext

    fun hasUsageAccess(): Boolean {
        val appOps = appContext.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            appContext.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun estimate(recordDate: LocalDate): SleepEstimateResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return@withContext SleepEstimateResult.Unavailable()
        }
        if (!hasUsageAccess()) return@withContext SleepEstimateResult.UsageAccessRequired

        val zone = ZoneId.systemDefault()
        val windowStart = LocalDateTime.of(recordDate, LocalTime.of(18, 0))
        val naturalEnd = LocalDateTime.of(recordDate.plusDays(1), SleepDateRules.dayBoundary)
        val now = LocalDateTime.now()
        val windowEnd = minOf(naturalEnd, now)
        if (!windowEnd.isAfter(windowStart)) return@withContext SleepEstimateResult.Unavailable()

        val manager = appContext.getSystemService(UsageStatsManager::class.java)
            ?: return@withContext SleepEstimateResult.Unavailable()
        val events = manager.queryEvents(
            windowStart.atZone(zone).toInstant().toEpochMilli(),
            windowEnd.atZone(zone).toInstant().toEpochMilli(),
        )
        val event = UsageEvents.Event()
        var lastScreenOff: Long? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                lastScreenOff = event.timeStamp
            }
        }
        lastScreenOff?.let { timestamp ->
            val dateTime = java.time.Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDateTime()
            SleepEstimateResult.Available(dateTime)
        } ?: SleepEstimateResult.Unavailable()
    }
}
