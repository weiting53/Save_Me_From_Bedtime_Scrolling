package com.sleepguardian

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object SleepScheduleHelper {

    const val KEY_DAILY_SCHEDULE = "daily_schedule_enabled"
    private const val REQUEST_ALARM = 0x5347

    fun isDailyEnabled(context: Context): Boolean {
        return context.getSharedPreferences(SleepService.PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DAILY_SCHEDULE, false)  // 預設關閉，使用者主動開啟才排程
    }

    fun setDailyEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(SleepService.PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DAILY_SCHEDULE, enabled)
            .apply()
        if (enabled) {
            scheduleNextOccurrence(context)
        } else {
            cancelAlarm(context)
        }
    }

    /** 以目前偏好內的睡覺時刻，排定「下一次」觸發（今天已過則明天）。 */
    fun scheduleNextOccurrence(context: Context) {
        if (!isDailyEnabled(context)) return
        val prefs = context.getSharedPreferences(SleepService.PREFS, Context.MODE_PRIVATE)
        val hour = prefs.getInt(SleepService.KEY_HOUR, 23)
        val minute = prefs.getInt(SleepService.KEY_MINUTE, 0)

        val triggerAt = nextSleepWallClockMillis(hour, minute)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val showIntent = Intent(context, MainActivity::class.java).let { i ->
            PendingIntent.getActivity(
                context, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val op = alarmPendingIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            am.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                op
            )
        } else {
            @Suppress("DEPRECATION")
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, op)
        }
    }

    /** Alarm 剛響過後，排定隔天同一睡覺時刻。 */
    fun scheduleNextDaySameTime(context: Context) {
        if (!isDailyEnabled(context)) return
        val prefs = context.getSharedPreferences(SleepService.PREFS, Context.MODE_PRIVATE)
        val hour = prefs.getInt(SleepService.KEY_HOUR, 23)
        val minute = prefs.getInt(SleepService.KEY_MINUTE, 0)

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerAt = cal.timeInMillis
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val showIntent = Intent(context, MainActivity::class.java).let { i ->
            PendingIntent.getActivity(
                context, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val op = alarmPendingIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            am.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                op
            )
        } else {
            @Suppress("DEPRECATION")
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, op)
        }
    }

    private fun nextSleepWallClockMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_START_SLEEP
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent(context)
        am.cancel(pi)
    }
}
