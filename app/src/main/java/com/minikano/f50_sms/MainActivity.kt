package com.minikano.f50_sms

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        WebView.setWebContentsDebuggingEnabled(true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val host = url.host ?: return false

                return if (host.contains("github.com")) {
                    // 手动跳转外部浏览器
                    val intent = Intent(Intent.ACTION_VIEW, url)
                    view?.context?.startActivity(intent)
                    true
                } else {
                    // 所有其他链接都在 WebView 内打开
                    view?.loadUrl(url.toString())
                    true
                }
            }
        }

        webView.addJavascriptInterface(JSInterface(this), "KANO_INTERFACE_API")
        showLoadingDialog()
        // 异步处理 IP 获取
        Thread {
            var gatewayIp = IPManager.getWifiGatewayIp(this)
            val ipAddr = resolveDomainToIpSync("ufi.ztedevice.com")

            if (gatewayIp != null && ipAddr != null) {
                if (ipAddr != gatewayIp) {
                    gatewayIp = ipAddr
                }

                // 回到主线程更新 UI
                runOnUiThread {
                    Toast.makeText(this, "当前网关地址：$gatewayIp", Toast.LENGTH_SHORT).show()
                    start(gatewayIp)
                }
            } else {
                runOnUiThread {
                    showAddressInputDialog(this) { inputAddress ->
                        if (isValidIPv4(inputAddress)) {
                            start(inputAddress)
                        } else {
                            Toast.makeText(this, "输入必须是ip地址！", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Toast.makeText(this, "未获取到网关地址", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private lateinit var loadingDialog: AlertDialog

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false) // 模态，不允许取消

        val progressBar = ProgressBar(this)
        val padding = 100
        progressBar.setPadding(padding, padding, padding, padding)
        builder.setView(progressBar)

        loadingDialog = builder.create()
        loadingDialog.show()
    }

    private fun dismissLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    fun resolveDomainToIpSync(domain: String): String? {
        return try {
            val inetAddress = InetAddress.getByName(domain)
            inetAddress.hostAddress
        } catch (e: UnknownHostException) {
            e.printStackTrace()
            null
        }
    }

    private fun start(gatewayIp: String){
        // 启动 Web 服务器
        startWebServer(gatewayIp)
        Handler(Looper.getMainLooper()).postDelayed({
            // 加载本地 HTML 页面
            webView.loadUrl("http://localhost:8090")
            dismissLoadingDialog()
        },500)
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