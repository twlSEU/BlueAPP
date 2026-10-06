package com.example.blue.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.R

internal val RefinedBackground = Color(0xFFF5F8FD)
internal val RefinedInk = Color(0xFF182E4B)
internal val RefinedMuted = Color(0xFF8294AE)
internal val RefinedLine = Color(0xFFE9EFF7)
internal val RefinedBlue = Color(0xFF4385EE)
internal val RefinedViolet = Color(0xFF8A79DA)
internal val RefinedTeal = Color(0xFF00A48D)
internal val RefinedCoral = Color(0xFFF05B72)
internal val RefinedShadow = Color(0xFF7895BC).copy(alpha = 0.055f)

@Composable
internal fun RefinedCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    iconRes: Int? = null,
    accent: Color = RefinedBlue,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier.fillMaxWidth().dropShadow(
            shape, Shadow(radius = 18.dp, color = RefinedShadow, offset = DpOffset(0.dp, 5.dp)),
        ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (iconRes != null) {
                        Box(
                            Modifier.size(30.dp).background(accent.copy(alpha = 0.09f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(iconRes), null, Modifier.size(17.dp), tint = accent)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(title, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold, color = RefinedInk)
                        if (!subtitle.isNullOrBlank()) {
                            Text(subtitle, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp, color = RefinedMuted)
                        }
                    }
                }
            }
            content()
        }
    }
}

@Composable
internal fun RefinedMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = RefinedInk,
) {
    Column(
        modifier.background(
            Brush.linearGradient(listOf(accent.copy(alpha = 0.045f), accent.copy(alpha = 0.025f))),
            RoundedCornerShape(16.dp),
        ).padding(horizontal = 12.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, fontSize = 11.sp, lineHeight = 17.sp, letterSpacing = 0.sp, color = RefinedMuted, maxLines = 2)
        Text(
            if (value.length > 14) value.replace(",", ",\u200B") else value,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = if (value.length > 14) 2 else 1,
            softWrap = value.length > 14,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 24.sp, stepSize = 0.5.sp),
        )
    }
}

@Composable
internal fun RefinedSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(Color(0xFFECF1F8), RoundedCornerShape(16.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = selectedIndex == index
            Surface(
                onClick = { onSelected(index) },
                modifier = Modifier.weight(1f).heightIn(min = 38.dp).semantics { selected = active },
                shape = RoundedCornerShape(12.dp),
                color = if (active) Color.White else Color.Transparent,
                shadowElevation = if (active) 1.dp else 0.dp,
            ) {
                Box(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        label, fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) RefinedInk else RefinedMuted,
                        maxLines = 1, softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun RefinedPeriodSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    canMoveBack: Boolean = true,
    canMoveForward: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth().dropShadow(
            CircleShape, Shadow(radius = 14.dp, color = RefinedShadow, offset = DpOffset(0.dp, 4.dp)),
        ),
        shape = RoundedCornerShape(percent = 50),
        color = Color.White,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            RefinedPeriodArrow(canMoveBack, true, onPrevious)
            Text(
                label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
                fontWeight = FontWeight.Medium, color = RefinedInk,
                maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 22.sp, stepSize = 0.5.sp),
            )
            RefinedPeriodArrow(canMoveForward, false, onNext)
        }
    }
}

@Composable
private fun RefinedPeriodArrow(enabled: Boolean, pointsLeft: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(
            painterResource(R.drawable.ic_home_chevron_right),
            contentDescription = if (pointsLeft) "上一期" else "下一期",
            tint = if (enabled) RefinedInk else RefinedMuted.copy(alpha = 0.35f),
            modifier = Modifier.size(width = 12.dp, height = 18.dp).graphicsLayer { rotationZ = if (pointsLeft) 180f else 0f },
        )
    }
}

@Composable
internal fun RefinedTopBar(title: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(RefinedBackground).statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            title, modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 52.dp),
            fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, color = RefinedInk,
            textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 22.sp, stepSize = 0.5.sp),
        )
        Surface(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(44.dp), shape = CircleShape, color = Color.White) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_home_chevron_right), "返回", Modifier.size(width = 18.dp, height = 26.dp).graphicsLayer { rotationZ = 180f }, tint = RefinedInk)
            }
        }
    }
}
