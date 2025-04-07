package com.minikano.f50_sms

import WebServer
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.util.TypedValueCompat.dpToPx
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        WebView.setWebContentsDebuggingEnabled(true) // 开启调试模式

        webView.settings.apply {
            javaScriptEnabled = true // 启用 JavaScript
            domStorageEnabled = true // 启用 DOM 存储
            allowFileAccess = true // 允许访问本地文件
            allowContentAccess = true // 允许访问 WebView 内部内容
            javaScriptCanOpenWindowsAutomatically = true // 允许 JS 打开新窗口
            allowFileAccess = true  // 允许 file:// 访问本地文件
            allowContentAccess = true // 允许访问内容 URI
            allowFileAccessFromFileURLs = true // 允许 file:// 加载其他 file:// 资源
            allowUniversalAccessFromFileURLs = true // 允许 file:// 访问 http/https 资源
        }

        val jsContext = JSInterface()
        webView.addJavascriptInterface(jsContext, "KANO_INTERFACE") // 绑定接口

        val gatewayIp = IPManager.getWifiGatewayIp(this)
        if (gatewayIp != null) {
//            Toast.makeText(this, "当前网关地址：$gatewayIp", Toast.LENGTH_SHORT).show()
            start(gatewayIp)
        } else {
            showAddressInputDialog(this) { inputAddress ->
                if(isValidIPv4(inputAddress)){
                    start(inputAddress)
                } else {
                    Toast.makeText(this, "输入必须是ip地址！", Toast.LENGTH_SHORT).show()
                }
            }
            Toast.makeText(this, "未获取到网关地址", Toast.LENGTH_SHORT).show()
        }
    }

    private fun start(gatewayIp: String){
        // 启动 Web 服务器
        startWebServer(gatewayIp)
        // 加载本地 HTML 页面
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun isValidIPv4(ip: String): Boolean {
        val regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(:([0-9]{1,5}))?\$"
        return ip.matches(regex.toRegex())
    }

    private fun showAddressInputDialog(context: Context, onAddressEntered: (String) -> Unit) {
        val editText = EditText(context).apply {
            hint = "IP地址"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        // 包裹 EditText 并设置左右 padding
        val container = FrameLayout(context).apply {
            setPadding(24, 0, 24, 0)
            addView(editText)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("输入IP地址")
            .setView(container)
            .setPositiveButton("确定", null)  // 不设置点击事件，防止直接关闭
            .setNegativeButton("退出") { _, _ ->
                exitProcess(0)
            }
            .create().apply {
                setCanceledOnTouchOutside(false)  // 禁止点击空白区域取消
            }

        dialog.show()

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val input = editText.text.toString().trim()
            if (!isValidIPv4(input)) {
                // 输入的不是有效的 IP 地址
                Toast.makeText(context, "输入必须是有效的IP地址！", Toast.LENGTH_SHORT).show()
            } else {
                onAddressEntered(input)
                dialog.dismiss()  // 关闭对话框
            }
        }
    }

    private fun startWebServer(gatewayIp:String) {
        // 在子线程中启动 WebServer
        Thread {
            try {
                val webServer = WebServer(applicationContext, 8090, gatewayIp)  // 使用端口 8090
                webServer.start()
                Log.d("WebServer", "Server started successfully")
            } catch (e: Exception) {
                Log.e("WebServer", "Error starting server", e)
            }
        }.start()
    }
}