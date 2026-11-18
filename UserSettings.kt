package com.jarvis.assistant.domain.model

import com.jarvis.assistant.data.remote.PromptConfig

enum class HudThemeStyle(val label: String) {
    ARC_CYAN("Arc Reactor Cyan"),
    STARK_COBALT("Stark Neon Blue"),
    STEALTH_ICE("Stealth Ice")
}

enum class JarvisVoiceEngine(val label: String) {
    CINEMATIC_NEURAL("Cinematic JARVIS Voice"),
    BRITISH_BARITONE("Offline British Baritone")
}

data class UserSettings(
    val geminiApiKey: String = "",
    val picovoiceAccessKey: String = "",
    val weatherApiKey: String = "",
    val systemPrompt: String = PromptConfig.DEFAULT_SYSTEM_PROMPT,
    val voiceEngine: JarvisVoiceEngine = JarvisVoiceEngine.CINEMATIC_NEURAL,
    val hudChimesEnabled: Boolean = true,
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 0.86f,
    val continuousListening: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val themeStyle: HudThemeStyle = HudThemeStyle.ARC_CYAN
)
