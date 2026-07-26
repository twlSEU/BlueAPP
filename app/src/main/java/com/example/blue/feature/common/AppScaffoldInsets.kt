package com.example.blue.feature.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable

/**
 * Standalone screens own system-bar insets, while pages hosted by FeatureHubScreen
 * inherit the insets already applied and consumed by the parent scaffold.
 */
@Composable
fun appScaffoldContentWindowInsets(showTopBar: Boolean): WindowInsets =
    if (showTopBar) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0)
