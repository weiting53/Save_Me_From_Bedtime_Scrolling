package com.sleepguardian

import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private enum class PermissionFlow {
        NONE,
        START_GUARDIAN,
        DEMO
    }

    private var selectedHour   = 23
    private var selectedMinute = 0
    private var wakeHour       = 5    // 預設起床時間 05:00
    private var wakeMinute     = 0
    private var selectedMode   = SleepService.MODE_BUNDLE
    private var permissionFlow = PermissionFlow.NONE

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
            when (permissionFlow) {
                PermissionFlow.START_GUARDIAN ->
                    tvStatus.text = "❌ 需要 VPN 權限才能使用降網速模式"
                PermissionFlow.DEMO ->
                    tvStatus.text = "❌ Demo 需 VPN 才能展示降網速段落"
                else -> Unit
            }
            permissionFlow = PermissionFlow.NONE
        }
    }

    // ── 元素
    private lateinit var tvSleepTime: TextView
    private lateinit var tvWakeTime:  TextView
    private lateinit var tvStatus:    TextView
    private lateinit var btnStart:    Button
    private lateinit var btnStop:     Button
    private lateinit var tvPhase:     TextView
    private lateinit var spMode:      Spinner
    private lateinit var swDaily:   Switch
    private lateinit var tvDemo:    TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSleepTime = findViewById(R.id.tv_sleep_time)
        tvWakeTime  = findViewById(R.id.tv_wake_time)
        tvStatus    = findViewById(R.id.tv_status)
        btnStart    = findViewById(R.id.btn_start)
        btnStop     = findViewById(R.id.btn_stop)
        tvPhase     = findViewById(R.id.tv_phase)
        spMode      = findViewById(R.id.sp_mode)
        swDaily     = findViewById(R.id.sw_daily_schedule)
        tvDemo      = findViewById(R.id.tv_demo)

        // 從 SharedPreferences 讀取上次設定
        val prefs = getSharedPreferences(SleepService.PREFS, MODE_PRIVATE)
        selectedHour   = prefs.getInt(SleepService.KEY_HOUR,         23)
        selectedMinute = prefs.getInt(SleepService.KEY_MINUTE,        0)
        wakeHour       = prefs.getInt(SleepService.KEY_WAKE_HOUR,     5)
        wakeMinute     = prefs.getInt(SleepService.KEY_WAKE_MINUTE,   0)
        selectedMode   = prefs.getInt(SleepService.KEY_MODE, SleepService.MODE_BUNDLE)

        updateSleepTimeDisplay()
        updateWakeTimeDisplay()
        setupModeSpinner()

        swDaily.setOnCheckedChangeListener { _, isChecked ->
            SleepScheduleHelper.setDailyEnabled(this, isChecked)
            refreshUI()
        }
        swDaily.isChecked = SleepScheduleHelper.isDailyEnabled(this)

        // 點擊整個時間區域（數字 + 編輯圖示）皆可觸發選擇器
        findViewById<android.view.View>(R.id.time_sleep_area).setOnClickListener { showSleepTimePicker() }
        findViewById<android.view.View>(R.id.time_wake_area).setOnClickListener  { showWakeTimePicker() }

        btnStart.setOnClickListener { checkPermissionsAndStart() }
        btnStop.setOnClickListener  { stopGuardian() }
        tvDemo.setOnClickListener   { tryStartDemo() }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    // ── 睡覺時間選擇器
    private fun showSleepTimePicker() {
        TimePickerDialog(this, { _, hour, minute ->
            selectedHour   = hour
            selectedMinute = minute
            updateSleepTimeDisplay()
            getSharedPreferences(SleepService.PREFS, MODE_PRIVATE).edit()
                .putInt(SleepService.KEY_HOUR,   hour)
                .putInt(SleepService.KEY_MINUTE, minute)
                .apply()
            if (SleepScheduleHelper.isDailyEnabled(this)) {
                SleepScheduleHelper.scheduleNextOccurrence(this)
            }
        }, selectedHour, selectedMinute, true).show()
    }

    // ── 起床時間選擇器
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

    private fun updateSleepTimeDisplay() {
        tvSleepTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
    }

    private fun updateWakeTimeDisplay() {
        tvWakeTime.text = String.format("%02d:%02d", wakeHour, wakeMinute)
    }

    private fun setupModeSpinner() {
        val options = listOf(
            "組合包（亮度＋灰階＋降網速＋降更新率）",
            "各自功能：亮度＋灰階",
            "各自功能：僅降網速",
            "各自功能：僅降更新率"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spMode.adapter = adapter
        spMode.setSelection(selectedMode.coerceIn(0, options.lastIndex), false)
        spMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMode = position
                getSharedPreferences(SleepService.PREFS, MODE_PRIVATE).edit()
                    .putInt(SleepService.KEY_MODE, selectedMode)
                    .apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun resumePermissionFlow() {
        when (permissionFlow) {
            PermissionFlow.START_GUARDIAN -> checkPermissionsAndStartInternal()
            PermissionFlow.DEMO -> tryStartDemoInternal()
            PermissionFlow.NONE -> Unit
        }
    }

    // ── 依序檢查所有必要權限
    private fun checkPermissionsAndStart() {
        permissionFlow = PermissionFlow.START_GUARDIAN
        checkPermissionsAndStartInternal()
    }

    private fun checkPermissionsAndStartInternal() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "請允許「顯示在其他應用程式上面」的權限", Toast.LENGTH_LONG).show()
            overlayLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
            return
        }
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "請允許「修改系統設定」的權限（用於控制亮度）", Toast.LENGTH_LONG).show()
            writeSettingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName"))
            )
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

    private fun requiresNetworkThrottle(mode: Int): Boolean {
        return mode == SleepService.MODE_BUNDLE || mode == SleepService.MODE_NETWORK_ONLY
    }

    private fun tryStartDemo() {
        if (SleepService.isRunning && !SleepService.isDemoModeActive) {
            Toast.makeText(this, "請先停止睡眠引導再試 Demo", Toast.LENGTH_SHORT).show()
            return
        }
        if (SleepService.isDemoModeActive) {
            Toast.makeText(this, "Demo 進行中", Toast.LENGTH_SHORT).show()
            return
        }
        permissionFlow = PermissionFlow.DEMO
        tryStartDemoInternal()
    }

    private fun tryStartDemoInternal() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Demo 需要 Overlay 權限", Toast.LENGTH_LONG).show()
            overlayLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
            return
        }
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Demo 需要「修改系統設定」權限", Toast.LENGTH_LONG).show()
            writeSettingsLauncher.launch(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName"))
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            Toast.makeText(this, "Demo 會輪播降網速，請允許 VPN", Toast.LENGTH_LONG).show()
            vpnLauncher.launch(prepareIntent)
            return
        }
        permissionFlow = PermissionFlow.NONE
        launchDemoService()
    }

    private fun launchDemoService() {
        val intent = Intent(this, SleepService::class.java).apply {
            putExtra(SleepService.EXTRA_DEMO, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        refreshUI()
    }

    // ── 所有權限就緒後啟動服務
    private fun attemptStart() {
        if (!Settings.canDrawOverlays(this)) {
            tvStatus.text = "❌ 需要「Overlay」權限才能啟動"
            return
        }
        if (!Settings.System.canWrite(this)) {
            tvStatus.text = "❌ 需要「修改系統設定」權限才能控制亮度"
            return
        }

        val (sleepMillis, wakeMillis) = SleepTimeCalculator.computeNextSleepAndWakeMillis(this)

        val intent = Intent(this, SleepService::class.java).apply {
            putExtra(SleepService.EXTRA_SLEEP_TIME, sleepMillis)
            putExtra(SleepService.EXTRA_WAKE_TIME,  wakeMillis)
            putExtra(SleepService.EXTRA_MODE, selectedMode)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        if (SleepScheduleHelper.isDailyEnabled(this)) {
            SleepScheduleHelper.scheduleNextOccurrence(this)
        }

        refreshUI()
    }

    // ── 停止服務
    private fun stopGuardian() {
        stopService(Intent(this, SleepService::class.java))
        refreshUI()
    }

    // ── 根據服務狀態更新 UI
    private fun refreshUI() {
        if (SleepService.isDemoModeActive) {
            swDaily.isEnabled = false
            btnStart.isEnabled = false
            btnStop.isEnabled  = true
            btnStart.alpha     = 0.35f
            tvPhase.text       = "Demo 演練中"
            tvStatus.text      = "約 15 秒內依序模擬：組合包 → 亮度灰階 → 僅降網速 → 僅降更新率（通知可看階段）"
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
            val wakeMillis  = prefs.getLong(SleepService.KEY_WAKE_TIME,  0L)
            val diffMin     = (System.currentTimeMillis() - sleepMillis) / 60_000.0

            tvPhase.text = when {
                sleepMillis == 0L                           -> "引導執行中"
                diffMin < 0                                  -> "等待睡覺時間（還有 ${(-diffMin).toInt()} 分鐘）"
                diffMin < SleepService.DIM_START_MIN         -> "就寢時刻已到"
                diffMin < SleepService.GRAY_START_MIN        -> "漸暗模式進行中"
                else                                         -> "灰階模式進行中"
            }

            val modeText = when (prefs.getInt(SleepService.KEY_MODE, SleepService.MODE_BUNDLE)) {
                SleepService.MODE_DIM_GRAY_ONLY -> "（各自功能：亮度＋灰階）"
                SleepService.MODE_NETWORK_ONLY -> "（各自功能：僅降網速）"
                SleepService.MODE_REFRESH_ONLY -> "（各自功能：僅降更新率）"
                else -> "（組合包）"
            }

            val wakeStr = if (wakeMillis > 0 && wakeMillis != Long.MAX_VALUE) {
                val cal = Calendar.getInstance().apply { timeInMillis = wakeMillis }
                String.format("，%02d:%02d 自動關閉", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } else ""

            val dailyStr = if (SleepScheduleHelper.isDailyEnabled(this)) {
                " · 每日排程已開啟"
            } else ""

            tvStatus.text = "✅ 睡眠引導背景執行中$wakeStr $modeText$dailyStr"
        } else {
            tvPhase.text  = "尚未啟動"
            val dailyStr = if (SleepScheduleHelper.isDailyEnabled(this)) {
                "已開啟「每日自動開始」，將在預計睡覺時間自動啟動（仍須具備權限）。"
            } else {
                "點「開始引導」讓引導在背景運行"
            }
            tvStatus.text = dailyStr
        }
    }
}
