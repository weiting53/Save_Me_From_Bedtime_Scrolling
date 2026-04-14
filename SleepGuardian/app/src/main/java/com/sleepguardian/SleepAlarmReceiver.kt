package com.sleepguardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class SleepAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_START_SLEEP = "com.sleepguardian.ACTION_DAILY_SLEEP_START"
        private const val PERMISSION_ALERT_CHANNEL_ID = "sleep_guardian_permission_alert"
        private const val PERMISSION_ALERT_NOTI_ID    = 3
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_START_SLEEP) return
        if (!SleepScheduleHelper.isDailyEnabled(context)) return

        try {
            val missingPermissions = collectMissingPermissions(context)

            if (missingPermissions.isNotEmpty()) {
                notifyPermissionMissing(context, missingPermissions)
                return
            }

            val sleepMillis = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val prefs = context.getSharedPreferences(SleepService.PREFS, Context.MODE_PRIVATE)
            val wakeHour   = prefs.getInt(SleepService.KEY_WAKE_HOUR,   5)
            val wakeMinute = prefs.getInt(SleepService.KEY_WAKE_MINUTE, 0)
            val mode       = prefs.getInt(SleepService.KEY_MODE, SleepService.MODE_BUNDLE)

            val wakeTarget = Calendar.getInstance().apply {
                timeInMillis = sleepMillis
                set(Calendar.HOUR_OF_DAY, wakeHour)
                set(Calendar.MINUTE, wakeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= sleepMillis) add(Calendar.DAY_OF_MONTH, 1)
            }

            val serviceIntent = Intent(context, SleepService::class.java).apply {
                putExtra(SleepService.EXTRA_SLEEP_TIME, sleepMillis)
                putExtra(SleepService.EXTRA_WAKE_TIME,  wakeTarget.timeInMillis)
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

    private fun collectMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        if (!Settings.canDrawOverlays(context)) {
            missing += "在其他應用程式上方顯示"
        }
        if (!Settings.System.canWrite(context)) {
            missing += "修改系統設定（亮度）"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing += "通知"
            }
        }
        return missing
    }

    private fun notifyPermissionMissing(context: Context, missing: List<String>) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensurePermissionAlertChannel(nm)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val missingText = missing.joinToString("、")
        val notification = NotificationCompat.Builder(context, PERMISSION_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🌙 睡眠引導無法自動啟動")
            .setContentText("缺少權限：$missingText，點此前往補授權")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("今晚的睡眠引導因缺少以下權限而無法啟動：$missingText。\n\n點此開啟 App 並前往系統設定補授權，即可讓明晚自動啟動。")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(PERMISSION_ALERT_NOTI_ID, notification)
    }

    private fun ensurePermissionAlertChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(PERMISSION_ALERT_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    PERMISSION_ALERT_CHANNEL_ID,
                    "睡眠引導權限提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "每日排程到點但缺少必要權限時發出提醒"
                    setShowBadge(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }
}
