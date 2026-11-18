package com.jarvis.assistant.presentation.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.domain.model.AssistantStatus
import com.jarvis.assistant.presentation.ui.components.ArcReactorOrb
import com.jarvis.assistant.presentation.ui.components.HudGridBackground
import com.jarvis.assistant.presentation.ui.theme.JarvisCyan
import com.jarvis.assistant.presentation.ui.theme.MutedSteel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.65f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700)
        )
        delay(950L)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HudGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale.value),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArcReactorOrb(
                status = AssistantStatus.THINKING,
                audioAmplitude = 0.4f,
                size = 190.dp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "J.A.R.V.I.S.",
                modifier = Modifier.alpha(alpha.value),
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = JarvisCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "JUST A RATHER VERY INTELLIGENT SYSTEM",
                modifier = Modifier.alpha(alpha.value),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MutedSteel,
                    letterSpacing = 1.8.sp
                )
            )
        }
    }
}
