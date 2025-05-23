package com.minikano.f50_sms

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    companion object {
        const val REQUEST_CODE_NOTIFICATION = 114514
        const val REQUEST_CODE_SMS = 1919810
    }
    private val port = 2333
    private val PREFS_NAME = "kano_ZTE_store"
    private val PREF_GATEWAY_IP = "gateway_ip"
    private val PREF_LOGIN_TOKEN = "login_token"
    private val PREF_TOKEN_ENABLED = "login_token_enabled"
    private val PREF_AUTO_IP_ENABLED = "auto_ip_enabled"
    private val serverStatusLiveData = MutableLiveData<Boolean>()
    private val SERVER_INTENT = "com.minikano.f50_sms.SERVER_STATUS_CHANGED"
    private val UI_INTENT = "com.minikano.f50_sms.UI_STATUS_CHANGED"
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestNotificationPermissionIfNeeded()

        // 忽略电池优化权限
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(this.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${this.packageName}")
            this.startActivity(intent)
        }

        //用户使用量权限
        if (!hasUsageAccessPermission(this)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        }

        val versionName = this.packageManager.getPackageInfo(this.packageName, 0).versionName

        //每次启动时需要检测IP变动，适应用户ip网段更改
        KanoUtils.adaptIPChange(applicationContext)

        //防止服务重复启动
        if (!isServiceRunning(WebService::class.java)) {
            startForegroundService(Intent(this, WebService::class.java))
        }
        if (!isServiceRunning(ADBService::class.java)) {
            startForegroundService(Intent(this, ADBService::class.java))
        }

        // 注册广播
        registerReceiver(
            serverStatusReceiver, IntentFilter(SERVER_INTENT),
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
                        "192.168.0.1:8080"
                    ) ?: "192.168.0.1:8080"
                )
            }

            var loginToken by remember {
                mutableStateOf(
                    sharedPrefs.getString(
                        PREF_LOGIN_TOKEN,
                        "admin"
                    ) ?: "admin"
                )
            }
            var isTokenEnabled by remember {
                mutableStateOf(
                    sharedPrefs.getString(
                        PREF_TOKEN_ENABLED,
                        true.toString()
                    ) ?: true.toString()
                )
            }
            var isAutoIpEnabled by remember {
                mutableStateOf(
                    sharedPrefs.getString(
                        PREF_AUTO_IP_ENABLED,
                        true.toString()
                    ) ?: true.toString()
                )
            }
            if (isServerRunning) {
                ServerUI(
                    serverAddress = "http://${gatewayIp.substringBefore(":")}:$port",
                    gatewayIp,
                    versionName = versionName ?: "未知",
                    onStopServer = {
                        sendBroadcast(Intent(UI_INTENT).putExtra("status", false))
                        serverStatusLiveData.postValue(false)
                        Log.d("kano_ZTE_LOG", "user touched stop btn")
                    }
                )
            } else {
                InputUI(
                    gatewayIp = gatewayIp,
                    onGatewayIpChange = { gatewayIp = it },
                    loginToken = loginToken,
                    versionName = versionName ?: "未知",
                    onLoginTokenChange = { loginToken = it },
                    isTokenEnabled = isTokenEnabled == true.toString(),
                    isAutoCheckIp = isAutoIpEnabled == true.toString(),
                    onTokenEnableChange = { isTokenEnabled = it.toString() },
                    onAutoCheckIpChange = {
                        isAutoIpEnabled = it.toString()
                        if (it.toString() == true.toString()) {
                            KanoUtils.adaptIPChange(context,true) { newIp ->
                                gatewayIp = newIp // 更新 Compose 状态变量，UI 立即更新
                            }
                        }
                    },
                    onConfirm = {
                        // 保存并重启服务器
                        sharedPrefs.edit().putString(PREF_GATEWAY_IP, gatewayIp).apply()
                        sharedPrefs.edit().putString(PREF_LOGIN_TOKEN, loginToken).apply()
                        sharedPrefs.edit().putString(PREF_TOKEN_ENABLED, isTokenEnabled).apply()
                        sharedPrefs.edit().putString(PREF_AUTO_IP_ENABLED, isAutoIpEnabled).apply()
                        sendBroadcast(Intent(UI_INTENT).putExtra("status", true))
                        serverStatusLiveData.postValue(true)
                        Log.d("kano_ZTE_LOG", "user touched start btn")
                        runADB()
                    }
                )
            }
        }

        runADB()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION
            )
        } else {
            requestSmsPermissionIfNeeded()
        }
    }

    private fun requestSmsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_SMS),
                REQUEST_CODE_SMS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            requestSmsPermissionIfNeeded()
        }
        if (requestCode == REQUEST_CODE_SMS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("权限", "短信权限已授予")
            } else {
                Log.d("权限", "短信权限被拒绝")
            }
        }
    }

    private fun runADB() {
        //网络adb
        //adb setprop service.adb.tcp.port 5555
        Thread {
            try {
                ShellKano.runShellCommand("/system/bin/setprop persist.service.adb.tcp.port 5555")
                ShellKano.runShellCommand("/system/bin/setprop service.adb.tcp.port 5555")
                Log.d("kano_ZTE_LOG", "网络adb调试执行成功")
            } catch (e: Exception) {
                try {
                    ShellKano.runShellCommand("/system/bin/setprop service.adb.tcp.port 5555")
                    ShellKano.runShellCommand("/system/bin/setprop persist.service.adb.tcp.port 5555")
                    Log.d("kano_ZTE_LOG", "网络adb调试执行成功")
                } catch (e: Exception) {
                    Log.d("kano_ZTE_LOG", "网络adb调试出错： ${e.message}")
                }
            }
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

    fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

@Composable
fun InputUI(
    gatewayIp: String,
    onGatewayIpChange: (String) -> Unit,
    loginToken: String,
    onLoginTokenChange: (String) -> Unit,
    onConfirm: () -> Unit,
    versionName: String,
    isTokenEnabled: Boolean,
    isAutoCheckIp: Boolean,
    onTokenEnableChange: (Boolean) -> Unit,
    onAutoCheckIpChange: (Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
                    enabled = !isAutoCheckIp,
                    singleLine = true
                )


                // 登录口令输入框
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = loginToken,
                    enabled = isTokenEnabled,
                    onValueChange = onLoginTokenChange,
                    label = { Text("登录口令(默认admin)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(16.dp))
                // 开关：是否启用登录口令
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("自动检测IP")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isAutoCheckIp,
                        onCheckedChange = onAutoCheckIpChange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("启用登录口令")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isTokenEnabled,
                        onCheckedChange = onTokenEnableChange
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onConfirm) {
                    Text("启动服务")
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Created by Minikano with ❤️ ver: $versionName", fontSize = 12.sp)
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
fun ServerUI(
    serverAddress: String,
    gatewayIP: String,
    onStopServer: () -> Unit,
    versionName: String
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(16.dp), // 圆角
            elevation = CardDefaults.cardElevation(8.dp), // 阴影
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // 外边距
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
                Spacer(modifier = Modifier.height(16.dp))
                HyperlinkText(
                    "在手机安装使用的，请点击localhost:2333跳转",
                    "localhost:2333",
                    fontSize = 10.sp,
                    url = "http://localhost:2333"
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("可点击停止服务更改网关和口令密码(默认admin)", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "本软件需安装在随身WiFi机内，安装在手机上会导致部分功能不可用",
                    fontSize = 10.sp
                )
                Text("如需手机使用，请下载手机独立版", fontSize = 10.sp)
                Spacer(modifier = Modifier.height(20.dp))
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
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                        val packageManager = context.packageManager
                        if (intent.resolveActivity(packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "无法打开链接，系统未安装浏览器", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "打开链接失败", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    )
}