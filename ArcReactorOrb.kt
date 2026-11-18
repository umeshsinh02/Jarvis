package com.jarvis.assistant.presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.domain.model.AssistantStatus
import com.jarvis.assistant.presentation.ui.theme.JarvisCyan
import com.jarvis.assistant.presentation.ui.theme.NeonBlue

@Composable
fun ArcReactorOrb(
    status: AssistantStatus,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    AssistantStatus.THINKING -> 2600
                    AssistantStatus.LISTENING -> 4200
                    AssistantStatus.SPEAKING -> 3400
                    AssistantStatus.IDLE -> 11000
                },
                easing = LinearEasing
            )
        ),
        label = "outer_rotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing)
        ),
        label = "inner_rotation"
    )

    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    val smoothAmp by animateFloatAsState(
        targetValue = audioAmplitude.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 90),
        label = "smooth_amplitude"
    )

    val coreScale = when (status) {
        AssistantStatus.LISTENING -> breathingPulse + smoothAmp * 0.22f
        AssistantStatus.SPEAKING -> breathingPulse + 0.12f
        AssistantStatus.THINKING -> breathingPulse
        AssistantStatus.IDLE -> breathingPulse * 0.96f
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.minDimension / 2f

            // Ambient radial halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JarvisCyan.copy(alpha = if (status == AssistantStatus.IDLE) 0.22f else 0.42f),
                        NeonBlue.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * coreScale
                ),
                radius = maxRadius * coreScale,
                center = center
            )

            // Outer segmented HUD ring
            rotate(degrees = outerRotation, pivot = center) {
                val ringRadius = maxRadius * 0.84f
                val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
                val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                for (i in 0 until 6) {
                    drawArc(
                        color = JarvisCyan.copy(alpha = if (i % 2 == 0) 0.85f else 0.35f),
                        startAngle = i * 60f + 8f,
                        sweepAngle = 42f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )
                }
            }

            // Counter-rotating inner tactical ring
            rotate(degrees = innerRotation, pivot = center) {
                val innerRadius = maxRadius * 0.64f
                val arcSize = Size(innerRadius * 2f, innerRadius * 2f)
                val topLeft = Offset(center.x - innerRadius, center.y - innerRadius)
                for (i in 0 until 4) {
                    drawArc(
                        color = NeonBlue.copy(alpha = 0.8f),
                        startAngle = i * 90f + 12f,
                        sweepAngle = 62f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }
            }

            // Pulsing Arc Reactor core
            val coreRadius = maxRadius * 0.34f * coreScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        JarvisCyan,
                        NeonBlue.copy(alpha = 0.85f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            drawCircle(
                color = JarvisCyan.copy(alpha = 0.9f),
                radius = maxRadius * 0.44f,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }
    }
}
