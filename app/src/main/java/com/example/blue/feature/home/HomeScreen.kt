package com.example.blue.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.blue.core.navigation.AppDestination
import com.example.blue.ui.theme.BlueTheme

private val HomeBackground = Color(0xFFF5F8FC)
private val HomeTitle = Color(0xFF243B47)
private val HomeBody = Color(0xFF70838E)

@Composable
fun HomeScreen(
    onActionClick: (HomeActionDestination) -> Unit,
    modifier: Modifier = Modifier,
    onFeatureClick: (AppDestination) -> Unit = {},
    metrics: HomeMetrics = HomeMetrics(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HomeBackground,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "welcome") {
                    HomeIntroduction()
                }
                items(
                    items = homeFeatures,
                    key = { feature -> feature.destination.route },
                    contentType = { "home-feature" },
                ) { feature ->
                    FeatureCard(
                        feature = feature,
                        metricLabel = metrics.labelFor(feature.destination),
                        onActionClick = onActionClick,
                        onFeatureClick = { onFeatureClick(feature.destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeIntroduction() {
    Column(
        modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "悟已往之不谏",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = HomeTitle,
        )
        Text(
            text = "往日暗沉不可追，来日之路光明灿烂",
            style = MaterialTheme.typography.bodyMedium,
            color = HomeBody,
        )
    }
}

@Composable
private fun FeatureCard(
    feature: HomeFeature,
    metricLabel: String,
    onActionClick: (HomeActionDestination) -> Unit,
    onFeatureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(feature.accent) { featurePalette(feature.accent) }
    val primaryAction = feature.primaryAction
    val directSecondaryAction = feature.directSecondaryAction
    val cardShape = RoundedCornerShape(26.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                ambientColor = palette.glow,
                spotColor = palette.glow,
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White, Color.White, palette.soft.copy(alpha = 0.34f)),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.width(58.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FeatureDimensionalIcon(feature = feature, palette = palette)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = feature.destination.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HomeTitle,
                        maxLines = 1,
                    )
                    FeatureMetricPill(label = metricLabel, palette = palette)
                }
                Column(
                    modifier = Modifier.width(90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (directSecondaryAction != null) {
                        // Time and data management expose both actions directly from home.
                        FeaturePrimaryButton(
                            action = directSecondaryAction,
                            palette = palette,
                            onClick = { onActionClick(directSecondaryAction.destination) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        FeatureEnterButton(
                            palette = palette,
                            onClick = onFeatureClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    FeaturePrimaryButton(
                        action = primaryAction,
                        palette = palette,
                        onClick = { onActionClick(primaryAction.destination) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureDimensionalIcon(
    feature: HomeFeature,
    palette: FeaturePalette,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(19.dp)
    Box(
        modifier = modifier
            .size(58.dp)
            .shadow(
                elevation = 9.dp,
                shape = shape,
                ambientColor = palette.glow,
                spotColor = palette.glow,
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(palette.iconStart, palette.iconEnd))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 7.dp)
                .size(width = 26.dp, height = 9.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
        )
        Box(
            modifier = Modifier
                .offset(x = 2.dp, y = 3.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.08f)),
        )
        Icon(
            painter = painterResource(feature.iconRes),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = Color.White,
        )
    }
}

@Composable
private fun FeatureMetricPill(label: String, palette: FeaturePalette) {
    Surface(
        shape = CircleShape,
        color = palette.soft.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(palette.accent),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = HomeTitle.copy(alpha = 0.82f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FeatureEnterButton(
    palette: FeaturePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "详情",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
            )
        }
    }
}

@Composable
private fun FeaturePrimaryButton(
    action: HomeAction,
    palette: FeaturePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .shadow(
                elevation = 7.dp,
                shape = CircleShape,
                ambientColor = palette.glow,
                spotColor = palette.glow,
            )
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(palette.iconStart, palette.iconEnd)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = action.symbol,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

private data class FeaturePalette(
    val iconStart: Color,
    val iconEnd: Color,
    val accent: Color,
    val soft: Color,
    val glow: Color,
)

private fun featurePalette(accent: FeatureAccent): FeaturePalette =
    when (accent) {
        FeatureAccent.PRIMARY -> FeaturePalette(
            iconStart = Color(0xFF69B9FF),
            iconEnd = Color(0xFF3978E8),
            accent = Color(0xFF3978E8),
            soft = Color(0xFFEAF4FF),
            glow = Color(0x553D8CF2),
        )
        FeatureAccent.SECONDARY -> FeaturePalette(
            iconStart = Color(0xFFFFBD70),
            iconEnd = Color(0xFFE77B43),
            accent = Color(0xFFC96B38),
            soft = Color(0xFFFFF1E5),
            glow = Color(0x44F19A55),
        )
        FeatureAccent.TERTIARY -> FeaturePalette(
            iconStart = Color(0xFF75C99A),
            iconEnd = Color(0xFF3D8B65),
            accent = Color(0xFF3E8663),
            soft = Color(0xFFEAF7F0),
            glow = Color(0x444FA879),
        )
        FeatureAccent.QUATERNARY -> FeaturePalette(
            iconStart = Color(0xFFA69BFF),
            iconEnd = Color(0xFF6B5DD2),
            accent = Color(0xFF685BC5),
            soft = Color(0xFFF0EEFF),
            glow = Color(0x446F63DF),
        )
        FeatureAccent.QUINARY -> FeaturePalette(
            iconStart = Color(0xFF85BBC7),
            iconEnd = Color(0xFF4E7882),
            accent = Color(0xFF4F7780),
            soft = Color(0xFFECF5F6),
            glow = Color(0x445D929D),
        )
    }

@Preview(
    name = "首页 · 四张功能卡片",
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun HomeScreenPreview() {
    BlueTheme(dynamicColor = false) {
        HomeScreen(
            onActionClick = {},
            metrics = HomeMetrics(
                diaryMonthCount = 12,
                accountingMonthCount = 28,
                sleepMonthCount = 14,
                timeEventCount = 36,
            ),
        )
    }
}
