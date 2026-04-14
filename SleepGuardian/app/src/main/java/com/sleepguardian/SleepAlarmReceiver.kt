package com.sleepguardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.Calendar

class SleepAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_START_SLEEP) return
        if (!SleepScheduleHelper.isDailyEnabled(context)) return

        try {
            val sleepMillis = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val prefs = context.getSharedPreferences(SleepService.PREFS, Context.MODE_PRIVATE)
            val wakeHour = prefs.getInt(SleepService.KEY_WAKE_HOUR, 5)
            val wakeMinute = prefs.getInt(SleepService.KEY_WAKE_MINUTE, 0)
            val mode = prefs.getInt(SleepService.KEY_MODE, SleepService.MODE_BUNDLE)

            val wakeTarget = Calendar.getInstance().apply {
                timeInMillis = sleepMillis
                set(Calendar.HOUR_OF_DAY, wakeHour)
                set(Calendar.MINUTE, wakeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= sleepMillis) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            if (!Settings.canDrawOverlays(context)) return
            if (!Settings.System.canWrite(context)) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val serviceIntent = Intent(context, SleepService::class.java).apply {
                putExtra(SleepService.EXTRA_SLEEP_TIME, sleepMillis)
                putExtra(SleepService.EXTRA_WAKE_TIME, wakeTarget.timeInMillis)
                putExtra(SleepService.EXTRA_MODE, mode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } finally {
            if (SleepScheduleHelper.isDailyEnabled(context)) {
                SleepScheduleHelper.scheduleNextDaySameTime(context)
            }
        }
    }

    companion object {
        const val ACTION_START_SLEEP = "com.sleepguardian.ACTION_DAILY_SLEEP_START"
    }
}
