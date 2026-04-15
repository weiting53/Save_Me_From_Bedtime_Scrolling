package com.sleepguardian

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Streak 連續達標計算器。
 *
 * 每天早上起床時間到，SleepService 會觸發 morning check-in 通知。
 * 使用者按「有做到」→ streak +1（若昨天也有 check-in）或 = 1（中斷後重新開始）。
 * 使用者按「沒有」→ streak 歸零。
 * 忽略不回應 → streak 不變（MVP 簡化）。
 */
object StreakHelper {

    private const val KEY_STREAK    = "streak_count"
    private const val KEY_LAST_DATE = "streak_last_date"  // "yyyy-MM-dd"

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getStreak(context: Context): Int =
        prefs(context).getInt(KEY_STREAK, 0)

    /**
     * 記錄今天的 check-in 結果。
     * @param achieved true = 有照時間睡，false = 沒有
     */
    fun recordCheckIn(context: Context, achieved: Boolean) {
        val p = prefs(context)
        val today = todayStr()
        val lastDate = p.getString(KEY_LAST_DATE, "")
        val current = p.getInt(KEY_STREAK, 0)

        val newStreak = if (achieved) {
            if (lastDate == yesterdayStr()) current + 1 else 1
        } else {
            0
        }
        p.edit()
            .putInt(KEY_STREAK, newStreak)
            .putString(KEY_LAST_DATE, today)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(SleepService.PREFS, Context.MODE_PRIVATE)

    private fun todayStr(): String = dateFmt.format(Calendar.getInstance().time)

    private fun yesterdayStr(): String {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        return dateFmt.format(c.time)
    }
}
