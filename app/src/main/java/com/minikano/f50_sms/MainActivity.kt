package com.minikano.f50_sms

import WebServer
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import java.net.HttpURLConnection
import java.net.URL

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

//         设置 WebViewClient 代理请求
//        webView.webViewClient = object : WebViewClient() {
//            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
//                return proxyRequest(request)
//            }
//        }

        // 启动 Web 服务器
        startWebServer()

        // 加载本地 HTML 页面
//        webView.loadUrl("http://localhost:8090")
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun startWebServer() {
        // 在子线程中启动 WebServer
        Thread {
            try {
                val webServer = WebServer(applicationContext, 8090)  // 使用端口 8090
                webServer.start()
                Log.d("WebServer", "Server started successfully")
            } catch (e: Exception) {
                Log.e("WebServer", "Error starting server", e)
            }
        }.start()
    }

    private fun proxyRequest(request: WebResourceRequest): WebResourceResponse? {
        try {
            val url = request.url.toString()


            // 仅针对 POST 请求
            if (request.method == "POST") {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = request.method

                // 设置 Referer 和其他请求头
                connection.setRequestProperty("Referer", "http://192.168.0.1")
                connection.setRequestProperty("User-Agent", System.getProperty("http.agent"))
                for ((key, value) in request.requestHeaders) {
                    connection.setRequestProperty(key, value)
                }
                connection.setRequestProperty("content-length", "101")

                connection.connect()
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return null
                }

                return WebResourceResponse(
                    connection.contentType,
                    connection.contentEncoding ?: "utf-8",
                    connection.inputStream
                )
            }

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = request.method

            // 复制请求头
            for ((key, value) in request.requestHeaders) {
                if(key != "kano-content-length"){
                    connection.setRequestProperty(key, value)
                }
            }

            // 手动设置请求头，直接复制需要的头部信息
            connection.setRequestProperty("User-Agent", System.getProperty("http.agent"))  // 可以手动设置 User-Agent 等
            connection.setRequestProperty("Content-Length", request.requestHeaders.get("kano-content-Length"))  // 可以手动设置 User-Agent 等
            connection.setRequestProperty("Referer", "http://192.168.0.1/index.html")  // 添加 Referer
            request.requestHeaders.set("Referer", "http://192.168.0.1/index.html")
            Log.d("WebViewProxy", "Intercepting request: $url")
            Log.d("WebViewProxy", "Request headers: ${connection.requestProperties}")

            // 你可以选择性地手动添加其他请求头
            // for ((key, value) in request.requestHeaders) {
            //     connection.setRequestProperty(key, value)
            // }

            // 连接并获取响应
            connection.connect()
            val responseCode = connection.responseCode
            Log.d("WebViewProxy", "Response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }

            return WebResourceResponse(
                connection.contentType,
                connection.contentEncoding ?: "utf-8",
                connection.inputStream
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}