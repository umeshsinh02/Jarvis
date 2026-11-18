package com.jarvis.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JarvisApplication : Application() {

    companion object {
        const val CHANNEL_VOICE = "jarvis_voice_channel"
        const val CHANNEL_WAKE = "jarvis_wake_channel"
        const val CHANNEL_SCREENSHOT = "jarvis_screenshot_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val voiceChannel = NotificationChannel(
                CHANNEL_VOICE,
                getString(R.string.channel_voice_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps JARVIS continuous voice loop active"
            }

            val wakeChannel = NotificationChannel(
                CHANNEL_WAKE,
                getString(R.string.channel_wake_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listens passively for Hey JARVIS wake word"
            }

            val screenshotChannel = NotificationChannel(
                CHANNEL_SCREENSHOT,
                "JARVIS Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )

            manager.createNotificationChannels(
                listOf(voiceChannel, wakeChannel, screenshotChannel)
            )
        }
    }
}
