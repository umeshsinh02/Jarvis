package com.jarvis.assistant.service

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.MainActivity

/**
 * Low-power offline wake-word engine powered by Picovoice Porcupine.
 * Triggers MainActivity voice capture whenever "JARVIS" / "Hey JARVIS" is detected.
 */
class WakeWordService : Service() {

    companion object {
        private const val TAG = "JarvisWakeWord"
        private const val NOTIFICATION_ID = 1002
        const val EXTRA_ACCESS_KEY = "extra_picovoice_access_key"
        const val EXTRA_WAKE_TRIGGERED = "extra_wake_triggered"
        const val ACTION_STOP = "com.jarvis.assistant.ACTION_STOP_WAKE"
    }

    private var porcupineManager: PorcupineManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopWakeEngine()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification("Listening for "JARVIS" wake word")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val accessKey = intent?.getStringExtra(EXTRA_ACCESS_KEY).orEmpty()
        if (accessKey.isNotBlank()) {
            startPorcupine(accessKey)
        } else {
            Log.w(TAG, "Picovoice AccessKey missing; stopping WakeWordService.")
            stopSelf()
        }

        return START_STICKY
    }

    private fun startPorcupine(accessKey: String) {
        stopWakeEngine()
        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .setSensitivity(0.65f)
                .build(applicationContext) { _ ->
                    // Wake word detected! Launch or bring MainActivity to front and start STT.
                    val wakeIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(EXTRA_WAKE_TRIGGERED, true)
                    }
                    startActivity(wakeIntent)
                }
            porcupineManager?.start()
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine initialization failed: ${e.message}", e)
            stopSelf()
        }
    }

    private fun stopWakeEngine() {
        runCatching {
            porcupineManager?.stop()
            porcupineManager?.delete()
        }
        porcupineManager = null
    }

    private fun buildNotification(statusText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, JarvisApplication.CHANNEL_WAKE)
            .setContentTitle("JARVIS Wake Word Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopWakeEngine()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
