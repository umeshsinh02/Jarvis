package com.jarvis.assistant.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jarvis.assistant.domain.model.AssistantStatus
import com.jarvis.assistant.domain.model.ChatMessage
import com.jarvis.assistant.domain.model.MessageSender
import com.jarvis.assistant.presentation.ui.components.ArcReactorOrb
import com.jarvis.assistant.presentation.ui.components.HolographicChatCard
import com.jarvis.assistant.presentation.ui.components.HudGridBackground
import com.jarvis.assistant.presentation.ui.components.VoiceWaveform
import com.jarvis.assistant.presentation.ui.theme.AlertCrimson
import com.jarvis.assistant.presentation.ui.theme.DarkNavy
import com.jarvis.assistant.presentation.ui.theme.GlassNavy
import com.jarvis.assistant.presentation.ui.theme.IceWhite
import com.jarvis.assistant.presentation.ui.theme.JarvisCyan
import com.jarvis.assistant.presentation.ui.theme.MutedSteel
import com.jarvis.assistant.presentation.ui.theme.NeonBlue
import com.jarvis.assistant.presentation.viewmodel.JarvisViewModel
import com.jarvis.assistant.util.PermissionHelper

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: JarvisViewModel,
    onOpenSettings: () -> Unit,
    onRequestScreenshot: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionHelper.allRuntimePermissions()
    )

    // Register screenshot request callback from ViewModel
    LaunchedEffect(Unit) {
        viewModel.screenshotRequestFlow.collect {
            onRequestScreenshot()
        }
    }

    // Auto-scroll to latest conversation turn
    LaunchedEffect(uiState.messages.size, uiState.streamingReply) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HudGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top HUD Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "J.A.R.V.I.S.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = JarvisCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = uiState.status.displayText,
                        style = MaterialTheme.typography.labelSmall.copy(color = MutedSteel)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AUTO",
                        style = MaterialTheme.typography.labelSmall.copy(color = MutedSteel)
                    )
                    Switch(
                        checked = uiState.isContinuousMode,
                        onCheckedChange = { enabled ->
                            if (!permissionsState.allPermissionsGranted) {
                                permissionsState.launchMultiplePermissionRequest()
                            }
                            viewModel.toggleContinuousListening(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkNavy,
                            checkedTrackColor = JarvisCyan
                        )
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = JarvisCyan
                        )
                    }
                }
            }

            // Runtime permission prompt banner if mic not yet granted
            AnimatedVisibility(visible = !permissionsState.allPermissionsGranted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassNavy)
                        .border(1.dp, JarvisCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grant mic & device permissions for full JARVIS control.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall.copy(color = IceWhite)
                    )
                    TextButton(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                        Text("ALLOW", color = JarvisCyan)
                    }
                }
            }

            // Error Banner
            uiState.errorMessage?.let { error ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AlertCrimson.copy(alpha = 0.16f))
                        .border(1.dp, AlertCrimson.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall.copy(color = IceWhite)
                    )
                    TextButton(onClick = { viewModel.dismissError() }) {
                        Text("OK", color = AlertCrimson)
                    }
                }
            }

            // Center Arc Reactor Orb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcReactorOrb(
                    status = uiState.status,
                    audioAmplitude = uiState.audioAmplitude,
                    size = 180.dp,
                    onClick = {
                        if (!permissionsState.allPermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                        } else {
                            viewModel.onOrbTapped()
                        }
                    }
                )
            }

            // Partial Transcript Preview
            AnimatedVisibility(visible = uiState.partialTranscript.isNotBlank()) {
                Text(
                    text = ""${uiState.partialTranscript}"",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(color = JarvisCyan)
                )
            }

            // Holographic Chat History
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    HolographicChatCard(message = msg)
                }

                if (uiState.streamingReply.isNotBlank()) {
                    item(key = "streaming_reply") {
                        HolographicChatCard(
                            message = ChatMessage(
                                id = -1L,
                                sender = MessageSender.JARVIS,
                                content = uiState.streamingReply
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Voice Waveform Visualizer
            VoiceWaveform(
                status = uiState.status,
                amplitude = uiState.audioAmplitude
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Control Dock: Text fallback + Push-to-Talk Mic Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Ask JARVIS or type command...",
                            color = MutedSteel
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = NeonBlue.copy(alpha = 0.45f),
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite,
                        focusedContainerColor = GlassNavy,
                        unfocusedContainerColor = GlassNavy
                    ),
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.submitUserPrompt(textInput)
                                    textInput = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = JarvisCyan
                                )
                            }
                        }
                    }
                )

                FilledIconButton(
                    onClick = {
                        if (!permissionsState.allPermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                        } else {
                            viewModel.onOrbTapped()
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, JarvisCyan, CircleShape),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (uiState.status == AssistantStatus.LISTENING) {
                            AlertCrimson
                        } else {
                            NeonBlue.copy(alpha = 0.35f)
                        },
                        contentColor = JarvisCyan
                    )
                ) {
                    Icon(
                        imageVector = when (uiState.status) {
                            AssistantStatus.LISTENING -> Icons.Default.Stop
                            AssistantStatus.SPEAKING -> Icons.Default.GraphicEq
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Push to Talk",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
