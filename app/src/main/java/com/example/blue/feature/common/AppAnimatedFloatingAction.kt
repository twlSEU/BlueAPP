package com.example.blue.feature.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/** Lightweight, shared entrance/exit motion for screen-level floating actions. */
@Composable
fun AppAnimatedFloatingAction(
    visible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) { visibility.targetState = visible }
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.92f),
        exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.94f),
        content = { content() },
    )
}
