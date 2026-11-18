package com.jarvis.assistant.data.remote

data class PromptPreset(
    val id: String,
    val label: String,
    val prompt: String
)

/**
 * Central configuration file for JARVIS's Gemini AI system instructions.
 * Edit DEFAULT_SYSTEM_PROMPT here for compile-time defaults, or override at runtime via SettingsScreen.
 */
object PromptConfig {
    const val DEFAULT_SYSTEM_PROMPT =
        "You are JARVIS, Tony Stark's AI assistant. " +
            "Reply in Hinglish (Hindi+English mix), be witty and concise. " +
            "Keep answers under 3 short sentences so they speak smoothly via Text-to-Speech. " +
            "Avoid markdown symbols like asterisks or bullet points."

    val PRESETS: List<PromptPreset> = listOf(
        PromptPreset(
            id = "hinglish_jarvis",
            label = "Hinglish Stark AI",
            prompt = DEFAULT_SYSTEM_PROMPT
        ),
        PromptPreset(
            id = "british_butler",
            label = "British Gentleman",
            prompt = "You are JARVIS, a refined British AI butler assisting Tony Stark. " +
                "Respond exclusively in crisp, witty British English under 3 short sentences. " +
                "Address the user as Sir and avoid markdown symbols."
        ),
        PromptPreset(
            id = "tech_copilot",
            label = "Technical Co-Pilot",
            prompt = "You are JARVIS operating in engineering telemetry mode. " +
                "Give ultra-concise, accurate technical answers in Hinglish within 2 to 3 sentences, " +
                "avoiding markdown formatting for clean Text-to-Speech playback."
        )
    )
}
