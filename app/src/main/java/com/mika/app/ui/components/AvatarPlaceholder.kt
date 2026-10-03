package com.mika.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mika.app.ui.theme.PinkPrimary
import com.mika.app.ui.theme.PurpleAccent

@Composable
fun AvatarPlaceholder(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar() ?: 'M'
    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            PinkPrimary,
            PurpleAccent,
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .background(brush = gradientBrush, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45).sp
        )
    }
}
