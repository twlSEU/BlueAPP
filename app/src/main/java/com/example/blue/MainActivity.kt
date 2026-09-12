package com.example.blue

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.blue.ui.theme.BlueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.argb(0xE6, 0xE0, 0xEB, 0xF5),
                darkScrim = Color.argb(0xE6, 0x10, 0x18, 0x20),
            ),
        )
        setContent {
            BlueTheme {
                LifeRecordApp()
            }
        }
    }
}
