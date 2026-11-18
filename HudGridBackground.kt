package com.jarvis.assistant.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.jarvis.assistant.presentation.ui.theme.DarkNavy
import com.jarvis.assistant.presentation.ui.theme.JarvisCyan
import com.jarvis.assistant.presentation.ui.theme.NeonBlue

@Composable
fun HudGridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Deep Dark Navy atmospheric backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF101C48), DarkNavy),
                center = Offset(size.width / 2f, size.height * 0.35f),
                radius = size.maxDimension * 0.75f
            )
        )

        val gridSpacing = 64f
        val lineColor = JarvisCyan.copy(alpha = 0.045f)

        var x = 0f
        while (x < size.width) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += gridSpacing
        }
        var y = 0f
        while (y < size.height) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += gridSpacing
        }

        // Subtle outer corner reticles
        val bracketLen = 36f
        val bracketColor = NeonBlue.copy(alpha = 0.35f)
        drawLine(bracketColor, Offset(24f, 48f), Offset(24f + bracketLen, 48f), 2f)
        drawLine(bracketColor, Offset(24f, 48f), Offset(24f, 48f + bracketLen), 2f)

        drawLine(
            bracketColor,
            Offset(size.width - 24f, 48f),
            Offset(size.width - 24f - bracketLen, 48f),
            2f
        )
        drawLine(
            bracketColor,
            Offset(size.width - 24f, 48f),
            Offset(size.width - 24f, 48f + bracketLen),
            2f
        )
    }
}
