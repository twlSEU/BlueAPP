package com.example.blue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.blue.core.navigation.LifeRecordNavHost

@Composable
fun LifeRecordApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as LifeRecordApplication
    val container = application.container

    LaunchedEffect(container) {
        // Default accounting categories are not needed to draw home. Starting this work
        // after the first frame avoids competing with cold-start composition and drawing.
        withFrameNanos { }
        container.initialize()
    }
    LifeRecordNavHost(
        navController = navController,
        container = container,
        modifier = modifier,
    )
}
