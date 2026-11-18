package com.jarvis.assistant.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceInfoHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getBatterySummary(): String {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempTenths / 10f

        val chargingNote = if (isCharging) "aur charger connected hai" else "discharging mode mein hai"
        val remark = when {
            pct <= 15 && !isCharging -> "Power critically low hai Sir, Arc Reactor ko charge pe lagaiye."
            pct >= 90 -> "Power reserves full capacity par hain, Boss."
            else -> "Power levels nominal hain, Sir."
        }

        return "Battery abhi $pct% par hai, $chargingNote, temperature ${tempC}°C. $remark"
    }
}
