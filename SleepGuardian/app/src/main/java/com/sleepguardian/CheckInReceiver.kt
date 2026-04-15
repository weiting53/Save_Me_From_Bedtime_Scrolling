package com.sleepguardian

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * 接收 morning check-in 通知上「有做到」/ 「沒有」按鈕的點擊。
 * 結果交給 StreakHelper 計算並更新 streak。
 */
class CheckInReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_YES = "com.sleepguardian.CHECKIN_YES"
        const val ACTION_NO  = "com.sleepguardian.CHECKIN_NO"
        const val NOTI_ID    = 4
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val achieved = intent?.action == ACTION_YES
        StreakHelper.recordCheckIn(context, achieved)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTI_ID)
        val msg = if (achieved) "太棒了！連續達標 ${StreakHelper.getStreak(context)} 天 🌙"
                  else "沒關係，今晚再試一次 💪"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
