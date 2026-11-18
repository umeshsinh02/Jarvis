package com.jarvis.assistant.domain.usecase

import com.jarvis.assistant.domain.model.LocalCommand
import com.jarvis.assistant.domain.repository.WeatherRepository
import com.jarvis.assistant.util.CommandExecutor
import com.jarvis.assistant.util.DeviceInfoHelper
import javax.inject.Inject

sealed class CommandOutcome {
    data class HandledLocally(val responseText: String, val commandLabel: String) : CommandOutcome()
    data object RequestScreenshotPermission : CommandOutcome()
    data object DelegateToGemini : CommandOutcome()
}

class ProcessVoiceCommandUseCase @Inject constructor(
    private val parseLocalCommand: ParseLocalCommandUseCase,
    private val commandExecutor: CommandExecutor,
    private val deviceInfoHelper: DeviceInfoHelper,
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(input: String, weatherApiKey: String): CommandOutcome {
        val command = parseLocalCommand(input) ?: return CommandOutcome.DelegateToGemini

        return when (command) {
            is LocalCommand.OpenApp -> {
                val reply = commandExecutor.openAppByName(command.appName)
                CommandOutcome.HandledLocally(reply, "OPEN_APP")
            }
            is LocalCommand.CallContact -> {
                val reply = commandExecutor.initiateCall(command.contactName)
                CommandOutcome.HandledLocally(reply, "CALL_PHONE")
            }
            is LocalCommand.SendSms -> {
                val reply = commandExecutor.sendSmsToContact(command.contactName, command.messageBody)
                CommandOutcome.HandledLocally(reply, "SEND_SMS")
            }
            is LocalCommand.SetAlarm -> {
                val reply = commandExecutor.setAlarm(command.hour, command.minute, command.label)
                CommandOutcome.HandledLocally(reply, "SET_ALARM")
            }
            is LocalCommand.BatteryStatus -> {
                val reply = deviceInfoHelper.getBatterySummary()
                CommandOutcome.HandledLocally(reply, "BATTERY_STATUS")
            }
            is LocalCommand.CheckWeather -> {
                val weatherResult = weatherRepository.fetchWeatherSummary(command.city, weatherApiKey)
                val reply = weatherResult.getOrElse {
                    "Sir, weather servers se connect nahi ho paaya: ${it.localizedMessage ?: "Network issue"}."
                }
                CommandOutcome.HandledLocally(reply, "WEATHER")
            }
            is LocalCommand.TakeScreenshot -> {
                CommandOutcome.RequestScreenshotPermission
            }
        }
    }
}
