package com.jarvis.assistant.domain.model

sealed class LocalCommand {
    data class OpenApp(val appName: String) : LocalCommand()
    data class CallContact(val contactName: String) : LocalCommand()
    data class SendSms(val contactName: String, val messageBody: String?) : LocalCommand()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : LocalCommand()
    data class CheckWeather(val city: String = "Mumbai") : LocalCommand()
    data object BatteryStatus : LocalCommand()
    data object TakeScreenshot : LocalCommand()
}
