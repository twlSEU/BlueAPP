package com.example.blue.feature.records

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.data.local.entity.NumericRecordEntity
import com.example.blue.feature.common.RefinedBackground
import com.example.blue.feature.common.RefinedCard
import com.example.blue.feature.common.RefinedLine
import com.example.blue.feature.common.RefinedMuted
import com.example.blue.feature.common.RefinedTeal
import com.example.blue.ui.theme.BlueTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max

internal fun recentRecordPoints(records: List<NumericRecordEntity>): List<NumericRecordEntity> =
    records.sortedWith(compareBy<NumericRecordEntity> { it.recordedAt }.thenBy { it.createdAt }.thenBy { it.id })
        .takeLast(7)

internal data class RecordChartRange(val minimum: Double, val maximum: Double)

internal fun recordChartRange(values: List<Double>): RecordChartRange {
    if (values.isEmpty()) return RecordChartRange(0.0, 1.0)
    val minimum = values.min()
    val maximum = values.max()
    val padding = if (minimum == maximum) max(abs(minimum) * 0.02, 1.0) else (maximum - minimum) * 0.15
    return RecordChartRange(minimum - padding, maximum + padding)
}

internal fun parseRecordValue(text: String): Double? {
    val normalized = text.trim().replace('。', '.').replace('．', '.')
    if (!normalized.matches(Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))) return null
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && abs(it) <= 1_000_000_000_000.0 }
}

internal fun formatRecordValue(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

internal fun recordDateLabel(timestamp: Long, pattern: String = "M月d日 HH:mm"): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))

internal fun recordAxisLabel(value: Double, span: Double): String {
    val magnitude = abs(value)
    val divisor = when {
        magnitude >= 100_000_000 -> 100_000_000.0
        magnitude >= 10_000 -> 10_000.0
        else -> 1.0
    }
    val suffix = when (divisor) { 100_000_000.0 -> "亿"; 10_000.0 -> "万"; else -> "" }
    val precision = (1 - floor(log10(span / divisor / 2)).toInt()).coerceIn(0, 12)
    return BigDecimal.valueOf(value / divisor).setScale(precision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + suffix
}

@Composable
internal fun RecordLineChart(records: List<NumericRecordEntity>, unit: String) {
    val points = remember(records) { recentRecordPoints(records) }
    if (points.isEmpty()) return
    val range = remember(points) { recordChartRange(points.map { it.value }) }
    val datePattern = if (points.map { recordDateLabel(it.recordedAt, "yyyy-MM-dd") }.distinct().size == 1) "HH:mm" else "M/d"
    val description = points.joinToString("；") { "${recordDateLabel(it.recordedAt)}，${formatRecordValue(it.value)} $unit" }
    Column(Modifier.fillMaxWidth().semantics { contentDescription = "最近 ${points.size} 次记录趋势。$description" }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.width(46.dp).height(180.dp), verticalArrangement = Arrangement.SpaceBetween) {
                listOf(range.maximum, (range.maximum + range.minimum) / 2, range.minimum).forEach {
                    Text(
                        recordAxisLabel(it, range.maximum - range.minimum), fontSize = 10.sp, lineHeight = 14.sp, color = RefinedMuted,
                        modifier = Modifier.fillMaxWidth(), maxLines = 1, softWrap = false,
                        autoSize = TextAutoSize.StepBased(7.sp, 10.sp, 0.5.sp),
                    )
                }
            }
            Canvas(Modifier.weight(1f).height(180.dp).padding(vertical = 7.dp)) {
                val bottom = size.height
                for (index in 0..2) {
                    val y = bottom * index / 2
                    drawLine(RefinedLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val offsets = points.mapIndexed { index, point ->
                    Offset(
                        size.width * (index + 0.5f) / points.size,
                        (bottom * (1 - (point.value - range.minimum) / (range.maximum - range.minimum))).toFloat(),
                    )
                }
                val line = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                if (offsets.size > 1) {
                    val area = Path().apply {
                        addPath(line)
                        lineTo(offsets.last().x, bottom)
                        lineTo(offsets.first().x, bottom)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(RefinedTeal.copy(alpha = 0.13f), RefinedTeal.copy(alpha = 0.01f))))
                    drawPath(line, RefinedTeal, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                offsets.forEachIndexed { index, offset ->
                    if (index == offsets.lastIndex) drawCircle(RefinedTeal.copy(alpha = 0.12f), 8.dp.toPx(), offset)
                    drawCircle(Color.White, 4.5.dp.toPx(), offset)
                    drawCircle(RefinedTeal, 3.dp.toPx(), offset)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Spacer(Modifier.width(52.dp))
            points.forEach { point ->
                Text(
                    recordDateLabel(point.recordedAt, datePattern), modifier = Modifier.weight(1f),
                    fontSize = 10.sp, lineHeight = 16.sp, color = RefinedMuted, textAlign = TextAlign.Center,
                    maxLines = 1, softWrap = false, autoSize = TextAutoSize.StepBased(7.sp, 10.sp, 0.5.sp),
                )
            }
        }
    }
}

@Preview(name = "记录折线图 · 最近七次", showBackground = true, widthDp = 390, heightDp = 360)
@Preview(name = "记录折线图 · 窄屏大字体", showBackground = true, widthDp = 320, heightDp = 420, fontScale = 1.5f)
@Composable
private fun RecordLineChartPreview() {
    RecordChartPreviewContent(sampleNumericRecords())
}

@Preview(name = "记录折线图 · 单条数据", showBackground = true, widthDp = 390, heightDp = 360)
@Composable
private fun RecordSinglePointChartPreview() {
    RecordChartPreviewContent(sampleNumericRecords().take(1))
}

@Composable
private fun RecordChartPreviewContent(records: List<NumericRecordEntity>) {
    BlueTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize().background(RefinedBackground).padding(16.dp)) {
            RefinedCard(title = "体重 · 最近七次", subtitle = "单位：kg", accent = RefinedTeal) {
                RecordLineChart(records, "kg")
            }
        }
    }
}
