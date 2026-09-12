package com.example.blue.feature.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

object AppMotion {
    const val PressInMillis = 80
    const val PressOutMillis = 120
}

/**
 * Adds a small draw-layer-only scale response to an existing clickable component.
 * The caller owns [interactionSource], so ripple, semantics, and this motion observe
 * the same press without adding another pointer-input handler.
 */
@Composable
fun Modifier.appPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed = interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed.value) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = if (pressed.value) AppMotion.PressInMillis else AppMotion.PressOutMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "appPressScale",
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
