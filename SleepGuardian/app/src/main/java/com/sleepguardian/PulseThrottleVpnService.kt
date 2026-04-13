package com.sleepguardian

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat

/**
 * 近似降速引擎：
 * 透過週期性「阻斷/放行」全系統流量，形成平均吞吐降低效果。
 * 注意：這不是封包級精準限速，但可在無 root 裝置落地運作。
 */
class PulseThrottleVpnService : VpnService() {

    companion object {
        const val ACTION_START_OR_UPDATE = "com.sleepguardian.action.START_OR_UPDATE_THROTTLE"
        const val ACTION_STOP = "com.sleepguardian.action.STOP_THROTTLE"
        const val EXTRA_TARGET_KBPS = "extra_target_kbps"

        private const val CHANNEL_ID = "sleep_guardian_throttle_channel"
        private const val NOTI_ID = 2
    }

    private val handler = Handler(Looper.getMainLooper())
    private var tunInterface: ParcelFileDescriptor? = null
    private var targetKbps: Int = 2500
    private var cycleMs: Long = 4000L
    private var blockMs: Long = 800L
    private var isBlocked = false

    private val pulseRunnable = object : Runnable {
        override fun run() {
            if (isBlocked) {
                teardownBlockTunnel()
                isBlocked = false
                handler.postDelayed(this, (cycleMs - blockMs).coerceAtLeast(200L))
            } else {
                setupBlockTunnel()
                isBlocked = true
                handler.postDelayed(this, blockMs.coerceAtLeast(200L))
            }
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPulseThrottle()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_OR_UPDATE -> {
                val requested = intent.getIntExtra(EXTRA_TARGET_KBPS, 2500).coerceAtLeast(500)
                targetKbps = requested
                val blockRatio = mapTargetToBlockRatio(targetKbps)
                blockMs = (cycleMs * blockRatio).toLong().coerceIn(200L, cycleMs - 200L)

                startForeground(NOTI_ID, buildNotification())
                restartPulseLoop()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPulseThrottle()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun restartPulseLoop() {
        handler.removeCallbacks(pulseRunnable)
        teardownBlockTunnel()
        isBlocked = false
        handler.post(pulseRunnable)
    }

    private fun stopPulseThrottle() {
        handler.removeCallbacks(pulseRunnable)
        teardownBlockTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /**
     * 建立一條不轉發的 TUN，等同暫時阻斷網路。
     */
    private fun setupBlockTunnel() {
        if (tunInterface != null) return
        tunInterface = Builder()
            .setSession("SleepGuardianThrottle")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)
            .establish()
    }

    private fun teardownBlockTunnel() {
        try {
            tunInterface?.close()
        } catch (_: Exception) {
        }
        tunInterface = null
    }

    private fun mapTargetToBlockRatio(kbps: Int): Double {
        return when {
            kbps <= 500 -> 0.88
            kbps <= 700 -> 0.78
            kbps <= 900 -> 0.65
            kbps <= 1300 -> 0.50
            kbps <= 1800 -> 0.35
            else -> 0.20
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "睡眠降速",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "網路節流服務通知"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("睡眠降速中")
            .setContentText("目標約 ${targetKbps}kbps（脈衝節流）")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTI_ID, buildNotification())
    }
}
