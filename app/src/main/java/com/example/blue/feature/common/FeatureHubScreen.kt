package com.example.blue.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Immutable
data class FeatureHubTab(
    val key: String,
    val label: String,
)

private val HubBackground = Color(0xFFF6F8FA)
private val HubSurface = Color(0xFFFEFFFF)
private val HubMuted = Color(0xFF91A0A8)
private val HubDivider = Color(0xFFE7ECEF)

/**
 * A compact, fixed header and a horizontally swipeable group of feature pages.
 * The indicator position is derived directly from the pager offset so it stays
 * attached to the user's finger instead of jumping after a page settles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureHubScreen(
    tabs: List<FeatureHubTab>,
    accentColor: Color,
    modifier: Modifier = Modifier,
    pageContent: @Composable (Int) -> Unit,
) {
    require(tabs.isNotEmpty())
    val pagerState = rememberPagerState(pageCount = tabs::size)
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HubBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HubSurface)
                    .statusBarsPadding(),
            ) {
                FeatureHubTabBar(
                    tabs = tabs,
                    selectedPage = pagerState.currentPage,
                    pagerState = pagerState,
                    accentColor = accentColor,
                    onTabClick = { page ->
                        if (page != pagerState.currentPage && !pagerState.isScrollInProgress) {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = page,
                                    animationSpec = tween(durationMillis = 240),
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            key = { page -> tabs[page].key },
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                pageContent(page)
            }
        }
    }
}

@Composable
private fun FeatureHubTabBar(
    tabs: List<FeatureHubTab>,
    selectedPage: Int,
    pagerState: PagerState,
    accentColor: Color,
    onTabClick: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(HubSurface),
    ) {
        val itemWidth = maxWidth / tabs.size
        val indicatorWidth = 28.dp.coerceAtMost(itemWidth - 16.dp)
        val density = LocalDensity.current
        val itemWidthPx = with(density) { itemWidth.toPx() }
        val indicatorInsetPx = with(density) { ((itemWidth - indicatorWidth) / 2).toPx() }

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedPage
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(role = Role.Tab) { onTabClick(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) accentColor else HubMuted,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset {
                    // Read the rapidly changing pager offset during placement, not composition.
                    val pagePosition = (
                        pagerState.currentPage + pagerState.currentPageOffsetFraction
                        ).coerceIn(0f, tabs.lastIndex.toFloat())
                    IntOffset(
                        x = (itemWidthPx * pagePosition + indicatorInsetPx).roundToInt(),
                        y = 0,
                    )
                }
                .padding(bottom = 4.dp)
                .size(width = indicatorWidth, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(HubDivider),
        )
    }
}
