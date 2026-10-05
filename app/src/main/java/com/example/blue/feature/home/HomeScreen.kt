package com.example.blue.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.R
import com.example.blue.core.navigation.AppDestination
import com.example.blue.feature.common.appPressScale
import com.example.blue.ui.theme.BlueTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val HomeCardShape = RoundedCornerShape(26.dp)
private val HomeIconShape = RoundedCornerShape(18.dp)
private val HomeButtonShape = RoundedCornerShape(50)
private val MainHomeFeatures = homeFeatures.filter { it.destination != AppDestination.Backup }

@Composable
fun HomeScreen(
    onActionClick: (HomeActionDestination) -> Unit,
    modifier: Modifier = Modifier,
    onFeatureClick: (AppDestination) -> Unit = {},
    metrics: HomeMetrics = HomeMetrics(),
    date: LocalDate = LocalDate.now(),
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = remember(darkTheme) { homeColors(darkTheme) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.backgroundGlow, Color.Transparent),
                            center = Offset(size.width * 0.08f, size.height * 0.2f),
                            radius = size.width * 1.15f,
                        ),
                    )
                }
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(start = 14.dp, top = 24.dp, end = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item(key = "welcome", contentType = "home-introduction") {
                    HomeIntroduction(date, colors)
                }
                items(
                    items = MainHomeFeatures,
                    key = { it.destination.route },
                    contentType = { "home-feature" },
                ) { feature ->
                    // The card opens the overview (or 去来); the pill retains the quick action.
                    val onOverviewClick = {
                        val secondaryAction = feature.directSecondaryAction
                        if (secondaryAction != null) {
                            onActionClick(secondaryAction.destination)
                        } else {
                            onFeatureClick(feature.destination)
                        }
                    }
                    FeatureCard(
                        feature = feature,
                        metricLabel = metrics.labelFor(feature.destination),
                        colors = colors,
                        darkTheme = darkTheme,
                        onActionClick = { onActionClick(feature.primaryAction.destination) },
                        onOverviewClick = onOverviewClick,
                    )
                }
                item(key = "backup", contentType = "home-backup") {
                    BackupSection(colors, onActionClick)
                }
            }
        }
    }
}

@Composable
private fun HomeIntroduction(date: LocalDate, colors: HomeColors) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 18.dp)) {
        val stackDate = maxWidth < 280.dp * fontScale + 72.dp
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (stackDate) {
                HomeTitle(colors)
                Text(
                    text = "${date.monthValue}月${date.dayOfMonth}日 · ${date.weekdayLabel()}",
                    color = colors.muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
                HomeSubtitle(colors)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HomeTitle(colors)
                        HomeSubtitle(colors)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(46.dp)
                                .background(colors.muted.copy(alpha = 0.55f)),
                        )
                        Column(
                            modifier = Modifier.padding(start = 13.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "${date.monthValue}月${date.dayOfMonth}日",
                                color = colors.muted,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                            )
                            Text(
                                text = date.weekdayLabel(),
                                color = colors.muted,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LocalDate.weekdayLabel(): String =
    dayOfWeek.getDisplayName(TextStyle.FULL, Locale.SIMPLIFIED_CHINESE)

@Composable
private fun HomeTitle(colors: HomeColors) {
    Text(
        text = "悟已往之不谏",
        modifier = Modifier.semantics { heading() },
        fontFamily = FontFamily.SansSerif,
        fontSize = 34.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        color = colors.title,
    )
}

@Composable
private fun HomeSubtitle(colors: HomeColors) {
    Text(
        text = "往日暗沉不可追，来日之路光明灿烂",
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        color = colors.muted,
    )
}

@Composable
private fun FeatureCard(
    feature: HomeFeature,
    metricLabel: String,
    colors: HomeColors,
    darkTheme: Boolean,
    onActionClick: () -> Unit,
    onOverviewClick: () -> Unit,
) {
    val palette = remember(feature.accent, darkTheme) { featurePalette(feature.accent, darkTheme) }
    val interactionSource = remember { MutableInteractionSource() }
    val fontScale = LocalDensity.current.fontScale

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .appPressScale(interactionSource, pressedScale = 0.99f)
            .dropShadow(
                shape = HomeCardShape,
                shadow = Shadow(
                    radius = 22.dp,
                    color = colors.shadow,
                    offset = DpOffset(0.dp, 8.dp),
                ),
            )
            .clip(HomeCardShape)
            .background(colors.surface)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.glow, Color.Transparent),
                        center = Offset(size.width * 0.79f, size.height * 0.75f),
                        radius = size.width * 0.68f,
                    ),
                )
            }
            .border(1.dp, colors.border, HomeCardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = feature.directSecondaryAction?.title ?: "查看${feature.destination.title}",
                onClick = onOverviewClick,
            )
            .padding(start = 18.dp, end = 8.dp, top = 20.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        val compact = maxWidth < 324.dp
        val iconSize = if (compact) 48.dp else 56.dp
        val iconGap = if (compact) 10.dp else 12.dp
        val buttonWidth = (if (compact) 96.dp else 108.dp) * fontScale
        val stackAction = maxWidth < iconSize + iconGap + 96.dp * fontScale + 8.dp + buttonWidth + 48.dp
        if (stackAction) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(iconGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FeatureIcon(feature, palette, iconSize)
                    FeatureInformation(feature.destination.title, metricLabel, colors, Modifier.weight(1f))
                    OverviewChevron(colors)
                }
                FeatureActionButton(
                    label = feature.primaryAction.title,
                    onClick = onActionClick,
                    containerColor = palette.button,
                    contentColor = palette.accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = iconSize + iconGap, end = 12.dp),
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FeatureIcon(feature, palette, iconSize)
                Spacer(Modifier.width(iconGap))
                FeatureInformation(feature.destination.title, metricLabel, colors, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                FeatureActionButton(
                    label = feature.primaryAction.title,
                    onClick = onActionClick,
                    containerColor = palette.button,
                    contentColor = palette.accent,
                    modifier = Modifier.width(buttonWidth),
                )
                OverviewChevron(colors)
            }
        }
    }
}

@Composable
private fun FeatureIcon(feature: HomeFeature, palette: FeaturePalette, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(palette.iconBackground, HomeIconShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(feature.iconRes),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = palette.accent,
        )
    }
}

@Composable
private fun FeatureInformation(
    title: String,
    metricLabel: String,
    colors: HomeColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            fontFamily = FontFamily.SansSerif,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.title,
        )
        Text(
            text = metricLabel,
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = colors.muted,
        )
    }
}

@Composable
private fun OverviewChevron(colors: HomeColors) {
    // The enclosing card owns the overview click and its accessibility label.
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(R.drawable.ic_home_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(width = 12.dp, height = 18.dp),
            tint = colors.muted,
        )
    }
}

@Composable
private fun FeatureActionButton(
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .appPressScale(interactionSource, pressedScale = 0.975f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = HomeButtonShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun BackupSection(colors: HomeColors, onActionClick: (HomeActionDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_backup),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.muted,
            )
            Text(
                text = "数据管理 · 本地保存",
                modifier = Modifier.semantics { heading() },
                color = colors.muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureActionButton(
                label = "导出备份",
                onClick = { onActionClick(HomeActionDestination.BACKUP_EXPORT) },
                containerColor = colors.surface,
                contentColor = colors.body,
                modifier = Modifier.weight(1f),
            )
            FeatureActionButton(
                label = "恢复数据",
                onClick = { onActionClick(HomeActionDestination.BACKUP_RESTORE) },
                containerColor = colors.surface,
                contentColor = colors.body,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Immutable
private data class HomeColors(
    val background: Color,
    val backgroundGlow: Color,
    val surface: Color,
    val title: Color,
    val body: Color,
    val muted: Color,
    val border: Color,
    val shadow: Color,
)

private fun homeColors(darkTheme: Boolean): HomeColors = if (darkTheme) {
    HomeColors(
        background = Color(0xFF111722),
        backgroundGlow = Color(0xFF20334E).copy(alpha = 0.45f),
        surface = Color(0xFF1B2432),
        title = Color(0xFFF0F4FC),
        body = Color(0xFFB7C4D8),
        muted = Color(0xFF9AAAC3),
        border = Color.White.copy(alpha = 0.09f),
        shadow = Color.Black.copy(alpha = 0.14f),
    )
} else {
    HomeColors(
        background = Color(0xFFF5F8FD),
        backgroundGlow = Color(0xFFE6EEFF).copy(alpha = 0.7f),
        surface = Color(0xFFFAFCFF),
        title = Color(0xFF0B1422),
        body = Color(0xFF63748F),
        muted = Color(0xFF7D8BA7),
        border = Color.White.copy(alpha = 0.9f),
        shadow = Color(0xFF647D9E).copy(alpha = 0.09f),
    )
}

@Immutable
private data class FeaturePalette(
    val accent: Color,
    val glow: Color,
    val iconBackground: Color,
    val button: Color,
)

private fun featurePalette(accent: FeatureAccent, darkTheme: Boolean): FeaturePalette {
    val color = when (accent) {
        FeatureAccent.PRIMARY -> if (darkTheme) Color(0xFF86B8FF) else Color(0xFF0074F8)
        FeatureAccent.SECONDARY -> if (darkTheme) Color(0xFFF0B280) else Color(0xFFA35309)
        FeatureAccent.QUATERNARY -> if (darkTheme) Color(0xFFB8A5FF) else Color(0xFF603AF0)
        FeatureAccent.QUINARY -> if (darkTheme) Color(0xFF78D3C8) else Color(0xFF00877D)
        FeatureAccent.TERTIARY -> if (darkTheme) Color(0xFF83D4AF) else Color(0xFF168450)
    }
    val tint = when (accent) {
        FeatureAccent.PRIMARY -> Color(0xFF85BAFF)
        FeatureAccent.SECONDARY -> Color(0xFFF0C6A5)
        FeatureAccent.QUATERNARY -> Color(0xFFB6A0FF)
        FeatureAccent.QUINARY -> Color(0xFF7FD1C6)
        FeatureAccent.TERTIARY -> Color(0xFF83D4AF)
    }
    val surface = if (darkTheme) Color(0xFF1B2432) else Color(0xFFFAFCFF)
    return FeaturePalette(
        accent = color,
        glow = tint.copy(alpha = if (darkTheme) 0.17f else 0.25f),
        iconBackground = tint.copy(alpha = if (darkTheme) 0.16f else 0.17f).compositeOver(surface),
        button = tint.copy(alpha = if (darkTheme) 0.22f else 0.24f).compositeOver(surface),
    )
}

private val HomePreviewMetrics = HomeMetrics(
    diaryMonthCount = 0,
    accountingMonthCount = 22,
    sleepMonthCount = 4,
    timeEventCount = 1,
)
private val HomePreviewDate = LocalDate.of(2025, 4, 16)

@Preview(name = "首页 · 设计稿", showBackground = true, widthDp = 390, heightDp = 686, locale = "zh-rCN")
@Preview(name = "首页 · 浅色", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844, locale = "zh-rCN")
@Preview(name = "首页 · 窄屏", showBackground = true, showSystemUi = true, widthDp = 320, heightDp = 740, locale = "zh-rCN")
@Preview(name = "首页 · 大字体", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844, fontScale = 1.5f, locale = "zh-rCN")
@Preview(name = "首页 · 平板", showBackground = true, showSystemUi = true, widthDp = 800, heightDp = 1100, locale = "zh-rCN")
@Composable
private fun HomeScreenPreview() {
    BlueTheme(darkTheme = false) {
        HomeScreen(onActionClick = {}, metrics = HomePreviewMetrics, date = HomePreviewDate)
    }
}

@Preview(
    name = "首页 · 深色",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "zh-rCN",
)
@Composable
private fun HomeScreenDarkPreview() {
    BlueTheme(darkTheme = true) {
        HomeScreen(onActionClick = {}, metrics = HomePreviewMetrics, date = HomePreviewDate)
    }
}
