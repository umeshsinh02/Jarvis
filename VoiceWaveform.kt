package com.jarvis.assistant.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.domain.model.AssistantStatus
import com.jarvis.assistant.presentation.ui.theme.JarvisCyan
import com.jarvis.assistant.presentation.ui.theme.NeonBlue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun VoiceWaveform(
    status: AssistantStatus,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing)
        ),
        label = "wave_phase"
    )

    val barCount = 28
    val effectiveEnergy = when (status) {
        AssistantStatus.LISTENING -> (0.25f + amplitude * 0.75f).coerceIn(0.2f, 1f)
        AssistantStatus.SPEAKING -> 0.72f
        AssistantStatus.THINKING -> 0.35f
        AssistantStatus.IDLE -> 0.08f
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val totalWidth = size.width
        val slotWidth = totalWidth / barCount
        val barWidth = (slotWidth * 0.48f).coerceAtLeast(4f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normX = (i.toFloat() / (barCount - 1)) * 2f - 1f
            val envelope = 1f - (normX * normX) * 0.65f
            val wave = abs(sin(phase + i * 0.45f))
            val barHeight = (size.height * 0.14f + size.height * 0.82f * wave * effectiveEnergy * envelope)
                .coerceAtMost(size.height)

            val left = i * slotWidth + (slotWidth - barWidth) / 2f
            val top = centerY - barHeight / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(JarvisCyan, NeonBlue)
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                alpha = if (status == AssistantStatus.IDLE) 0.35f else 0.92f
            )
        }
    }
}
