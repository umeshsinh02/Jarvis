package com.jarvis.assistant.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.data.remote.PromptConfig
import com.jarvis.assistant.domain.model.HudThemeStyle
import com.jarvis.assistant.domain.model.JarvisVoiceEngine
import com.jarvis.assistant.presentation.ui.components.HudGridBackground
import com.jarvis.assistant.presentation.ui.theme.AlertCrimson
import com.jarvis.assistant.presentation.ui.theme.DarkNavy
import com.jarvis.assistant.presentation.ui.theme.GlassNavy
import com.jarvis.assistant.presentation.ui.theme.IceWhite
import com.jarvis.assistant.presentation.ui.theme.JarvisCyan
import com.jarvis.assistant.presentation.ui.theme.MutedSteel
import com.jarvis.assistant.presentation.ui.theme.NeonBlue
import com.jarvis.assistant.presentation.viewmodel.SettingsViewModel
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var picovoiceKey by remember(settings.picovoiceAccessKey) { mutableStateOf(settings.picovoiceAccessKey) }
    var weatherKey by remember(settings.weatherApiKey) { mutableStateOf(settings.weatherApiKey) }
    var systemPromptText by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
    var savedFeedback by remember { mutableStateOf(false) }
    var promptSavedFeedback by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        HudGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = JarvisCyan
                    )
                }
                Text(
                    text = "JARVIS CORE SETTINGS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = JarvisCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // API Keys Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassNavy)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "API CREDENTIALS",
                    style = MaterialTheme.typography.labelSmall.copy(color = JarvisCyan)
                )

                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = {
                        geminiKey = it
                        savedFeedback = false
                    },
                    label = { Text("Google Gemini API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = NeonBlue.copy(alpha = 0.4f),
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    )
                )

                OutlinedTextField(
                    value = picovoiceKey,
                    onValueChange = {
                        picovoiceKey = it
                        savedFeedback = false
                    },
                    label = { Text("Picovoice AccessKey (Wake Word)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = NeonBlue.copy(alpha = 0.4f),
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    )
                )

                OutlinedTextField(
                    value = weatherKey,
                    onValueChange = {
                        weatherKey = it
                        savedFeedback = false
                    },
                    label = { Text("OpenWeather API Key (Optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = NeonBlue.copy(alpha = 0.4f),
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    )
                )

                Button(
                    onClick = {
                        viewModel.saveApiKeys(geminiKey, picovoiceKey, weatherKey)
                        savedFeedback = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan,
                        contentColor = DarkNavy
                    )
                ) {
                    Text(
                        text = if (savedFeedback) "CREDENTIALS SAVED" else "SAVE API KEYS",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Gemini AI System Prompt Configuration Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassNavy)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI BRAIN SYSTEM PROMPT",
                    style = MaterialTheme.typography.labelSmall.copy(color = JarvisCyan)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PromptConfig.PRESETS.forEach { preset ->
                        FilterChip(
                            selected = systemPromptText.trim() == preset.prompt.trim(),
                            onClick = {
                                systemPromptText = preset.prompt
                                promptSavedFeedback = false
                            },
                            label = { Text(preset.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan,
                                selectedLabelColor = DarkNavy,
                                labelColor = IceWhite
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = systemPromptText,
                    onValueChange = {
                        systemPromptText = it
                        promptSavedFeedback = false
                    },
                    label = { Text("Gemini System Instruction") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = NeonBlue.copy(alpha = 0.4f),
                        focusedTextColor = IceWhite,
                        unfocusedTextColor = IceWhite
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            systemPromptText = PromptConfig.DEFAULT_SYSTEM_PROMPT
                            viewModel.resetSystemPrompt()
                            promptSavedFeedback = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan)
                    ) {
                        Text("RESET DEFAULT")
                    }

                    Button(
                        onClick = {
                            viewModel.updateSystemPrompt(systemPromptText)
                            promptSavedFeedback = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = DarkNavy
                        )
                    ) {
                        Text(
                            text = if (promptSavedFeedback) "PROMPT SAVED" else "SAVE PROMPT",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Voice & Wake Word Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassNavy)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "VOICE ENGINE & WAKE WORD",
                    style = MaterialTheme.typography.labelSmall.copy(color = JarvisCyan)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    JarvisVoiceEngine.entries.forEach { engine ->
                        FilterChip(
                            selected = settings.voiceEngine == engine,
                            onClick = { viewModel.setVoiceEngine(engine) },
                            label = { Text(engine.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan,
                                selectedLabelColor = DarkNavy,
                                labelColor = IceWhite
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stark HUD Acoustic Chimes", color = IceWhite)
                        Text(
                            text = "Play arc-tone cue on voice activation",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSteel)
                        )
                    }
                    Switch(
                        checked = settings.hudChimesEnabled,
                        onCheckedChange = { viewModel.toggleHudChimes(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkNavy,
                            checkedTrackColor = JarvisCyan
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake Word ("Hey JARVIS")", color = IceWhite)
                        Text(
                            text = "Uses Picovoice Porcupine offline service",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutedSteel)
                        )
                    }
                    Switch(
                        checked = settings.wakeWordEnabled,
                        onCheckedChange = { viewModel.toggleWakeWord(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkNavy,
                            checkedTrackColor = JarvisCyan
                        )
                    )
                }

                Text(
                    text = String.format(Locale.getDefault(), "Voice Speed: %.2fx", settings.voiceSpeed),
                    color = IceWhite
                )
                Slider(
                    value = settings.voiceSpeed,
                    onValueChange = { viewModel.updateVoiceSpeed(it) },
                    valueRange = 0.65f..1.65f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyan,
                        activeTrackColor = JarvisCyan
                    )
                )

                Text(
                    text = String.format(Locale.getDefault(), "Voice Pitch: %.2f", settings.voicePitch),
                    color = IceWhite
                )
                Slider(
                    value = settings.voicePitch,
                    onValueChange = { viewModel.updateVoicePitch(it) },
                    valueRange = 0.65f..1.35f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonBlue,
                        activeTrackColor = NeonBlue
                    )
                )
            }

            // HUD Theme Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassNavy)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "HUD ACCENT THEME",
                    style = MaterialTheme.typography.labelSmall.copy(color = JarvisCyan)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HudThemeStyle.entries.forEach { style ->
                        FilterChip(
                            selected = settings.themeStyle == style,
                            onClick = { viewModel.setThemeStyle(style) },
                            label = { Text(style.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan,
                                selectedLabelColor = DarkNavy,
                                labelColor = IceWhite
                            )
                        )
                    }
                }
            }

            // Clear Conversation Memory
            OutlinedButton(
                onClick = { viewModel.clearChatHistory() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("CLEAR MEMORY LOGS")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
