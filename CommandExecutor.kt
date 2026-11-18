package com.jarvis.assistant.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Searches installed launcher apps by display label and opens the closest match.
     */
    @SuppressLint("QueryPermissionsNeeded")
    fun openAppByName(rawAppName: String): String {
        val target = rawAppName.lowercase(Locale.getDefault()).trim()
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val matched = apps.firstOrNull { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString().lowercase(Locale.getDefault())
            label == target || label.contains(target)
        } ?: return "Sir, '$rawAppName' phone mein installed nahi mila."

        val packageName = matched.activityInfo.packageName
        val launchIntent = pm.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return "Sir, '$rawAppName' ko launch karne ki permission system ne block ki hai."

        return runCatching {
            context.startActivity(launchIntent)
            val appLabel = matched.loadLabel(pm).toString()
            "Opening $appLabel, Sir."
        }.getOrElse {
            "Sorry Boss, $rawAppName open karne mein error aaya."
        }
    }

    /**
     * Resolves contact number from ContactsContract and initiates a call if CALL_PHONE is granted,
     * otherwise opens the system dialer pre-filled with the number.
     */
    fun initiateCall(contactName: String): String {
        if (!PermissionHelper.hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            return "Sir, contacts read karne ki permission nahi hai. Please permissions allow kijiye."
        }

        val phoneNumber = findPhoneNumber(contactName)
            ?: return "Sir, '$contactName' aapke contacts mein nahi mila."

        val canDirectCall = PermissionHelper.hasPermission(context, Manifest.permission.CALL_PHONE)
        val action = if (canDirectCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val intent = Intent(action, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            context.startActivity(intent)
            if (canDirectCall) {
                "$contactName ko call connect kar raha hoon, Sir."
            } else {
                "$contactName ka number dialer mein open kar diya hai, Sir."
            }
        }.getOrElse {
            "Call initiate karne mein problem aayi: ${it.localizedMessage}"
        }
    }

    /**
     * Sends SMS directly via SmsManager when messageBody is provided and SEND_SMS granted,
     * or opens the default SMS app composer.
     */
    fun sendSmsToContact(contactName: String, messageBody: String?): String {
        if (!PermissionHelper.hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            return "Sir, message bhejne ke liye Contacts permission chahiye."
        }

        val phoneNumber = findPhoneNumber(contactName)
            ?: return "Sir, '$contactName' ka number contacts mein nahi mila."

        if (!messageBody.isNullOrBlank() &&
            PermissionHelper.hasPermission(context, Manifest.permission.SEND_SMS)
        ) {
            return runCatching {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, messageBody, null, null)
                "$contactName ko SMS bhej diya gaya hai, Boss: "$messageBody""
            }.getOrElse {
                "SMS send nahi ho paaya: ${it.localizedMessage}"
            }
        }

        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
            putExtra("sms_body", messageBody.orEmpty())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(smsIntent)
            "$contactName ke liye SMS composer open kar diya hai, Sir."
        }.getOrElse {
            "Messaging app open karne mein failure aaya."
        }
    }

    /**
     * Sets an alarm via AlarmClock intent and schedules a backup AlarmManager trigger.
     */
    fun setAlarm(hour: Int, minute: Int, label: String): String {
        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val clockStarted = runCatching {
            context.startActivity(alarmIntent)
            true
        }.getOrDefault(false)

        if (!clockStarted) {
            // Fallback using AlarmManager exact/inexact scheduling
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val pending = PendingIntent.getActivity(
                context,
                hour * 100 + minute,
                Intent(context, Class.forName("com.jarvis.assistant.MainActivity")),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
        }

        val formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        return "Done Sir! $formatted ke liye alarm set kar diya hai."
    }

    private fun findPhoneNumber(contactName: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIdx >= 0) {
                    return cursor.getString(numberIdx)?.replace("\\s+".toRegex(), "")
                }
            }
        }
        return null
    }
}
