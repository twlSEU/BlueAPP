package com.example.blue.feature.home

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blue.R
import com.example.blue.core.navigation.AppDestination
import com.example.blue.feature.common.appPressScale
import com.example.blue.ui.theme.BlueTheme

private val HomeCardShape = RoundedCornerShape(28.dp)
private val HomeIconShape = RoundedCornerShape(20.dp)
private val HomeButtonShape = RoundedCornerShape(24.dp)

@Composable
fun HomeScreen(
    onActionClick: (HomeActionDestination) -> Unit,
    modifier: Modifier = Modifier,
    onFeatureClick: (AppDestination) -> Unit = {},
    metrics: HomeMetrics = HomeMetrics(),
) {
    // Follow the active app theme, including explicit dark-theme previews.
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
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item(key = "welcome", contentType = "home-introduction") {
                    HomeIntroduction(colors)
                }
                items(
                    items = homeFeatures,
                    key = { feature -> feature.destination.route },
                    contentType = { "home-feature" },
                ) { feature ->
                    FeatureCard(
                        feature = feature,
                        metricLabel = metrics.labelFor(feature.destination),
                        colors = colors,
                        darkTheme = darkTheme,
                        onActionClick = onActionClick,
                        onFeatureClick = { onFeatureClick(feature.destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeIntroduction(colors: HomeColors) {
    Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "悟已往之不谏",
            modifier = Modifier.semantics { heading() },
            fontFamily = FontFamily.SansSerif,
            fontSize = 34.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            color = colors.title,
        )
        Text(
            text = "往日暗沉不可追，来日之路光明灿烂",
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            color = colors.muted,
        )
    }
}

@Composable
private fun FeatureCard(
    feature: HomeFeature,
    metricLabel: String,
    colors: HomeColors,
    darkTheme: Boolean,
    onActionClick: (HomeActionDestination) -> Unit,
    onFeatureClick: () -> Unit,
) {
    val palette = remember(feature.accent, darkTheme) { featurePalette(feature.accent, darkTheme) }
    val fontScale = LocalDensity.current.fontScale

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .dropShadow(
                shape = HomeCardShape,
                shadow = Shadow(
                    radius = 24.dp,
                    color = colors.shadow,
                    offset = DpOffset(0.dp, 8.dp),
                ),
            ),
        shape = HomeCardShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        BoxWithConstraints(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val buttonWidth = 96.dp * fontScale
            // The usual layout has three aligned columns. At large font sizes,
            // move both actions below the information instead of clipping labels.
            val useStackedActions = maxWidth < 56.dp + 24.dp + 104.dp * fontScale + buttonWidth
            if (useStackedActions) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FeatureIcon(feature, palette, colors)
                        FeatureInformation(
                            title = feature.destination.title,
                            metricLabel = metricLabel,
                            colors = colors,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FeatureSecondaryButton(
                            feature, palette, colors, onActionClick, onFeatureClick,
                            modifier = Modifier.weight(1f),
                        )
                        FeatureActionButton(
                            label = feature.primaryAction.title,
                            iconRes = actionIconFor(feature.primaryAction.destination),
                            onClick = { onActionClick(feature.primaryAction.destination) },
                            containerColor = palette.button,
                            contentColor = palette.onButton,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FeatureIcon(feature, palette, colors)
                    FeatureInformation(
                        title = feature.destination.title,
                        metricLabel = metricLabel,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )
                    Column(
                        modifier = Modifier.width(buttonWidth),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FeatureSecondaryButton(feature, palette, colors, onActionClick, onFeatureClick)
                        FeatureActionButton(
                            label = feature.primaryAction.title,
                            iconRes = actionIconFor(feature.primaryAction.destination),
                            onClick = { onActionClick(feature.primaryAction.destination) },
                            containerColor = palette.button,
                            contentColor = palette.onButton,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureIcon(feature: HomeFeature, palette: FeaturePalette, colors: HomeColors) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                palette.accent.copy(alpha = 0.08f).compositeOver(colors.surface),
                HomeIconShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(feature.iconRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            fontFamily = FontFamily.SansSerif,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            color = colors.title,
        )
        Text(
            text = metricLabel,
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            color = colors.body,
        )
    }
}

@Composable
private fun FeatureSecondaryButton(
    feature: HomeFeature,
    palette: FeaturePalette,
    colors: HomeColors,
    onActionClick: (HomeActionDestination) -> Unit,
    onFeatureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val action = feature.directSecondaryAction
    FeatureActionButton(
        label = action?.title ?: "详情",
        iconRes = action?.destination?.let(::actionIconFor) ?: R.drawable.ic_home_details,
        onClick = { if (action != null) onActionClick(action.destination) else onFeatureClick() },
        containerColor = palette.accent.copy(alpha = 0.06f).compositeOver(colors.surface),
        contentColor = colors.body,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier,
    )
}

@Composable
private fun FeatureActionButton(
    label: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
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
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            )
        }
    }
}

@DrawableRes
private fun actionIconFor(destination: HomeActionDestination): Int = when (destination) {
    HomeActionDestination.DIARY_QUICK_ADD -> R.drawable.ic_home_edit
    HomeActionDestination.ACCOUNTING_QUICK_ENTRY,
    HomeActionDestination.SLEEP_QUICK_RECORD -> R.drawable.ic_home_add
    HomeActionDestination.TIME_LIFE_TRACE -> R.drawable.ic_grid
    HomeActionDestination.TIME_EVENTS -> R.drawable.ic_calendar
    HomeActionDestination.BACKUP_EXPORT -> R.drawable.ic_home_export
    HomeActionDestination.BACKUP_RESTORE -> R.drawable.ic_home_restore
}

@Immutable
private data class HomeColors(
    val background: Color,
    val surface: Color,
    val title: Color,
    val body: Color,
    val muted: Color,
    val border: Color,
    val shadow: Color,
)

private fun homeColors(darkTheme: Boolean): HomeColors = if (darkTheme) {
    HomeColors(
        background = Color(0xFF111A23),
        surface = Color(0xFF1B2733),
        title = Color(0xFFEBF1F7),
        body = Color(0xFFB4C0CE),
        muted = Color(0xFF8E9EAF),
        border = Color.White.copy(alpha = 0.06f),
        shadow = Color.Black.copy(alpha = 0.10f),
    )
} else {
    HomeColors(
        background = Color(0xFFF7F9FC),
        surface = Color.White,
        title = Color(0xFF0F2740),
        body = Color(0xFF5F6F82),
        muted = Color(0xFF8E9AAD),
        border = Color(0xFF0F2740).copy(alpha = 0.05f),
        shadow = Color(0xFF1F3750).copy(alpha = 0.06f),
    )
}

@Immutable
private data class FeaturePalette(val accent: Color, val button: Color, val onButton: Color)

private fun featurePalette(accent: FeatureAccent, darkTheme: Boolean): FeaturePalette {
    if (darkTheme) {
        val color = when (accent) {
            FeatureAccent.PRIMARY -> Color(0xFF8AB8F4)
            FeatureAccent.SECONDARY -> Color(0xFFFFAC78)
            FeatureAccent.QUATERNARY -> Color(0xFFB5A2FF)
            FeatureAccent.QUINARY -> Color(0xFF81C4CF)
            FeatureAccent.TERTIARY -> Color(0xFF83D4AF)
        }
        return FeaturePalette(accent = color, button = color, onButton = Color(0xFF0F2740))
    }
    // Slightly deeper blue/teal/green button tones keep small white labels legible.
    return when (accent) {
        FeatureAccent.PRIMARY -> FeaturePalette(Color(0xFF2F80ED), Color(0xFF2675D8), Color.White)
        FeatureAccent.SECONDARY -> FeaturePalette(Color(0xFFFF8A3D), Color(0xFFFF8A3D), Color(0xFF3D2618))
        FeatureAccent.QUATERNARY -> FeaturePalette(Color(0xFF7C5CFC), Color(0xFF7C5CFC), Color.White)
        FeatureAccent.QUINARY -> FeaturePalette(Color(0xFF2697A6), Color(0xFF1D7784), Color.White)
        FeatureAccent.TERTIARY -> FeaturePalette(Color(0xFF20A86B), Color(0xFF168450), Color.White)
    }
}

private val HomePreviewMetrics = HomeMetrics(
    diaryMonthCount = 0,
    accountingMonthCount = 19,
    sleepMonthCount = 3,
    timeEventCount = 1,
)

@Preview(name = "首页 · 浅色", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Preview(name = "首页 · 窄屏", showBackground = true, showSystemUi = true, widthDp = 320, heightDp = 740)
@Preview(name = "首页 · 大字体", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Preview(name = "首页 · 平板", showBackground = true, showSystemUi = true, widthDp = 800, heightDp = 1100)
@Composable
private fun HomeScreenPreview() {
    BlueTheme(darkTheme = false) {
        HomeScreen(onActionClick = {}, metrics = HomePreviewMetrics)
    }
}

@Preview(
    name = "首页 · 深色",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenDarkPreview() {
    BlueTheme(darkTheme = true) {
        HomeScreen(onActionClick = {}, metrics = HomePreviewMetrics)
    }
}
