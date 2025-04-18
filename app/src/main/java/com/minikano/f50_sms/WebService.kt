package com.minikano.f50_sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream


class WebService : Service() {
    private var webServer: WebServer? = null
    private val port = 2333
    private val SERVER_INTENT = "com.minikano.f50_sms.SERVER_STATUS_CHANGED"
    private val UI_INTENT = "com.minikano.f50_sms.UI_STATUS_CHANGED"
    private val PREFS_NAME = "kano_ZTE_store"

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d("kano_ZTE_LOG", "WebService 收到 Intent")
            if (action == UI_INTENT) {
                val shouldStart = intent.getBooleanExtra("status", false)
                if (shouldStart) {
                    startWebServer()
                } else {
                    stopWebServer()
                }
            }
        }
    }

    private fun runADB(){
        //网络adb
        //adb setprop service.adb.tcp.port 5555
        Thread {
            try {
                ShellKano.runShellCommand("/system/bin/setprop persist.service.adb.tcp.port 5555")
                ShellKano.runShellCommand("/system/bin/setprop service.adb.tcp.port 5555")
                Log.d("kano_ZTE_LOG", "网络adb调试执行成功")
            }catch(e:Exception) {
                try {
                    ShellKano.runShellCommand("/system/bin/setprop service.adb.tcp.port 5555")
                    ShellKano.runShellCommand("/system/bin/setprop persist.service.adb.tcp.port 5555")
                    Log.d("kano_ZTE_LOG", "网络adb调试执行成功")
                }catch(e:Exception) {
                    Log.d("kano_ZTE_LOG", "网络adb调试出错： ${e.message}")
                }
            }

            try{
                val sharedPrefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                val ADB_IP_ENABLED = sharedPrefs.getString("ADB_IP_ENABLED", "") ?: null

                if(ADB_IP_ENABLED == "true") {

                    val ADB_IP =
                        sharedPrefs.getString("ADB_IP", "") ?: throw Exception("没有ADMIN_IP")
                    val ADMIN_PWD =
                        sharedPrefs.getString("ADMIN_PWD", "") ?: throw Exception("没有ADMIN_IP")

                    Log.d(
                        "kano_ZTE_LOG", "读取网络ADB所需配置：ADB_IP:${
                            ADB_IP
                        } ADMIN_PWD:${
                            ADMIN_PWD
                        }"
                    )

                    val adb_wifi = ShellKano.executeShellFromAssetsSubfolderWithArgs(
                        applicationContext,

                        "shell/adbPort",
                        "-ip", ADB_IP,
                        "-pwd", ADMIN_PWD,
                        "-port", "5555"
                    )
                    Log.d("kano_ZTE_LOG", "ADB_WIFI自启动执行结果：$adb_wifi")
                }else{
                    Log.d("kano_ZTE_LOG", "不需要自启动ADB_WIFI")
                }
            }catch (e:Exception){
                Log.d("kano_ZTE_LOG", "ADB_WIFI自启动执行错误：${e.message}")
                e.printStackTrace()
            }
            Thread.sleep(5000)
            try{
                Log.d("kano_ZTE_LOG", "adb服务正在启动。。。")
                val res_adb = ShellKano.executeShellFromAssetsSubfolderWithArgs(
                    applicationContext,
                    "shell/adb",
                    "devices"
                )
                Log.d("kano_ZTE_LOG", "adb启动执行结果：${res_adb}")
            }
            catch (e:Exception){
                Log.d("kano_ZTE_LOG", "adb服务启动失败：${e.message}")
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        // 注册广播接收器
        registerReceiver(statusReceiver, IntentFilter(UI_INTENT), Context.RECEIVER_EXPORTED)
        startForeground(114514, createNotification())
        startWebServer()

        runADB()

        Log.d("kano_ZTE_LOG", "WebService Init Success!")
    }

    private fun startWebServer() {
        val ip = getSharedPreferences("kano_ZTE_store", Context.MODE_PRIVATE)
            .getString("gateway_ip", "192.168.0.1:8080") ?: "192.168.0.1:8080"
        Thread {
            webServer = WebServer(applicationContext, port, ip)
            webServer?.start()
            Log.d("kano_ZTE_LOG", "Web server started on http://0.0.0.0:$port")
            Log.d("kano_ZTE_LOG", "Web server proxy IP: $ip")
            sendBroadcast(Intent(SERVER_INTENT).putExtra("status", true))
        }.start()
    }

    private fun stopWebServer() {
        webServer?.stop()
        sendBroadcast(Intent(SERVER_INTENT).putExtra("status", false))
        Log.d("kano_ZTE_LOG", "Web server stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWebServer()
    }

    private fun createNotification(): Notification {
        val channelId = "web_server_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Web Server",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ZTE Tools Web Server")
            .setContentText("服务正在后台运行中")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 替换成你的图标
            .setOngoing(true)

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val channelId = "running_service"
        val channelName = "服务器状态"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        Log.d("kano_ZTE_LOG", "通知已建立")
    }
}