package com.jarvis.assistant.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.domain.model.ChatMessage
import com.jarvis.assistant.domain.model.MessageSender
import com.jarvis.assistant.presentation.ui.theme.IceWhite
import com.jarvis.assistant.presentation.ui.theme.JarvisCyan
import com.jarvis.assistant.presentation.ui.theme.MutedSteel
import com.jarvis.assistant.presentation.ui.theme.NeonBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HolographicChatCard(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )

    val borderColor = if (isUser) {
        NeonBlue.copy(alpha = 0.45f)
    } else {
        JarvisCyan.copy(alpha = 0.45f)
    }

    val cardBrush = if (isUser) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x660088FF),
                Color(0x440F1638)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0x5500E5FF),
                Color(0x770F1638)
            )
        )
    }

    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(shape)
                .background(cardBrush)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = when {
                    isUser -> "SIR"
                    message.isCommandExecution -> "JARVIS · ${message.commandType ?: "SYSTEM"}"
                    else -> "JARVIS"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isUser) NeonBlue else JarvisCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutedSteel,
                        fontSize = 10.sp
                    )
                )
            }

            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium.copy(color = IceWhite)
            )
        }
    }
}
