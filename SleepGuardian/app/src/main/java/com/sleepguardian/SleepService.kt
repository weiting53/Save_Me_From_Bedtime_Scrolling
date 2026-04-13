package com.sleepguardian

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * 前景服務：在使用者設定的睡覺時間後，漸進式降低亮度並套用灰階效果。
 *
 * 時間軸：
 *  T+0  min  → 睡覺時間到
 *  T+3  min  → 開始每隔 3 分鐘降低 3% 亮度（透過 WRITE_SETTINGS 直接改系統亮度）
 *  T+15 min  → 開始套用灰階 Overlay（逐步加深）
 *
 * 灰階策略：
 *  1. 優先嘗試透過 Settings.Secure 切換系統無障礙灰階（需事先以 ADB 授予 WRITE_SECURE_SETTINGS）
 *  2. 無該權限時退而求其次，疊加半透明灰色 Overlay 視覺模擬灰階
 */
class SleepService : Service() {

    companion object {
        const val PREFS            = "sleep_guardian_prefs"
        const val KEY_HOUR         = "hour"
        const val KEY_MINUTE       = "minute"
        const val KEY_SLEEP_TIME   = "sleep_time_millis"
        const val EXTRA_SLEEP_TIME = "sleep_time_millis"

        const val KEY_WAKE_HOUR    = "wake_hour"
        const val KEY_WAKE_MINUTE  = "wake_minute"
        const val KEY_WAKE_TIME    = "wake_time_millis"
        const val EXTRA_WAKE_TIME  = "wake_time_millis"

        const val DIM_START_MIN    = 3.0
        const val DIM_STEP_PCT     = 3         // 每次降低 3%
        const val DIM_INTERVAL_MIN = 3.0
        const val DIM_MAX_PCT      = 75        // 最暗降到原始亮度的 25%

        const val GRAY_START_MIN   = 15.0
        const val GRAY_INTERVAL_MIN = 3.0
        // 灰階進程：每 3 分鐘遞進，0→20→40→60→80→100%
        val GRAY_LEVELS = intArrayOf(20, 40, 60, 80, 100)

        const val CHANNEL_ID = "sleep_guardian_channel"

        @Volatile var isRunning = false
    }

    private lateinit var windowManager: WindowManager
    private var dimView:  View? = null   // 黑色遮罩（降低亮度用）
    private var grayView: View? = null   // 灰色遮罩（視覺灰階 fallback）

    private val handler = Handler(Looper.getMainLooper())
    private var sleepTimeMillis = 0L
    private var wakeTimeMillis  = 0L     // 早上自動關閉的時間點
    private var originalBrightness = -1  // 記錄原始亮度，停止時恢復

    // ────────────────────────────────────────────
    // 每 30 秒執行一次的 tick
    // ────────────────────────────────────────────
    private val tickRunnable = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 30_000L)
        }
    }

    // ────────────────────────────────────────────
    // 生命周期
    // ────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        sleepTimeMillis = intent?.getLongExtra(EXTRA_SLEEP_TIME, 0L)
            ?: prefs.getLong(KEY_SLEEP_TIME, 0L)

        wakeTimeMillis = intent?.getLongExtra(EXTRA_WAKE_TIME, 0L)
            ?: prefs.getLong(KEY_WAKE_TIME, 0L)

        // 持久化，讓服務被 START_STICKY 重啟後也能讀到
        prefs.edit()
            .putLong(KEY_SLEEP_TIME, sleepTimeMillis)
            .putLong(KEY_WAKE_TIME, wakeTimeMillis)
            .apply()

        // 記錄原始亮度（僅記一次）
        if (originalBrightness == -1 && Settings.System.canWrite(this)) {
            originalBrightness = Settings.System.getInt(
                contentResolver, Settings.System.SCREEN_BRIGHTNESS, 200
            )
        }

        startForeground(1, buildNotification("睡眠引導中", "背景監控中，等待睡覺時間…"))

        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)   // 立即執行一次

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(tickRunnable)

        // 移除遮罩
        removeOverlays()

        // 嘗試恢復系統亮度
        restoreBrightness()

        // 關閉系統灰階（若有成功啟用）
        trySetSystemGrayscale(false)
    }

    override fun onBind(intent: Intent?) = null

    // ────────────────────────────────────────────
    // 核心邏輯
    // ────────────────────────────────────────────
    private fun tick() {
        // 到了起床時間 → 自動關閉服務，恢復亮度與灰階
        if (wakeTimeMillis > 0 && System.currentTimeMillis() >= wakeTimeMillis) {
            updateNotification("自動關閉", "已到起床時間，引導結束，早安 ☀️")
            handler.postDelayed({ stopSelf() }, 2_000L)
            return
        }

        val diffMin = (System.currentTimeMillis() - sleepTimeMillis) / 60_000.0

        val phase: String
        val noticeText: String

        when {
            // 還沒到睡覺時間
            diffMin < 0 -> {
                val rem = (-diffMin).toInt()
                phase      = "等待中"
                noticeText = "距離睡覺還有 $rem 分鐘"
            }

            // 睡覺時間已到，但還沒開始降亮（T+0 ~ T+3）
            diffMin < DIM_START_MIN -> {
                phase      = "就寢時刻"
                noticeText = "睡覺時間到了，放鬆一下 ✨"
            }

            // T+3 以後：開始降低系統亮度
            diffMin < GRAY_START_MIN -> {
                // +1 讓第一步在 T+3 立刻生效（而非等到 T+6）
                val steps   = ((diffMin - DIM_START_MIN) / DIM_INTERVAL_MIN).toInt() + 1
                val dimPct  = (steps * DIM_STEP_PCT).coerceAtMost(DIM_MAX_PCT)
                applyBrightnessDim(dimPct)
                phase      = "漸暗模式"
                noticeText  = "亮度已降低 $dimPct%"
            }

            // T+15 以後：亮度繼續降，同時開始灰階
            else -> {
                val dimSteps  = ((diffMin - DIM_START_MIN)  / DIM_INTERVAL_MIN).toInt() + 1
                val graySteps = ((diffMin - GRAY_START_MIN) / GRAY_INTERVAL_MIN).toInt()
                val dimPct    = (dimSteps * DIM_STEP_PCT).coerceAtMost(DIM_MAX_PCT)
                val grayIdx   = graySteps.coerceAtMost(GRAY_LEVELS.size - 1)
                val grayPct   = GRAY_LEVELS[grayIdx]

                applyBrightnessDim(dimPct)
                applyGrayscale(grayPct)

                phase      = "灰階模式"
                noticeText  = "亮度 -$dimPct%  ·  灰階 $grayPct%"
            }
        }

        updateNotification(phase, noticeText)
    }

    // ────────────────────────────────────────────
    // 系統亮度（Settings.System.SCREEN_BRIGHTNESS，0-255）
    // ────────────────────────────────────────────
    private fun applyBrightnessDim(dimPct: Int) {
        if (!Settings.System.canWrite(this)) return
        val base   = if (originalBrightness > 0) originalBrightness else 200
        val target = (base * (1f - dimPct / 100f)).toInt().coerceIn(15, 255)
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, target)
    }

    private fun restoreBrightness() {
        if (!Settings.System.canWrite(this)) return
        if (originalBrightness > 0) {
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, originalBrightness)
        }
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
    }

    // ────────────────────────────────────────────
    // 灰階：先試系統無障礙灰階，不行再用 Overlay
    // ────────────────────────────────────────────
    private fun applyGrayscale(pct: Int) {
        if (trySetSystemGrayscale(pct > 0)) {
            // 成功使用系統灰階，移除灰色 Overlay（避免重疊）
            grayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
            grayView = null
        } else {
            // Fallback：疊加半透明灰色 Overlay 模擬灰階視覺效果
            val alpha = pct / 100f * 0.75f   // 最高 75% 不透明度
            ensureGrayOverlay()
            grayView?.alpha = alpha
        }
    }

    /**
     * 嘗試用 Settings.Secure 控制系統無障礙灰階模式。
     * 需要事先執行：adb shell pm grant com.sleepguardian android.permission.WRITE_SECURE_SETTINGS
     * @return true = 成功切換系統灰階；false = 沒有權限，使用 Overlay fallback
     */
    private fun trySetSystemGrayscale(enable: Boolean): Boolean {
        return try {
            Settings.Secure.putInt(contentResolver,
                "accessibility_display_daltonizer_enabled", if (enable) 1 else 0)
            if (enable) {
                Settings.Secure.putInt(contentResolver,
                    "accessibility_display_daltonizer", -1)  // -1 = 灰階
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    // ────────────────────────────────────────────
    // WindowManager Overlay 管理
    // ────────────────────────────────────────────
    private fun overlayParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    private fun ensureGrayOverlay() {
        if (grayView == null && Settings.canDrawOverlays(this)) {
            grayView = View(this).apply {
                setBackgroundColor(Color.parseColor("#B0B0B0"))
                alpha = 0f
            }
            windowManager.addView(grayView, overlayParams())
        }
    }

    private fun removeOverlays() {
        dimView?.let  { try { windowManager.removeView(it) } catch (_: Exception) {} }
        grayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        dimView  = null
        grayView = null
    }

    // ────────────────────────────────────────────
    // 前景服務通知
    // ────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "睡眠引導",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "睡眠引導背景服務通知"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_sleep)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(phase: String, text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, buildNotification("睡眠引導・$phase", text))
    }
}
