package com.example.blue.feature.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.blue.R

private val BackButtonIconColor = Color(0xFF263B4A)
private val BackButtonShadowColor = Color(0xFF7890A3)

@Composable
fun AppBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(start = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = 7.dp,
                    shape = CircleShape,
                    ambientColor = BackButtonShadowColor.copy(alpha = 0.18f),
                    spotColor = BackButtonShadowColor.copy(alpha = 0.28f),
                ),
            shape = CircleShape,
            color = Color.White,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "返回",
                    modifier = Modifier.size(20.dp),
                    tint = BackButtonIconColor,
                )
            }
        }
    }
}
