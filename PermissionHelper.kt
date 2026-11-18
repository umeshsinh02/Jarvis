package com.jarvis.assistant.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun requiredVoicePermissions(): List<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun optionalActionPermissions(): List<String> = listOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS
    )

    fun allRuntimePermissions(): List<String> =
        (requiredVoicePermissions() + optionalActionPermissions()).distinct()

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasRecordAudio(context: Context): Boolean =
        hasPermission(context, Manifest.permission.RECORD_AUDIO)
}
