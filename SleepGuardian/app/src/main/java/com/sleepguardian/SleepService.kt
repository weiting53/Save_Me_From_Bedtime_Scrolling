package com.sleepguardian

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.net.VpnService
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
        const val KEY_MODE         = "guardian_mode"
        const val EXTRA_MODE       = "guardian_mode"

        const val MODE_BUNDLE         = 0
        const val MODE_DIM_GRAY_ONLY  = 1
        const val MODE_NETWORK_ONLY   = 2
        const val MODE_REFRESH_ONLY   = 3

        const val DIM_START_MIN    = 3.0
        const val DIM_STEP_PCT     = 3         // 每次降低 3%
        const val DIM_INTERVAL_MIN = 3.0
        const val DIM_MAX_PCT      = 75        // 最暗降到原始亮度的 25%

        const val GRAY_START_MIN   = 15.0
        const val GRAY_INTERVAL_MIN = 3.0
        // 灰階進程：每 3 分鐘遞進，0→20→40→60→80→100%
        val GRAY_LEVELS = intArrayOf(20, 40, 60, 80, 100)

        const val NET_START_MIN    = 10.0
        const val NET_INTERVAL_MIN = 3.0
        val NET_SPEED_LEVELS_KBPS = intArrayOf(2500, 1800, 1300, 900, 700, 500)

        const val REFRESH_START_MIN = 10.0
        const val REFRESH_INTERVAL_MIN = 4.0
        // 更細緻曲線：系統會自動映射到裝置實際支援的最接近檔位（最低 60Hz）
        val REFRESH_LEVELS_HZ = floatArrayOf(110f, 100f, 90f, 80f, 72f, 60f)

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
    private var selectedMode = MODE_BUNDLE
    private var currentNetCapKbps: Int? = null
    private var currentRefreshRateHz: Float? = null
    private var originalPeakRefreshRate: Float? = null
    private var originalMinRefreshRate: Float? = null

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
        selectedMode = intent?.getIntExtra(EXTRA_MODE, MODE_BUNDLE)
            ?: prefs.getInt(KEY_MODE, MODE_BUNDLE)

        // 持久化，讓服務被 START_STICKY 重啟後也能讀到
        prefs.edit()
            .putLong(KEY_SLEEP_TIME, sleepTimeMillis)
            .putLong(KEY_WAKE_TIME, wakeTimeMillis)
            .putInt(KEY_MODE, selectedMode)
            .apply()

        // 記錄原始亮度（僅記一次）
        if (originalBrightness == -1 && Settings.System.canWrite(this)) {
            originalBrightness = Settings.System.getInt(
                contentResolver, Settings.System.SCREEN_BRIGHTNESS, 200
            )
        }
        captureOriginalRefreshSettingsIfNeeded()

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
        resetNetworkThrottle()
        restoreRefreshRate()

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
                applyRefreshRateByTimeline(diffMin)
                phase      = "就寢時刻"
                noticeText = "睡覺時間到了，放鬆一下 ✨"
            }

            // T+3 以後：開始降低系統亮度
            diffMin < GRAY_START_MIN -> {
                val dimPct = calcDimPct(diffMin)
                applyBrightnessAndGrayscale(diffMin, dimPct)
                applyNetworkThrottleByTimeline(diffMin)
                applyRefreshRateByTimeline(diffMin)
                phase      = "漸暗模式"
                noticeText  = buildStatusText(dimPct, null, currentNetCapKbps, currentRefreshRateHz)
            }

            // T+15 以後：亮度繼續降，同時開始灰階
            else -> {
                val dimPct  = calcDimPct(diffMin)
                val grayPct = calcGrayPct(diffMin)
                applyBrightnessAndGrayscale(diffMin, dimPct)
                applyNetworkThrottleByTimeline(diffMin)
                applyRefreshRateByTimeline(diffMin)

                phase      = "灰階模式"
                noticeText  = buildStatusText(dimPct, grayPct, currentNetCapKbps, currentRefreshRateHz)
            }
        }

        updateNotification(phase, noticeText)
    }

    private fun calcDimPct(diffMin: Double): Int {
        val steps = ((diffMin - DIM_START_MIN) / DIM_INTERVAL_MIN).toInt() + 1
        return (steps * DIM_STEP_PCT).coerceAtMost(DIM_MAX_PCT)
    }

    private fun calcGrayPct(diffMin: Double): Int? {
        if (diffMin < GRAY_START_MIN) return null
        val graySteps = ((diffMin - GRAY_START_MIN) / GRAY_INTERVAL_MIN).toInt()
        val grayIdx   = graySteps.coerceAtMost(GRAY_LEVELS.size - 1)
        return GRAY_LEVELS[grayIdx]
    }

    private fun buildStatusText(dimPct: Int?, grayPct: Int?, speedKbps: Int?, refreshHz: Float?): String {
        val parts = mutableListOf<String>()
        dimPct?.let { parts += "亮度 -$it%" }
        grayPct?.let { parts += "灰階 $it%" }
        speedKbps?.let { parts += "網速上限約 ${it}kbps" }
        refreshHz?.let { parts += "更新率 ${it.toInt()}Hz" }
        return if (parts.isEmpty()) "睡前緩衝中" else parts.joinToString("  ·  ")
    }

    private fun applyBrightnessAndGrayscale(diffMin: Double, dimPct: Int) {
        if (selectedMode == MODE_NETWORK_ONLY || selectedMode == MODE_REFRESH_ONLY) {
            applyGrayscale(0)
            return
        }
        applyBrightnessDim(dimPct)
        val grayPct = calcGrayPct(diffMin) ?: 0
        applyGrayscale(grayPct)
    }

    /**
     * Android 原生無公開 API 可直接全系統限速；這裡先以可替換介面封裝，
     * 後續可改接 VPNService 做真正封包級節流。
     */
    private fun applyNetworkThrottleByTimeline(diffMin: Double) {
        if (selectedMode == MODE_DIM_GRAY_ONLY) {
            resetNetworkThrottle()
            return
        }
        if (diffMin < NET_START_MIN) {
            resetNetworkThrottle()
            return
        }
        val netSteps = ((diffMin - NET_START_MIN) / NET_INTERVAL_MIN).toInt()
        val netIdx = netSteps.coerceAtMost(NET_SPEED_LEVELS_KBPS.size - 1)
        applyNetworkThrottle(NET_SPEED_LEVELS_KBPS[netIdx])
    }

    private fun applyNetworkThrottle(targetKbps: Int) {
        val capped = targetKbps.coerceAtLeast(500)
        currentNetCapKbps = capped
        if (VpnService.prepare(this) != null) return
        val intent = Intent(this, PulseThrottleVpnService::class.java).apply {
            action = PulseThrottleVpnService.ACTION_START_OR_UPDATE
            putExtra(PulseThrottleVpnService.EXTRA_TARGET_KBPS, capped)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun resetNetworkThrottle() {
        currentNetCapKbps = null
        val intent = Intent(this, PulseThrottleVpnService::class.java).apply {
            action = PulseThrottleVpnService.ACTION_STOP
        }
        startService(intent)
    }

    private fun applyRefreshRateByTimeline(diffMin: Double) {
        if (selectedMode != MODE_BUNDLE && selectedMode != MODE_REFRESH_ONLY) {
            restoreRefreshRate()
            return
        }
        if (diffMin < REFRESH_START_MIN) {
            restoreRefreshRate()
            return
        }
        val steps = ((diffMin - REFRESH_START_MIN) / REFRESH_INTERVAL_MIN).toInt()
        val idx = steps.coerceAtMost(REFRESH_LEVELS_HZ.size - 1)
        applyRefreshRate(REFRESH_LEVELS_HZ[idx])
    }

    private fun applyRefreshRate(targetHz: Float) {
        if (!Settings.System.canWrite(this)) {
            currentRefreshRateHz = null
            return
        }
        val supportedHz = resolveSupportedRefreshRate(targetHz)
        val ok = setSystemRefreshRate(supportedHz)
        currentRefreshRateHz = if (ok) supportedHz else null
    }

    private fun captureOriginalRefreshSettingsIfNeeded() {
        if (!Settings.System.canWrite(this)) return
        if (originalPeakRefreshRate != null || originalMinRefreshRate != null) return
        originalPeakRefreshRate = try {
            Settings.System.getFloat(contentResolver, "peak_refresh_rate")
        } catch (_: Settings.SettingNotFoundException) {
            null
        }
        originalMinRefreshRate = try {
            Settings.System.getFloat(contentResolver, "min_refresh_rate")
        } catch (_: Settings.SettingNotFoundException) {
            null
        }
    }

    private fun setSystemRefreshRate(targetHz: Float): Boolean {
        return try {
            Settings.System.putFloat(contentResolver, "peak_refresh_rate", targetHz)
            Settings.System.putFloat(contentResolver, "min_refresh_rate", 60f)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun restoreRefreshRate() {
        currentRefreshRateHz = null
        if (!Settings.System.canWrite(this)) return
        try {
            originalPeakRefreshRate?.let {
                Settings.System.putFloat(contentResolver, "peak_refresh_rate", it)
            }
            originalMinRefreshRate?.let {
                Settings.System.putFloat(contentResolver, "min_refresh_rate", it)
            }
        } catch (_: Exception) {
        }
    }

    private fun resolveSupportedRefreshRate(targetHz: Float): Float {
        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val display = dm.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: return targetHz
        val rates = display.supportedModes
            .map { it.refreshRate }
            .filter { it >= 60f }
            .distinct()
            .sortedDescending()
        if (rates.isEmpty()) return targetHz.coerceAtLeast(60f)
        return rates.firstOrNull { it <= targetHz } ?: rates.last()
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
