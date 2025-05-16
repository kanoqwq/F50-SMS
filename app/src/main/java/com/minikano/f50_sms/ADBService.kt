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

        Thread {
            try {
                val adbPath = "shell/adb"

                while (true) {
                    //激活SMB指令
                    Log.d("kano_ZTE_LOG", "激活SMB内置脚本中...")
                    SmbThrottledRunner.runOnceInThread()

                    Log.d("kano_ZTE_LOG", "保活ADB服务中...")
                    var result = executeShellFromAssetsSubfolderWithArgs(applicationContext, adbPath, "devices",
                        onTimeout = {
                            ShellKano.killProcessByName("adb")
                        })
                    Log.d("kano_ZTE_LOG", "adb devices 执行状态：$result")

                    if (result?.contains("localhost:5555\tdevice") == true) {
                        Log.d("kano_ZTE_LOG", "adb存活，无需启动")
                        adbIsReady = true
                    } else {
                        Log.w("kano_ZTE_LOG", "adb无设备或已退出，尝试启动")
                        adbIsReady = false

                        // 重启 ADB 服务
                        ShellKano.killProcessByName("adb")
//                        executeShellFromAssetsSubfolderWithArgs(applicationContext, adbPath, "kill-server", logTag = "kill-server")
                        Thread.sleep(1000)
                        executeShellFromAssetsSubfolderWithArgs(applicationContext, adbPath, "connect", "localhost",
                            onTimeout = {
                                ShellKano.killProcessByName("adb")
                            })

                        // 等待最多 5 秒，看设备是否连接成功
                        val maxWaitMs = 5_000
                        val interval = 500
                        var waited = 0

                        while (waited < maxWaitMs) {
                            result = executeShellFromAssetsSubfolderWithArgs(applicationContext, adbPath, "devices",
                                onTimeout = {
                                ShellKano.killProcessByName("adb")
                            })
                            if (result?.contains("localhost:5555\tdevice") == true) {
                                Log.d("kano_ZTE_LOG", "ADB连接成功: $result")
                                adbIsReady = true
                                break
                            } else {
                                Log.d("kano_ZTE_LOG", "未等待到ADB成功结果：$result")
                            }

                            Thread.sleep(interval.toLong())
                            waited += interval
                        }
                    }

                    // 等待下一轮检测
                    Thread.sleep(11_000)
                }
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "ADB 保活线程异常: ${e.message}")
            }
        }.start()

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