package com.minikano.f50_sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.minikano.f50_sms.ShellKano.Companion.executeShellFromAssetsSubfolderWithArgs

class ADBService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    companion object {
        @Volatile
        var adbIsReady: Boolean = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())

        runnable = object : Runnable {
            override fun run() {
                Log.d("MyService", "保活ADB服务中...")
                handler.postDelayed(this, 11_000) // 每 11 秒执行一次
                try {
                    val adbPath = "shell/adb"
                    // 第一次检测
                    var result = executeShellFromAssetsSubfolderWithArgs(applicationContext, adbPath, "devices")
                    Log.d("kano_ZTE_LOG", "adb device 执行状态：$result")

                    if (result?.contains("localhost:5555\tdevice") == true) {
                        Log.d("kano_ZTE_LOG", "adb存活，无需启动")
                        adbIsReady = true
                        return
                    }

                    adbIsReady = false

                    Log.w("kano_ZTE_LOG", "adb无设备或已退出，尝试启动")

                    // 重启 ADB server
                    executeShellFromAssetsSubfolderWithArgs(applicationContext, adbPath, "kill-server")
                    Thread.sleep(1000)
                    executeShellFromAssetsSubfolderWithArgs(applicationContext, adbPath, "connect", "localhost")

                    // 等待最多 10 秒，设备变为 "device"
                    val maxWaitMs = 10_000
                    val interval = 500
                    var waited = 0

                    while (waited < maxWaitMs) {
                        result = executeShellFromAssetsSubfolderWithArgs(applicationContext, adbPath, "devices")
                        Log.d("kano_ZTE_LOG", "等待 ADB 启动中：$result")
                        if (result?.contains("localhost:5555\tdevice") == true) {
                            Log.d("kano_ZTE_LOG", "ADB连接成功")
                            return
                        }
                        Thread.sleep(interval.toLong())
                        waited += interval
                    }

                    Log.e("kano_ZTE_LOG", "等待ADB device超时")
                    return
                } catch (e: Exception) {
                    Log.e("kano_ZTE_LOG", "检测/启动ADB失败: ${e.message}")
                    return
                }
            }
        }
        handler.post(runnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "kano_adb_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "adb_service后台服务", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("adb_service后台运行中")
            .setContentText("正在执行adb_service定时任务")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}