package com.jarvis.assistant.domain.usecase

import com.jarvis.assistant.domain.model.LocalCommand
import java.util.Locale
import javax.inject.Inject

/**
 * Evaluates spoken or typed user input locally for instant device actions
 * before routing conversational queries to Gemini 1.5 Flash.
 */
class ParseLocalCommandUseCase @Inject constructor() {

    operator fun invoke(rawInput: String): LocalCommand? {
        val text = rawInput.trim()
        val lower = text.lowercase(Locale.getDefault())

        // 1. Open [app name]
        val openRegex = Regex("^(?:hey jarvis[,\\s]+)?(?:open|launch|start)\\s+(.+)$")
        openRegex.find(lower)?.let { match ->
            val appName = match.groupValues[1].trim()
            if (appName.isNotEmpty()) return LocalCommand.OpenApp(appName)
        }

        // 2. Call [contact]
        val callRegex = Regex("^(?:hey jarvis[,\\s]+)?(?:call|dial|phone)\\s+(.+)$")
        callRegex.find(lower)?.let { match ->
            val contact = match.groupValues[1].trim()
            if (contact.isNotEmpty()) return LocalCommand.CallContact(contact)
        }

        // 3. Send message to [contact] (optional: saying/that [body])
        val smsWithBodyRegex = Regex(
            "^(?:hey jarvis[,\\s]+)?(?:send (?:a )?(?:message|sms|text) to|message|text)\\s+(.+?)\\s+(?:saying|that|:)\\s+(.+)$"
        )
        smsWithBodyRegex.find(lower)?.let { match ->
            return LocalCommand.SendSms(
                contactName = match.groupValues[1].trim(),
                messageBody = match.groupValues[2].trim()
            )
        }
        val smsSimpleRegex = Regex(
            "^(?:hey jarvis[,\\s]+)?(?:send (?:a )?(?:message|sms|text) to|message|text)\\s+(.+)$"
        )
        smsSimpleRegex.find(lower)?.let { match ->
            return LocalCommand.SendSms(
                contactName = match.groupValues[1].trim(),
                messageBody = null
            )
        }

        // 4. Set alarm for [time] e.g. "set alarm for 7:30 am" or "set alarm for 18:00"
        if (lower.contains("alarm")) {
            val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)?")
            val match = timeRegex.find(lower)
            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: 7
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val meridiem = match.groupValues[3].replace(".", "")

                if (meridiem == "pm" && hour < 12) hour += 12
                if (meridiem == "am" && hour == 12) hour = 0

                if (hour in 0..23 && minute in 0..59) {
                    return LocalCommand.SetAlarm(hour, minute, "JARVIS Alarm")
                }
            }
        }

        // 5. Weather query
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("temperature")) {
            val cityMatch = Regex("(?:weather|temperature)(?:\\s+in|\\s+at|\\s+for)\\s+([a-zA-Z\\s]+)")
                .find(lower)
            val city = cityMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: "Mumbai"
            return LocalCommand.CheckWeather(city)
        }

        // 6. Battery status
        if (lower.contains("battery") || lower.contains("charging") || lower.contains("power status")) {
            return LocalCommand.BatteryStatus
        }

        // 7. Screenshot
        if (lower.contains("screenshot") || lower.contains("screen shot") || lower.contains("capture screen")) {
            return LocalCommand.TakeScreenshot
        }

        return null
    }
}
