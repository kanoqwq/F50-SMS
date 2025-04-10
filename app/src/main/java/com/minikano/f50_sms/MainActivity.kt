package com.minikano.f50_sms

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gatewayIp = "127.0.0.1:8080"
        start(gatewayIp)
    }

    private fun start(gatewayIp: String){
        // 启动 Web 服务器
        startWebServer(gatewayIp)
    }

    private fun isValidIPv4(ip: String): Boolean {
        val regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(:([0-9]{1,5}))?\$"
        return ip.matches(regex.toRegex())
    }
    private fun startWebServer(gatewayIp:String) {
        // 在子线程中启动 WebServer
        Thread {
            try {
                val webServer = WebServer(applicationContext, 2333, gatewayIp)  // 使用端口 2333
                webServer.start()
                Log.d("WebServer", "Server started successfully")
            } catch (e: Exception) {
                Log.e("WebServer", "Error starting server", e)
            }
        }.start()
    }
}