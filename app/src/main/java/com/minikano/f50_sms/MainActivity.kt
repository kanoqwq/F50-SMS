package com.minikano.f50_sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var webServer: WebServer? = null
    private val port = 2333
    private val PREFS_NAME = "kano_ZTE_store"
    private val PREF_GATEWAY_IP = "gateway_ip"
    private val serverStatusLiveData = MutableLiveData<Boolean>()
    private val SERVER_INTENT = "com.minikano.f50_sms.SERVER_STATUS_CHANGED"
    private val UI_INTENT = "com.minikano.f50_sms.UI_STATUS_CHANGED"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    114514 // 请求码随便定义一个
                )
            }
        }

        val intent = Intent(this, WebService::class.java)
        startForegroundService(intent)

        // 注册广播
        registerReceiver(serverStatusReceiver, IntentFilter(SERVER_INTENT),
            RECEIVER_NOT_EXPORTED
        )

        setContent {
            val context = this@MainActivity
            val sharedPrefs = remember {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }

            val isServerRunning by serverStatusLiveData.observeAsState(false)

            var gatewayIp by remember {
                mutableStateOf(
                    sharedPrefs.getString(
                        PREF_GATEWAY_IP,
                        IPManager.getWifiGatewayIp(context) ?: "192.168.0.1:8080"
                    ) ?: "192.168.0.1:8080"
                )
            }

            if (isServerRunning) {
                ServerUI(
                    serverAddress = "http://0.0.0.0:$port",
                    gatewayIp,
                    onStopServer = {
                        sendBroadcast(Intent(UI_INTENT).putExtra("status", false))
                    }
                )
            } else {
                InputUI(
                    gatewayIp = gatewayIp,
                    onGatewayIpChange = { gatewayIp = it },
                    onConfirm = {
                        // 保存并重启服务器
                        sharedPrefs.edit().putString(PREF_GATEWAY_IP, gatewayIp).apply()
                        sendBroadcast(Intent(UI_INTENT).putExtra("status", true))
                    }
                )
            }
        }
    }

    private val serverStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isRunning = intent?.getBooleanExtra("status", false) ?: false
            serverStatusLiveData.postValue(isRunning)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serverStatusReceiver)
    }
}

@Composable
fun InputUI(gatewayIp: String, onGatewayIpChange: (String) -> Unit, onConfirm: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("服务已停止", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("请输入路由器管理 IP", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = gatewayIp,
                onValueChange = onGatewayIpChange,
                label = { Text("路由器管理 IP") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onConfirm) {
                Text("启动服务")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Created by Minikano with ❤️", fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            HyperlinkText("View Source on Github(Minikano)","Github(Minikano)","https://github.com/kanoqwq/F50-SMS")
        }
    }
}

@Composable
fun ServerUI(serverAddress: String,gatewayIP:String, onStopServer: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("服务运行中", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("页面地址: $serverAddress", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("网关地址: $gatewayIP", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onStopServer) {
                Text("停止服务")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Created by Minikano with ❤️", fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            HyperlinkText("View Source on Github(Minikano)","Github(Minikano)","https://github.com/kanoqwq/F50-SMS")
        }
    }
}

@Composable
fun HyperlinkText(
    fullText: String,
    linkText: String,
    url: String
) {
    val context = LocalContext.current
    val annotatedText = buildAnnotatedString {
        val startIndex = fullText.indexOf(linkText)
        val endIndex = startIndex + linkText.length

        append(fullText)

        if (startIndex >= 0) {
            addStyle(
                style = SpanStyle(
                    color = Color(0xFF1E88E5),
                    textDecoration = TextDecoration.Underline
                ),
                start = startIndex,
                end = endIndex
            )

            addStringAnnotation(
                tag = "URL",
                annotation = url,
                start = startIndex,
                end = endIndex
            )
        }
    }

    ClickableText(
        text = annotatedText,
        style = MaterialTheme.typography.bodyLarge,
        onClick = { offset ->
            annotatedText.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                    context.startActivity(intent)
                }
        }
    )
}