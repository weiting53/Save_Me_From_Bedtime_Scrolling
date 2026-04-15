package com.sleepguardian

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private enum class PermissionFlow { NONE, START_GUARDIAN, DEMO }

    private var selectedHour   = 23
    private var selectedMinute = 0
    private var wakeHour       = 5
    private var wakeMinute     = 0
    private var selectedMode   = SleepService.MODE_BUNDLE
    private var permissionFlow = PermissionFlow.NONE

    // ── Activity Result Launchers ──────────────────────────────
    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resumePermissionFlow() }

    private val writeSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resumePermissionFlow() }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { resumePermissionFlow() }

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            resumePermissionFlow()
        } else {
            val msg = when (permissionFlow) {
                PermissionFlow.START_GUARDIAN -> "❌ 需要 VPN 權限才能使用降網速模式"
                PermissionFlow.DEMO          -> "❌ Demo 需 VPN 才能展示降網速段落"
                else -> ""
            }
            if (msg.isNotEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            permissionFlow = PermissionFlow.NONE
        }
    }

    // ── Views ──────────────────────────────────────────────────
    private lateinit var clockView:   SegmentClockView
    private lateinit var tvWakeTime:  TextView
    private lateinit var tvPhase:     TextView
    private lateinit var btnStart:    Button
    private lateinit var btnStop:     Button
    private lateinit var modeScroll:  HorizontalScrollView
    private lateinit var card0:       GlowCardView
    private lateinit var card1:       GlowCardView
    private lateinit var card2:       GlowCardView
    private lateinit var card3:       GlowCardView
    private lateinit var swDaily:     Switch
    private lateinit var tvDemo:      TextView
    private lateinit var tvAdbHint:   TextView
    private lateinit var ivSleeping:  ImageView

    // 卡片滑動後 snap 到最近的卡片（延遲觸發）
    private val snapRunnable = Runnable { snapToNearestCard() }

    // ── Lifecycle ──────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockView  = findViewById(R.id.clock_view)
        tvWakeTime = findViewById(R.id.tv_wake_time)
        tvPhase    = findViewById(R.id.tv_phase)
        btnStart   = findViewById(R.id.btn_start)
        btnStop    = findViewById(R.id.btn_stop)
        modeScroll = findViewById(R.id.mode_scroll)
        card0      = findViewById(R.id.card_0)
        card1      = findViewById(R.id.card_1)
        card2      = findViewById(R.id.card_2)
        card3      = findViewById(R.id.card_3)
        swDaily    = findViewById(R.id.sw_daily_schedule)
        tvDemo     = findViewById(R.id.tv_demo)
        tvAdbHint  = findViewById(R.id.tv_adb_hint)
        ivSleeping = findViewById(R.id.iv_sleeping)

        // 載入 GIF 動畫（Glide 自動循環播放）
        Glide.with(this)
            .asGif()
            .load(R.drawable.bunny_sleep)
            .into(ivSleeping)

        // 讀取上次設定
        val prefs = getSharedPreferences(SleepService.PREFS, MODE_PRIVATE)
        selectedHour   = prefs.getInt(SleepService.KEY_HOUR,       23)
        selectedMinute = prefs.getInt(SleepService.KEY_MINUTE,      0)
        wakeHour       = prefs.getInt(SleepService.KEY_WAKE_HOUR,   5)
        wakeMinute     = prefs.getInt(SleepService.KEY_WAKE_MINUTE, 0)
        selectedMode   = prefs.getInt(SleepService.KEY_MODE, SleepService.MODE_BUNDLE)

        updateClockDisplay()
        updateWakeTimeDisplay()
        setupModeCards()

        swDaily.isChecked = SleepScheduleHelper.isDailyEnabled(this)
        swDaily.setOnCheckedChangeListener { _, isChecked ->
            SleepScheduleHelper.setDailyEnabled(this, isChecked)
            refreshUI()
        }

        // 點擊時鐘修改睡覺時間
        clockView.setOnClickListener { showSleepTimePicker() }
        // 起床時間（time_wake_area 是整個 LinearLayout）
        findViewById<android.view.View>(R.id.time_wake_area).setOnClickListener { showWakeTimePicker() }

        btnStart.setOnClickListener  { checkPermissionsAndStart() }
        btnStop.setOnClickListener   { stopGuardian() }
        tvDemo.setOnClickListener    { tryStartDemo() }
        tvAdbHint.setOnClickListener { showAdbDialog() }

        if (SleepScheduleHelper.isDailyEnabled(this)) {
            SleepScheduleHelper.scheduleNextOccurrence(this)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    // ── 時間選擇器 ─────────────────────────────────────────────
    private fun showSleepTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            selectedHour   = hour
            selectedMinute = minute
            updateClockDisplay()
            getSharedPreferences(SleepService.PREFS, MODE_PRIVATE).edit()
                .putInt(SleepService.KEY_HOUR,   hour)
                .putInt(SleepService.KEY_MINUTE, minute)
                .apply()
            if (SleepScheduleHelper.isDailyEnabled(this)) {
                SleepScheduleHelper.scheduleNextOccurrence(this)
            }
        }, selectedHour, selectedMinute, true).show()
    }

    private fun showWakeTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            wakeHour   = hour
            wakeMinute = minute
            updateWakeTimeDisplay()
            getSharedPreferences(SleepService.PREFS, MODE_PRIVATE).edit()
                .putInt(SleepService.KEY_WAKE_HOUR,   hour)
                .putInt(SleepService.KEY_WAKE_MINUTE, minute)
                .apply()
        }, wakeHour, wakeMinute, true).show()
    }

    private fun updateClockDisplay() {
        clockView.hour   = selectedHour
        clockView.minute = selectedMinute
    }

    private fun updateWakeTimeDisplay() {
        tvWakeTime.text = String.format("%02d:%02d", wakeHour, wakeMinute)
    }

    // ── 模式卡片（橫滑 + 點選 + snap） ────────────────────────
    private fun setupModeCards() {
        // 初始高亮
        updateCardGlow(selectedMode)

        listOf(card0, card1, card2, card3).forEachIndexed { idx, card ->
            card.setOnClickListener {
                updateCardGlow(idx)
                scrollToCard(idx)
                selectedMode = idx
                saveMode()
            }
        }

        // 滑動後 snap
        modeScroll.setOnScrollChangeListener { _, _, _, _, _ ->
            modeScroll.removeCallbacks(snapRunnable)
            modeScroll.postDelayed(snapRunnable, 140)
        }
    }

    private fun updateCardGlow(active: Int) {
        listOf(card0, card1, card2, card3).forEachIndexed { i, card ->
            card.isGlowing = (i == active)
        }
    }

    private fun scrollToCard(idx: Int) {
        val cardWidthPx = (200 * resources.displayMetrics.density).toInt()
        val gapPx       = (16  * resources.displayMetrics.density).toInt()
        modeScroll.smoothScrollTo(idx * (cardWidthPx + gapPx), 0)
    }

    private fun snapToNearestCard() {
        val cardWidthPx = (200 * resources.displayMetrics.density).toInt()
        val gapPx       = (16  * resources.displayMetrics.density).toInt()
        val step        = (cardWidthPx + gapPx).toFloat()
        val scrollX     = modeScroll.scrollX
        val nearest     = Math.round(scrollX / step).coerceIn(0, 3)
        modeScroll.smoothScrollTo(nearest * (cardWidthPx + gapPx), 0)
        if (selectedMode != nearest) {
            selectedMode = nearest
            saveMode()
            updateCardGlow(nearest)
        }
    }

    private fun saveMode() {
        getSharedPreferences(SleepService.PREFS, MODE_PRIVATE).edit()
            .putInt(SleepService.KEY_MODE, selectedMode)
            .apply()
    }

    // ── 權限流程 ────────────────────────────────────────────────
    private fun resumePermissionFlow() {
        when (permissionFlow) {
            PermissionFlow.START_GUARDIAN -> checkPermissionsAndStartInternal()
            PermissionFlow.DEMO          -> tryStartDemoInternal()
            PermissionFlow.NONE          -> Unit
        }
    }

    private fun checkPermissionsAndStart() {
        permissionFlow = PermissionFlow.START_GUARDIAN
        checkPermissionsAndStartInternal()
    }

    private fun checkPermissionsAndStartInternal() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "請允許「在其他應用程式上方顯示」的權限（灰階遮罩需要）", Toast.LENGTH_LONG).show()
            overlayLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            return
        }
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "請允許「修改系統設定」的權限（用於控制亮度）", Toast.LENGTH_LONG).show()
            writeSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        if (requiresNetworkThrottle(selectedMode)) {
            val prepareIntent = VpnService.prepare(this)
            if (prepareIntent != null) {
                Toast.makeText(this, "請允許 VPN 權限以啟用降網速", Toast.LENGTH_LONG).show()
                vpnLauncher.launch(prepareIntent)
                return
            }
        }
        permissionFlow = PermissionFlow.NONE
        attemptStart()
    }

    private fun requiresNetworkThrottle(mode: Int) =
        mode == SleepService.MODE_BUNDLE || mode == SleepService.MODE_NETWORK_ONLY

    private fun tryStartDemo() {
        if (SleepService.isRunning && !SleepService.isDemoModeActive) {
            Toast.makeText(this, "請先停止睡眠引導再試 Demo", Toast.LENGTH_SHORT).show(); return
        }
        if (SleepService.isDemoModeActive) {
            Toast.makeText(this, "Demo 進行中", Toast.LENGTH_SHORT).show(); return
        }
        permissionFlow = PermissionFlow.DEMO
        tryStartDemoInternal()
    }

    private fun tryStartDemoInternal() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Demo 需要 Overlay 權限（灰階遮罩）", Toast.LENGTH_LONG).show()
            overlayLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))); return
        }
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Demo 需要「修改系統設定」權限", Toast.LENGTH_LONG).show()
            writeSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName"))); return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS); return
            }
        }
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            Toast.makeText(this,
                "Demo 中的「降網速」與「組合包」段落需要 VPN 權限，拒絕則只有那兩段無效果",
                Toast.LENGTH_LONG).show()
            vpnLauncher.launch(prepareIntent); return
        }
        permissionFlow = PermissionFlow.NONE
        launchDemoService()
    }

    private fun launchDemoService() {
        val intent = Intent(this, SleepService::class.java).apply {
            putExtra(SleepService.EXTRA_DEMO, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        refreshUI()
    }

    private fun showAdbDialog() {
        val cmd = "adb shell pm grant com.sleepguardian android.permission.WRITE_SECURE_SETTINGS"
        AlertDialog.Builder(this)
            .setTitle("灰階功能 · 一次性 ADB 授權")
            .setMessage(
                "灰階使用 Android 系統無障礙灰階（非遮罩），效果更乾淨。\n\n" +
                "連接電腦（USB / 無線 ADB），執行一次：\n\n" +
                "$cmd\n\n" +
                "授權後重新啟動 App 即生效，之後不需再做。"
            )
            .setPositiveButton("知道了", null)
            .setNeutralButton("複製指令") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB 指令", cmd))
                Toast.makeText(this, "已複製到剪貼簿", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ── 啟動 / 停止服務 ─────────────────────────────────────────
    private fun attemptStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "❌ 需要 Overlay 權限", Toast.LENGTH_SHORT).show(); return
        }
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "❌ 需要修改系統設定權限", Toast.LENGTH_SHORT).show(); return
        }
        val (sleepMillis, wakeMillis) = SleepTimeCalculator.computeNextSleepAndWakeMillis(this)
        val intent = Intent(this, SleepService::class.java).apply {
            putExtra(SleepService.EXTRA_SLEEP_TIME, sleepMillis)
            putExtra(SleepService.EXTRA_WAKE_TIME,  wakeMillis)
            putExtra(SleepService.EXTRA_MODE, selectedMode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        if (SleepScheduleHelper.isDailyEnabled(this)) SleepScheduleHelper.scheduleNextOccurrence(this)
        refreshUI()
    }

    private fun stopGuardian() {
        stopService(Intent(this, SleepService::class.java))
        refreshUI()
    }

    // ── UI 更新 ─────────────────────────────────────────────────
    private fun refreshUI() {
        if (SleepService.isDemoModeActive) {
            swDaily.isEnabled  = false
            btnStart.isEnabled = false
            btnStop.isEnabled  = true
            btnStart.alpha     = 0.35f
            tvPhase.text       = "● DEMO  IN PROGRESS"
            return
        }

        swDaily.isEnabled = true
        val running = SleepService.isRunning
        btnStart.isEnabled = !running
        btnStop.isEnabled  = running
        btnStart.alpha     = if (running) 0.35f else 1f

        if (running) {
            val prefs       = getSharedPreferences(SleepService.PREFS, MODE_PRIVATE)
            val sleepMillis = prefs.getLong(SleepService.KEY_SLEEP_TIME, 0L)
            val diffMin     = (System.currentTimeMillis() - sleepMillis) / 60_000.0

            tvPhase.text = when {
                sleepMillis == 0L                            -> "● ACTIVE"
                diffMin < 0                                  -> "● WAITING  ${(-diffMin).toInt()} min"
                diffMin < SleepService.DIM_START_MIN         -> "● BEDTIME"
                diffMin < SleepService.GRAY_START_MIN        -> "● DIMMING"
                else                                         -> "● GRAYSCALE"
            }
        } else {
            tvPhase.text = if (SleepScheduleHelper.isDailyEnabled(this)) {
                "AUTO  NEXT: ${String.format("%02d:%02d", selectedHour, selectedMinute)}"
            } else {
                "IDLE"
            }
        }
    }
}
