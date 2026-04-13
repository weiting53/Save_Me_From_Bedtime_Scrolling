package com.sleepguardian

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private var selectedHour   = 23
    private var selectedMinute = 0
    private var wakeHour       = 5    // 預設起床時間 05:00
    private var wakeMinute     = 0

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { attemptStart() }

    private val writeSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { attemptStart() }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { attemptStart() }

    // ── 元素
    private lateinit var tvSleepTime: TextView
    private lateinit var tvWakeTime:  TextView
    private lateinit var tvStatus:    TextView
    private lateinit var btnStart:    Button
    private lateinit var btnStop:     Button
    private lateinit var tvPhase:     TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSleepTime = findViewById(R.id.tv_sleep_time)
        tvWakeTime  = findViewById(R.id.tv_wake_time)
        tvStatus    = findViewById(R.id.tv_status)
        btnStart    = findViewById(R.id.btn_start)
        btnStop     = findViewById(R.id.btn_stop)
        tvPhase     = findViewById(R.id.tv_phase)

        // 從 SharedPreferences 讀取上次設定
        val prefs = getSharedPreferences(SleepService.PREFS, MODE_PRIVATE)
        selectedHour   = prefs.getInt(SleepService.KEY_HOUR,         23)
        selectedMinute = prefs.getInt(SleepService.KEY_MINUTE,        0)
        wakeHour       = prefs.getInt(SleepService.KEY_WAKE_HOUR,     5)
        wakeMinute     = prefs.getInt(SleepService.KEY_WAKE_MINUTE,   0)

        updateSleepTimeDisplay()
        updateWakeTimeDisplay()

        // 點擊整個時間區域（數字 + 編輯圖示）皆可觸發選擇器
        findViewById<android.view.View>(R.id.time_sleep_area).setOnClickListener { showSleepTimePicker() }
        findViewById<android.view.View>(R.id.time_wake_area).setOnClickListener  { showWakeTimePicker() }

        btnStart.setOnClickListener { checkPermissionsAndStart() }
        btnStop.setOnClickListener  { stopGuardian() }
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

    // ── 依序檢查所有必要權限
    private fun checkPermissionsAndStart() {
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
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        attemptStart()
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

        // 計算睡覺時間（若今日時間已過則設為明天）
        val sleepTarget = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // 計算起床時間（以睡覺時間為基準往後找，確保一定在睡覺時間之後）
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

        val intent = Intent(this, SleepService::class.java).apply {
            putExtra(SleepService.EXTRA_SLEEP_TIME, sleepTarget.timeInMillis)
            putExtra(SleepService.EXTRA_WAKE_TIME,  wakeTarget.timeInMillis)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
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

            val wakeStr = if (wakeMillis > 0) {
                val cal = Calendar.getInstance().apply { timeInMillis = wakeMillis }
                String.format("，%02d:%02d 自動關閉", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } else ""

            tvStatus.text = "✅ 睡眠引導背景執行中$wakeStr"
        } else {
            tvPhase.text  = "尚未啟動"
            tvStatus.text = "點「開始引導」讓引導在背景運行"
        }
    }
}
