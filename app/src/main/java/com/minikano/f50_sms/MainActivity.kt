package com.minikano.f50_sms

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
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

        val versionName = this.packageManager.getPackageInfo(this.packageName, 0).versionName

        val intent = Intent(this, WebService::class.java)
        startForegroundService(intent)

        // 注册广播
        registerReceiver(serverStatusReceiver, IntentFilter(SERVER_INTENT),
            Context.RECEIVER_EXPORTED
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
                    serverAddress = "http://localhost:$port",
                    gatewayIp,
                    versionName = versionName ?: "未知" ,
                    onStopServer = {
                        sendBroadcast(Intent(UI_INTENT).putExtra("status", false))
                        Log.d("kano_ZTE_LOG", "user touched stop btn")
                    }
                )
            } else {
                InputUI(
                    gatewayIp = gatewayIp,
                    onGatewayIpChange = { gatewayIp = it },
                    versionName = versionName ?: "未知" ,
                    onConfirm = {
                        // 保存并重启服务器
                        sharedPrefs.edit().putString(PREF_GATEWAY_IP, gatewayIp).apply()
                        sendBroadcast(Intent(UI_INTENT).putExtra("status", true))
                        Log.d("kano_ZTE_LOG", "user touched start btn")
                    }
                )
            }
        }

        //网络adb
        //adb setprop service.adb.tcp.port 5555
        Thread {
            try {
                var result =
                    ShellKano.runShellCommand("/system/bin/setprop service.adb.tcp.port 5555") // 你可以替换成其他命令
                result += "\n" + ShellKano.runShellCommand("/system/bin/setprop persist.service.adb.tcp.port 5555") // 你可以替换成其他命令
                Log.d("kano_ZTE_LOG", "网络adb调试： $result")
            }catch(e:Exception) {}
        }.start()
    }

    private val serverStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == SERVER_INTENT) {
                val isRunning = intent.getBooleanExtra("status", false) ?: false
                Log.d("kano_ZTE_LOG", "isServerRunning is $isRunning")
                serverStatusLiveData.postValue(isRunning)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serverStatusReceiver)
    }
}

@Composable
fun InputUI(gatewayIp: String, onGatewayIpChange: (String) -> Unit, onConfirm: () -> Unit,versionName:String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(16.dp), // 圆角
            elevation = CardDefaults.cardElevation(8.dp), // 阴影
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // 外边距
        ) {
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
                Text("Created by Minikano with ❤️ ver: ${versionName}", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                HyperlinkText(
                    "View source code on Github(Minikano)",
                    "Github(Minikano)",
                    fontSize = 12.sp,
                    "https://github.com/kanoqwq/F50-SMS"
                )
            }
        }
    }
}

@Composable
fun ServerUI(serverAddress: String,gatewayIP:String, onStopServer: () -> Unit,versionName:String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(16.dp), // 圆角
            elevation = CardDefaults.cardElevation(8.dp), // 阴影
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // 外边距
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("服务运行中", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                HyperlinkText(
                    "页面地址: $serverAddress",
                    serverAddress,
                    fontSize = 16.sp,
                    url = serverAddress
                )
                Spacer(modifier = Modifier.height(16.dp))
                HyperlinkText(
                    "网关地址: $gatewayIP",
                    gatewayIP,
                    fontSize = 16.sp,
                    url = "http://$gatewayIP"
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onStopServer) {
                    Text("停止服务")
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Created by Minikano with ❤️ ver: ${versionName}", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                HyperlinkText(
                    "View source code on Github(Minikano)",
                    "Github(Minikano)",
                    fontSize = 12.sp,
                    "https://github.com/kanoqwq/F50-SMS"
                )
            }
        }
    }
}

@Composable
fun HyperlinkText(
    fullText: String,
    linkText: String,
    fontSize: TextUnit,
    url: String,
) {
    val context = LocalContext.current
    val annotatedText = buildAnnotatedString {
        // 整段默认字体大小
        withStyle(style = SpanStyle(fontSize = fontSize)) {
            append(fullText)
        }

        val startIndex = fullText.indexOf(linkText)
        val endIndex = startIndex + linkText.length

        if (startIndex >= 0) {
            // 链接部分样式（覆盖字体大小、颜色、下划线）
            addStyle(
                style = SpanStyle(
                    color = Color(0xFF1E88E5),
                    textDecoration = TextDecoration.Underline,
                    fontSize = fontSize
                ),
                start = startIndex,
                end = endIndex,
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
        style = TextStyle(fontSize = fontSize), // 控制整体字体大小
        onClick = { offset ->
            annotatedText.getStringAnnotations("URL", offset, offset)
                .firstOrNull()?.let {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                    context.startActivity(intent)
                }
        }
    )
}