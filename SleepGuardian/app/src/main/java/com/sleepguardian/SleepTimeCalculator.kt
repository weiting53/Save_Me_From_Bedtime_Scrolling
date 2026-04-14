package com.sleepguardian

import android.content.Context
import java.util.Calendar

/**
 * 與 MainActivity 一致的「下一個睡覺／起床」時間計算，供排程與 Alarm 觸發時使用。
 */
object SleepTimeCalculator {

    fun computeNextSleepAndWakeMillis(context: Context): Pair<Long, Long> {
        val prefs = context.getSharedPreferences(SleepService.PREFS, Context.MODE_PRIVATE)
        val hour = prefs.getInt(SleepService.KEY_HOUR, 23)
        val minute = prefs.getInt(SleepService.KEY_MINUTE, 0)
        val wakeHour = prefs.getInt(SleepService.KEY_WAKE_HOUR, 5)
        val wakeMinute = prefs.getInt(SleepService.KEY_WAKE_MINUTE, 0)

        val sleepTarget = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val wakeTarget = Calendar.getInstance().apply {
            timeInMillis = sleepTarget.timeInMillis
            set(Calendar.HOUR_OF_DAY, wakeHour)
            set(Calendar.MINUTE, wakeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= sleepTarget.timeInMillis) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return sleepTarget.timeInMillis to wakeTarget.timeInMillis
    }
}
