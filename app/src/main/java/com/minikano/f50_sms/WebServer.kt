package com.minikano.f50_sms

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.util.Log
import com.minikano.f50_sms.ShellKano.Companion.fillInputAndSend
import com.minikano.f50_sms.ShellKano.Companion.runShellCommand
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random
import kotlin.concurrent.thread

class WebServer(context: Context, port: Int, gatewayIp: String) : NanoHTTPD(port) {

    private val targetServer = "http://$gatewayIp"  // 目标服务器地址
    private val targetServerIP = gatewayIp  // 目标服务器地址
    private val PREFS_NAME = "kano_ZTE_store"
    private val PREF_LOGIN_TOKEN = "login_token"
    private val PREF_TOKEN_ENABLED = "login_token_enabled"

    @Volatile
    var downloadInProgress = false

    @Volatile
    var download_percent = 0

    @Volatile
    var downloadResultPath: String? = null

    @Volatile
    var downloadError: String? = null

    @Volatile
    var currentDownloadingUrl: String = ""

//    init {
//        try {
//            val password = "kano123".toCharArray()
//
//            val assetManager = context.assets
//            val keystoreInputStream = assetManager.open("certs/192_168_0_1_ssl.bks")
//
//            val ks = KeyStore.getInstance("BKS") // 改为 BKS
//            ks.load(keystoreInputStream, password)
//            keystoreInputStream.close()
//
//            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
//            kmf.init(ks, password)
//
//            val sslContext = SSLContext.getInstance("TLS")
//            sslContext.init(kmf.keyManagers, null, null)
//
//            makeSecure(sslContext.serverSocketFactory, null)
//
//            Log.i("kano_ZTE_LOG", "SSL 启用成功")
//
//        } catch (e: Exception) {
//            Log.e("kano_ZTE_LOG", "SSL失败: ${e.message}", e)
//        }
//    }

    override fun serve(session: IHTTPSession?): Response {
        val method = session?.method.toString()
        val uri = session?.uri?.removePrefix("/api") ?: "/"
        val sharedPrefsForToken = context_app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val auth_token = sharedPrefsForToken.getString(PREF_LOGIN_TOKEN, "admin")
        val token_enabled = sharedPrefsForToken.getString(PREF_TOKEN_ENABLED, true.toString())
        try {
            if (token_enabled == "true") {
                if (session?.uri != null && session.uri.contains("/api")) {
                    // 获取请求头
                    val headers = session.headers ?: throw Exception("401")
                    val authHeader = headers["authorization"]
                    if (authHeader != auth_token) {
                        throw Exception("401")
                    }
                }
            }
        } catch (e: Exception) {
            return newFixedLengthResponse(
                Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "401 Unauthorized"
            )
        }

        //cpu温度
        if (method == "GET" && uri == "/temp") {
            return try {
                val temp = ShellKano.executeShellFromAssetsSubfolder(context_app, "shell/temp.sh")
                val temp1 = ShellKano.runShellCommand("cat /sys/class/thermal/thermal_zone1/temp")
                Log.d("kano_ZTE_LOG", "获取CPU温度成功: $temp")
                val response = newFixedLengthResponse(
                    Response.Status.OK, "application/json", """{"temp":${temp ?: temp1}}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "获取CPU温度出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"获取CPU温度失败"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //cpu使用率
        if (method == "GET" && uri == "/cpu") {
            return try {
                val stat1 =
                    ShellKano.runShellCommand("cat /proc/stat") ?: throw Exception("stat1没有数据")
                val (total1, idle1) = KanoUtils.parseCpuStat(stat1) ?: throw Exception("parseCpuStat执行失败")
                val stat2 =
                    ShellKano.runShellCommand("cat /proc/stat") ?: throw Exception("stat2没有数据")
                val (total2, idle2) = KanoUtils.parseCpuStat(stat2) ?: throw Exception("parseCpuStat执行失败")
                val totalDiff = total2 - total1
                val idleDiff = idle2 - idle1
                val usage = if (totalDiff > 0) (totalDiff - idleDiff).toFloat() / totalDiff else 0f

                Log.d("kano_ZTE_LOG", "CPU 使用率：%.2f%%".format(usage * 100))
                val response = newFixedLengthResponse(
                    Response.Status.OK, "application/json", """{"cpu":${usage * 100}}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "获取cpu使用率出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"获取cpu使用率出错"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //内存使用率
        if (method == "GET" && uri == "/mem") {
            return try {
                val info = runShellCommand("cat /proc/meminfo") ?: throw Exception("没有info")
                val usage = KanoUtils.parseMeminfo(info)
                Log.d("kano_ZTE_LOG", "内存使用率：%.2f%%".format(usage * 100))
                val response = newFixedLengthResponse(
                    Response.Status.OK, "application/json", """{"mem":${usage * 100}}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "获取内存信息出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"获取内存信息失败"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //自启无线adb
        if (method == "GET" && uri == "/adb_wifi_setting") {
            return try {
                val sharedPrefs = context_app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val ADB_IP_ENABLED = sharedPrefs.getString("ADB_IP_ENABLED", "false")
                val response = newFixedLengthResponse(
                    Response.Status.OK, "application/json", """{"enabled":$ADB_IP_ENABLED}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "获取网络adb信息出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"获取网络adb信息失败"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //发送AT指令
        if (method == "GET" && uri == "/AT") {
            // 创建一个 Piped 流来异步返回数据
            val pipedInput = PipedInputStream()
            val pipedOutput = PipedOutputStream(pipedInput)

            // 使用协程处理耗时任务
            CoroutineScope(Dispatchers.IO).launch {
                val writer = OutputStreamWriter(pipedOutput, Charsets.UTF_8)
                try {
                    // 解析 query 参数
                    val rawParams = session?.parameters ?: throw Exception("缺少 query 参数")
                    val AT_command_arr =
                        rawParams["command"] ?: throw Exception("qeury 缺少 command 参数")
                    val AT_slot_arr = rawParams["slot"] ?: throw Exception("qeury 缺少 slot 参数")
                    val AT_command = AT_command_arr[0]
                    val AT_slot = AT_slot_arr.getOrNull(0)?.toIntOrNull() ?: 0 // 如果取不到或不是数字，就用 0
                    Log.d("kano_ZTE_LOG", "AT_command 传入参数：${AT_command}")

                    //复制依赖
                    val outFile_at = KanoUtils.copyFileToFilesDir(context_app,"shell/sendat") ?: throw Exception("复制sendat 到filesDir失败")

                    outFile_at.setExecutable(true)

                    //AT+CGEQOSRDP=1
                    if (!AT_command.toString().startsWith("AT", ignoreCase = true)) {
                        throw Exception("解析失败，AT指令需要以 “AT” 开头")
                    }
                    val command =
                        "${outFile_at.absolutePath} -n $AT_slot -c '${AT_command.trimStart()}'"
                    val result = runShellCommand(command, true) ?: throw Exception("没有输出")
                    var res = result.replace("\"", "\\\"")      // 转义引号
                        .replace("\n", "")          // 去掉换行
                        .replace("\r", "")          // 去掉回车
                        .trimStart()
                    if (res.lowercase().endsWith("ok")) {
                        res = res.dropLast(2).trimEnd() + " OK"
                    }
                    // 去掉开头的逗号
                    if (res.startsWith(",")) {
                        res = res.removePrefix(",").trimStart()
                    }
                    Log.d("kano_ZTE_LOG", "AT_cmd：$command")
                    Log.d("kano_ZTE_LOG", "AT_result：$res")
                    val jsonResult = """{"result":"${res}"}"""
                    writer.write(jsonResult)
                } catch (e: Exception) {
                    writer.write("""{"error":"AT指令执行错误：${e.message}"}""")
                } finally {
                    writer.flush()
                    pipedOutput.close()
                }
            }

            val response = newChunkedResponse(
                Response.Status.OK, "application/json", pipedInput
            )
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        //更改samba分享地址为根目录
        if (method == "GET" && uri == "/smbPath") {
            // 创建一个 Piped 流来异步返回数据
            val pipedInput = PipedInputStream()
            val pipedOutput = PipedOutputStream(pipedInput)

            // 使用协程处理耗时任务
            CoroutineScope(Dispatchers.IO).launch {
                val writer = OutputStreamWriter(pipedOutput, Charsets.UTF_8)
                try {
                    // 解析 query 参数
                    val rawParams = session?.parameters ?: throw Exception("缺少 query 参数")
                    val qeury_arr =
                        rawParams["enable"] ?: throw Exception("qeury 缺少 enable 参数")
                    val enabled = qeury_arr[0]
                    Log.d("kano_ZTE_LOG", "enable 传入参数：${enabled}")

                    //复制依赖
                    val outFile_adb = KanoUtils.copyFileToFilesDir(context_app,"shell/adb") ?: throw Exception("复制adb 到filesDir失败")
                    val smb_path = SMBConfig.writeConfig(context_app)  ?: throw Exception("复制smb.conf 到filesDir失败")
                    val outFile_ttyd = KanoUtils.copyFileToFilesDir(context_app,"shell/ttyd") ?: throw Exception("复制ttyd 到filesDir失败")
                    val outFile_socat = KanoUtils.copyFileToFilesDir(context_app,"shell/socat") ?: throw Exception("socat 到filesDir失败")
                    val outFile_smb_sh = KanoUtils.copyFileToFilesDir(context_app,"shell/samba_exec.sh",false) ?: throw Exception("复制samba_exec.sh 到filesDir失败")

                    outFile_smb_sh.setExecutable(true)
                    outFile_adb.setExecutable(true)
                    outFile_ttyd.setExecutable(true)
                    outFile_socat.setExecutable(true)
                    var jsonResult = """{"result":"执行成功，重启生效！"}"""

                    if(enabled == "1") {
                        //输入指令，点击发送
                        runShellCommand(
                            "${outFile_adb.absolutePath} -s localhost shell cat ${smb_path} > /data/samba/etc/smb.conf",
                            context = context_app
                        ) ?: throw Exception("修改smb.conf失败")
                        jsonResult = """{"result":"执行成功，等待一会儿即可生效！"}"""
                    } else {
                        val script = """
                            sh /sdcard/ufi_tools_boot.sh
                            chattr -i /data/samba/etc/smb.conf
                            chmod 644 /data/samba/etc/smb.conf
                            rm -f /data/samba/etc/smb.conf
                            sync
                        """.trimIndent()
                        val socketPath = File(context_app.filesDir.absolutePath,"kano_root_shell.sock")
                        if(!socketPath.exists()){
                           throw Exception("执行失败，没有找到socat创建的sock")
                        }
                        val result =  RootShell.sendCommandToSocket(script, socketPath.absolutePath) ?: throw Exception("删除smb.conf失败")
                        Log.d("kano_ZTE_LOG", "sendCommandToSocket Output:\n$result")
                    }

                    //刷新SMB
                    Log.d("kano_ZTE_LOG", "刷新SMB中...")
                    SmbThrottledRunner.runOnceInThread(context_app)

                    writer.write(jsonResult)
                } catch (e: Exception) {
                    writer.write("""{"error":"执行错误：${e.message}"}""")
                } finally {
                    writer.flush()
                    pipedOutput.close()
                }
            }

            val response = newChunkedResponse(
                Response.Status.OK, "application/json", pipedInput
            )
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        //adb自启
        if (method == "POST" && uri == "/adb_wifi_setting") {
            return try {
                val map = HashMap<String, String>()
                session?.parseBody(map)  // 必须调用 parseBody 才能安全读 inputStream

                val body = map["postData"] ?: throw Exception("postData is null")
                val json = JSONObject(body)

                val enabled = json.optBoolean("enabled", false)
                val password = json.optString("password", "")

                Log.d("kano_ZTE_LOG", "接收到ADB_WIFI配置：enabled=$enabled, password=$password")

                val sharedPrefs = context_app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                val host = targetServerIP.substringBefore(":")

                //保存
                if (enabled == true) {
                    sharedPrefs.edit().putString("ADB_IP", host).putString("ADMIN_PWD", password)
                        .putString("ADB_IP_ENABLED", "true").apply()
                } else {
                    sharedPrefs.edit().remove("ADB_IP").remove("ADMIN_PWD")
                        .putString("ADB_IP_ENABLED", "false").apply()
                }

                Log.d(
                    "kano_ZTE_LOG", "保存结果：ADB_IP:${
                        sharedPrefs.getString("ADB_IP", "")
                    } ADMIN_PWD:${
                        sharedPrefs.getString("ADMIN_PWD", "")
                    }"
                )

                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"result":"success","enabled":"$enabled"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "解析ADB_WIFI POST 请求出错：${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"参数解析失败"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //型号与电量
        if (method == "GET" && uri == "/battery_and_model") {
            return try {
                val model = Build.MODEL // 设备型号
                val batteryLevel: Int = KanoUtils.getBatteryPercentage(context_app)// 充电状态
                val packageManager = context_app.packageManager
                val packageName = context_app.packageName
                val versionName = packageManager.getPackageInfo(packageName, 0).versionName
                val versionCode= packageManager.getPackageInfo(packageName, 0).versionCode

                Log.d("kano_ZTE_LOG", "型号与电量：$model $batteryLevel")

                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"app_ver":"$versionName","app_ver_code":"$versionCode","model":"$model","battery":"$batteryLevel"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "获取型号与电量信息出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"获取型号与电量信息出错"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //测速
        // from https://github.com/bg6cq/speedtest/blob/master/backend/garbage.php
        // 优化后的测速接口
        if (method == "GET" && uri == "/speedtest") {
            return try {
                val parms = session?.parameters ?: throw Exception("缺少 query 参数")
                val ckSize = KanoUtils.getChunkCount(parms["ckSize"]?.firstOrNull())
                val enableCors = parms.containsKey("cors")

                // Headers
                val headers = mutableMapOf<String, String>()
                if (enableCors) {
                    headers["Access-Control-Allow-Origin"] = "*"
                    headers["Access-Control-Allow-Methods"] = "GET, POST"
                }

                headers["Content-Description"] = "File Transfer"
                headers["Content-Type"] = "application/octet-stream"
                headers["Content-Disposition"] = "attachment; filename=random.dat"
                headers["Content-Transfer-Encoding"] = "binary"
                headers["Cache-Control"] =
                    "no-store, no-cache, must-revalidate, max-age=0, s-maxage=0"
                headers["Pragma"] = "no-cache"

                val chunkSizeBytes = 1024 * 1024  // 每个chunk 1MB
                val totalChunks = ckSize

                // 预生成一个随机1MB buffer
                val fixedBuffer = ByteArray(chunkSizeBytes).apply {
                    Random().nextBytes(this)
                }

                // 构造高效输出流
                val stream = object : InputStream() {
                    private var chunksSent = 0
                    private var bufferPos = 0

                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (chunksSent >= totalChunks) return -1

                        val bytesRemaining = chunkSizeBytes - bufferPos
                        val bytesToCopy = minOf(len, bytesRemaining)

                        System.arraycopy(fixedBuffer, bufferPos, b, off, bytesToCopy)
                        bufferPos += bytesToCopy

                        if (bufferPos >= chunkSizeBytes) {
                            chunksSent++
                            bufferPos = 0
                        }

                        return bytesToCopy
                    }

                    override fun read(): Int {
                        // fallback 单字节读取，仍然使用已有buffer
                        if (chunksSent >= totalChunks) return -1
                        val byteValue = fixedBuffer[bufferPos++].toInt() and 0xFF
                        if (bufferPos >= chunkSizeBytes) {
                            bufferPos = 0
                            chunksSent++
                        }
                        return byteValue
                    }
                }

                val response =
                    newChunkedResponse(Response.Status.OK, "application/octet-stream", stream)

                headers.forEach { (k, v) -> response.addHeader(k, v) }

                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "测速出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR, "application/json", """{"error":"测速出错"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //判断是否存在TTYD
        if (method == "GET" && uri == "/hasTTYD") {
            return try {
                // 解析 query 参数
                val rawParams = session?.parameters ?: throw Exception("缺少 port 参数")
                val portParam = rawParams["port"] ?: throw Exception("qeury 缺少 port 参数")

                val host = targetServerIP.substringBefore(":")

                val code = getStatusCode("http://$host:${portParam[0]}")

                Log.d("kano_ZTE_LOG", "TTYD获取ip+port信息： ${"$host:$portParam 返回code:$code"}")

                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"code":"$code","ip":"$host:${portParam[0]}"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "获取TTYD信息出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"获取TTYD信息出错:${e.message}"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //存储获取
        if (method == "GET" && uri == "/storage_and_dailyData") {
            return try {
                //内部存储
                val internalStorage: File = context_app.filesDir
                val statFs = StatFs(internalStorage.absolutePath)
                val totalSize = (statFs.blockSizeLong * statFs.blockCountLong)
                val availableSize = (statFs.blockSizeLong * statFs.availableBlocksLong)
                val usedSize = totalSize - availableSize

                val dailyData = (KanoUtils.getTodayDataUsage(context_app))

                //外部存储
                val ex_storage_info = KanoUtils.getRemovableStorageInfo(context_app)
                var ex_storage_total_size = ex_storage_info?.totalBytes ?: 0
                var ex_storage_avalible_size = ex_storage_info?.availableBytes ?: 0
                var ex_storage_used_size = ex_storage_total_size - ex_storage_avalible_size


                Log.d("kano_ZTE_LOG", "日用流量：$dailyData")
                Log.d("kano_ZTE_LOG", "内部存储：$usedSize/$totalSize")
                Log.d(
                    "kano_ZTE_LOG",
                    "外部存储：${(ex_storage_info?.availableBytes ?: 0)}/${(ex_storage_info?.totalBytes ?: 0)}"
                )

                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"daily_data":$dailyData,"internal_available_storage":$availableSize,"internal_used_storage":$usedSize,"internal_total_storage":$totalSize,"external_total_storage":${(ex_storage_info?.totalBytes ?: 0)},"external_used_storage":$ex_storage_used_size,"external_available_storage":${(ex_storage_info?.availableBytes ?: 0)}}""".trimIndent()
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "获取型号与电量信息出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"获取型号与电量信息出错"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //检查更新
        if (method == "GET" && uri == "/check_update") {
            return try {
                val path = "/UFI-TOOLS-UPDATE"
                val download_url = "https://pan.kanokano.cn/d${path}/"
                //changelog
                val changelog_url = "https://pan.kanokano.cn/d${path}/changelog.txt"
                val changelog = KanoRequest.getTextFromUrl(changelog_url)
                val json =
                    """{"path":"$path","password":"","page":1,"per_page":0,"refresh":false}""".trimIndent()
                val res = KanoRequest.postJson(
                    "https://pan.kanokano.cn/api/fs/list", json
                )
                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"base_uri":"${download_url}","alist_res":${res.body?.string()},"changelog":"${changelog?.replace(Regex("\r?\n"), "<br>")}"}""".trimIndent()
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "请求出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR, "application/json", """{"error":"请求出错"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //从URL下载apk
        if (method == "POST" && uri == "/download_apk") {
            return try {
                val map = HashMap<String, String>()
                session?.parseBody(map)
                val body = map["postData"] ?: throw Exception("postData is null")
                val json = JSONObject(body)

                val apk_url = json.optString("apk_url", "").trim()
                if (apk_url.isEmpty()) {
                    throw Exception("请提供apk_url")
                }

                Log.d("kano_ZTE_LOG", "接收到apk_url=$apk_url")

                synchronized(this) {
                    if (downloadInProgress && apk_url == currentDownloadingUrl) {
                        Log.d("kano_ZTE_LOG", "已在下载该 APK，忽略重复请求")
                    } else {
                        downloadInProgress = true
                        download_percent = 0
                        downloadResultPath = null
                        downloadError = null
                        currentDownloadingUrl = apk_url

                        val outputFile =
                            File(context_app.getExternalFilesDir(null), "downloaded_app.apk")
                        if (outputFile.exists()) outputFile.delete()

                        thread {
                            try {
                                val path =
                                    KanoRequest.downloadFile(apk_url, outputFile) { percent ->
                                        download_percent = percent
                                    }
                                if (path != null) {
                                    downloadResultPath = path
                                    Log.d("kano_ZTE_LOG", "下载完成：$path")
                                } else {
                                    downloadError = "下载失败"
                                    Log.d("kano_ZTE_LOG", "下载失败")
                                }
                            } catch (e: Exception) {
                                downloadError = e.message ?: "未知错误"
                                Log.d("kano_ZTE_LOG", "下载异常：${e.message}")
                            } finally {
                                downloadInProgress = false
                            }
                        }
                    }
                }

                val response = newFixedLengthResponse(
                    Response.Status.OK, "application/json", """{"result":"download_started"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "执行 /install_apk 出错：${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"${e.message}"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

        //下载进度
        if (method == "GET" && uri == "/download_apk_status") {
            val status = when {
                downloadInProgress -> "downloading"
                downloadError != null -> "error"
                downloadResultPath != null -> "done"
                else -> "idle"
            }
            val response = newFixedLengthResponse(
                Response.Status.OK, "application/json", """{
                "status":"$status",
                "percent":$download_percent,
                "error":"${downloadError ?: ""}"
                }""".trimIndent()
            )
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        //安装下载好的APK
        if (method == "POST" && uri == "/install_apk") {
            val pipedInput = PipedInputStream()
            val pipedOutput = PipedOutputStream(pipedInput)
            if (downloadResultPath != null) {
                //安装APK
                // 使用协程处理耗时任务
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d("kano_ZTE_LOG", "开始安装apk，位置：$downloadResultPath")
                    val writer = OutputStreamWriter(pipedOutput, Charsets.UTF_8)
                    try {
                        //复制依赖
                        val outFile_adb = KanoUtils.copyFileToFilesDir(context_app,"shell/adb") ?: throw Exception("复制adb 到filesDir失败")

                        outFile_adb.setExecutable(true)

                        //复制APK到SD卡根目录
                        val adb_command_copy =
                            "${outFile_adb.absolutePath} -s localhost shell sh -c 'cp ${downloadResultPath} /sdcard/ufi_tools_latest.apk'"
                        val adb_result_copy = runShellCommand(adb_command_copy, context_app)
                        Log.d("kano_ZTE_LOG", "adb_执行命令：$adb_command_copy")
                        Log.d("kano_ZTE_LOG", "adb_result：$adb_result_copy")

                        //创建sh文件
                        val sh_text = """#!/system/bin/sh
                        pm install -r /sdcard/ufi_tools_latest.apk >> /sdcard/ufi_tools_update.log
                        am start -n com.minikano.f50_sms/.MainActivity  >> /sdcard/ufi_tools_update.log
                        echo "done"""".trimIndent()

                        //复制sh到SD卡根目录
                        val file =
                            ShellKano.createShellScript(context_app, "ufi_tools_update.sh", sh_text)
                        val shAbsolutePath = file.absolutePath
                        Log.d("kano_ZTE_LOG", "Script created at: $shAbsolutePath")
                        val adb_command_copy_sh =
                            "${outFile_adb.absolutePath} -s localhost shell sh -c 'cp ${shAbsolutePath} /sdcard/ufi_tools_update.sh'"
                        val adb_result_copy_sh = runShellCommand(adb_command_copy_sh, context_app)
                        Log.d("kano_ZTE_LOG", "copy_sh执行命令：$adb_command_copy_sh")
                        Log.d("kano_ZTE_LOG", "copy_sh_result：$adb_result_copy_sh")

                        //模拟操作
                        fun click_stage() {
                            //打开工程模式活动
                            var Eng_result:Any? = null
                            // 唤醒屏幕
                            runShellCommand("${outFile_adb.absolutePath} -s localhost shell input keyevent KEYCODE_WAKEUP", context_app)
                            Thread.sleep(10)
                            // 解锁
                            runShellCommand("${outFile_adb.absolutePath} -s localhost shell input keyevent 82", context_app)
                            Thread.sleep(10)
                            // 点击一下，防止系统卡住
                            runShellCommand("${outFile_adb.absolutePath} -s localhost shell input tap 0 0", context_app)
                            Thread.sleep(10)
                            repeat(5) {
                                Eng_result = runShellCommand(
                                "${outFile_adb.absolutePath} -s localhost shell am start -n com.sprd.engineermode/.EngineerModeActivity",
                                context_app)
                                Log.d("kano_ZTE_LOG", "工程模式打开结果：$Eng_result")
                            }
                            if(Eng_result == null) {
                                throw Exception("工程模式活动打开失败")
                            }
                            Thread.sleep(400)
                            val res_debug_log_btn = ShellKano.parseUiDumpAndClick(
                                "DEBUG&LOG", outFile_adb.absolutePath, context_app
                            )
                            if (res_debug_log_btn == -1) throw Exception("点击 DEBUG&LOG 失败")
                            if (res_debug_log_btn == 0) {
                                val res = ShellKano.parseUiDumpAndClick(
                                    "Adb shell", outFile_adb.absolutePath, context_app
                                )
                                if (res == -1) throw Exception("点击 Adb Shell 按钮失败")
                            }
                        }

                        fun tryClickStage(maxRetry: Int = 2) {
                            var retry = 0
                            while (retry <= maxRetry) {
                                try {
                                    click_stage()
                                    return // 成功则直接退出
                                } catch (e: Exception) {
                                    Log.w("kano_ZTE_LOG", "click_stage 执行失败，尝试第 ${retry + 1} 次，错误：${e.message}")
                                    // 退回操作
                                    repeat(10) {
                                        runShellCommand(
                                            "${outFile_adb.absolutePath} -s localhost shell input keyevent KEYCODE_BACK",
                                            context_app
                                        )
                                    }
                                    Thread.sleep(1000)
                                    retry++
                                }
                            }
                            throw Exception("click_stage 多次重试失败")
                        }

                        tryClickStage()

                        //输入指令，点击发送
                        var jsonResult = """{"result":"success"}"""
                        try {
                            val escapedCommand =
                                "sh /sdcard/ufi_tools_update.sh".replace(
                                    "\"", "\\\""
                                )
                            fillInputAndSend(
                                escapedCommand, outFile_adb.absolutePath, context_app, "", listOf("START","开始"),
                                needBack = false,
                                useClipBoard = true
                            )
                        } catch (e: Exception) {
                            jsonResult = """{"error":"adb安装apk执行错误：${e.message}"}"""
                        }
                        //清理临时文件
                        writer.write(jsonResult)
                    } catch (e: Exception) {
                        writer.write("""{"error":"adb安装apk执行错误：${e.message}"}""")
                    } finally {
                        writer.flush()
                        pipedOutput.close()
                    }
                }
            }

            val response = newChunkedResponse(
                Response.Status.OK, "application/json", pipedInput
            )
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        //ADB初始化状态
        if (method == "GET" && uri == "/adb_alive"){
            val response = newFixedLengthResponse(
                Response.Status.OK, "application/json", """{"result":"${ADBService.adbIsReady}"}""".trimIndent()
            )
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        //执行shell脚本
        if (method == "GET" && uri == "/one_click_shell"){
            // 创建一个 Piped 流来异步返回数据
            val pipedInput = PipedInputStream()
            val pipedOutput = PipedOutputStream(pipedInput)

            // 使用协程处理耗时任务
            CoroutineScope(Dispatchers.IO).launch {
                val writer = OutputStreamWriter(pipedOutput, Charsets.UTF_8)
                try {
                    //复制依赖
                    val outFile_adb = KanoUtils.copyFileToFilesDir(context_app,"shell/adb") ?: throw Exception("复制adb 到filesDir失败")

                    outFile_adb.setExecutable(true)

                    fun click_stage1() {
                        var Eng_result:Any? = null
                        // 唤醒屏幕
                        runShellCommand("${outFile_adb.absolutePath} -s localhost shell input keyevent KEYCODE_WAKEUP", context_app)
                        Thread.sleep(10)
                        // 解锁
                        runShellCommand("${outFile_adb.absolutePath} -s localhost shell input keyevent 82", context_app)
                        Thread.sleep(10)
                        // 点击一下，防止系统卡住
                        runShellCommand("${outFile_adb.absolutePath} -s localhost shell input tap 0 0", context_app)
                        Thread.sleep(10)
                        repeat(10) {
                            Eng_result = runShellCommand(
                                "${outFile_adb.absolutePath} -s localhost shell am start -n com.sprd.engineermode/.EngineerModeActivity",
                                context_app
                            )
                            Log.d("kano_ZTE_LOG", "工程模式打开结果：$Eng_result")
                        }
                        if(Eng_result == null) {
                            throw Exception("工程模式活动打开失败")
                        }
                        Thread.sleep(400)
                        val res_debug_log_btn = ShellKano.parseUiDumpAndClick(
                            "DEBUG&LOG", outFile_adb.absolutePath, context_app
                        )
                        if (res_debug_log_btn == -1) throw Exception("点击 DEBUG&LOG 失败")
                        if (res_debug_log_btn == 0) {
                            val res = ShellKano.parseUiDumpAndClick(
                                "Adb shell", outFile_adb.absolutePath, context_app
                            )
                            if (res == -1) throw Exception("点击 Adb Shell 按钮失败")
                        }
                    }

                    fun tryClickStage1(maxRetry: Int = 2) {
                        var retry = 0
                        while (retry <= maxRetry) {
                            try {
                                click_stage1()
                                return // 成功则直接退出
                            } catch (e: Exception) {
                                Log.w("kano_ZTE_LOG", "click_stage1 执行失败，尝试第 ${retry + 1} 次，错误：${e.message}")
                                // 退回操作
                                repeat(10) {
                                    runShellCommand(
                                        "${outFile_adb.absolutePath} -s localhost shell input keyevent KEYCODE_BACK",
                                        context_app
                                    )
                                }
                                Thread.sleep(1000)
                                retry++
                            }
                        }
                        throw Exception("click_stage1 多次重试失败")
                    }

                    tryClickStage1()

                    //输入指令，点击发送
                    var jsonResult = """{"result":"执行成功"}"""
                    try {
                        val escapedCommand =
                            "sh /sdcard/one_click_shell.sh".replace(
                                "\"", "\\\""
                            )
                        fillInputAndSend(
                            escapedCommand, outFile_adb.absolutePath, context_app, "", listOf("START","开始"), useClipBoard = true
                        )
                    } catch (e: Exception) {
                        jsonResult = """{"result":"执行失败"}"""
                    }
                    writer.write(jsonResult)
                } catch (e: Exception) {
                    writer.write("""{"error":"one_click_shell执行错误：${e.message}"}""")
                } finally {
                    writer.flush()
                    pipedOutput.close()
                }
            }

            val response = newChunkedResponse(
                Response.Status.OK, "application/json", pipedInput
            )
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        // 静态文件逻辑
        if (!session?.uri.orEmpty().startsWith("/api")) {
            return serveStaticFile(session?.uri ?: "/")
        }

        // 获取查询参数
        val queryString = session?.queryParameterString
        val fullUrl = if (queryString.isNullOrEmpty()) {
            "$targetServer$uri"
        } else {
            "$targetServer$uri?$queryString"
        }

        // 处理 OPTIONS 请求
        if (method == "OPTIONS") {
            val response = newFixedLengthResponse(Response.Status.OK, "text/plain", "")
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With")
            return response
        }

        Log.d("kano_ZTE_LOG", fullUrl)

        // 构造目标 URL
        return try {
            val url = URL(fullUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            // 复制请求头
            session?.headers?.forEach { (key, value) ->
                if (!key.equals("host", ignoreCase = true)) { // 跳过 Host 头，避免影响目标服务器
                    conn.setRequestProperty(key, value)
                }
            }
            conn.setRequestProperty("Referer", targetServer) // 添加 Referer 头

            // 处理 POST 请求体
            if (method == "POST" || method == "PUT") {
                val contentLength = session?.headers?.get("content-length")?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    // 手动读取请求体
                    val inputStream = session?.inputStream
                    val requestBody = ByteArray(contentLength)
                    if (inputStream != null) {
                        inputStream.read(requestBody)
                    }

                    // 将请求体转换为字符串
                    val requestBodyStr = String(requestBody, Charsets.UTF_8)
                    Log.d("kano_ZTE_LOG", "Request Length: ${requestBodyStr.length}")
                    Log.d("kano_ZTE_LOG", "Request Body: $requestBodyStr")

                    // 解析 URL 编码格式的请求体
                    val params = KanoUtils.parseUrlEncoded(requestBodyStr)
                    Log.d("kano_ZTE_LOG", "Parsed Body: $params")

                    // 发送请求体到目标服务器
                    conn.doOutput = true
                    conn.setRequestProperty(
                        "Content-Length", requestBodyStr.toByteArray().size.toString()
                    )
                    conn.outputStream.use { it.write(requestBodyStr.toByteArray()) }
                }
            }

            conn.connect()

            val responseCode = conn.responseCode
            val responseStream: InputStream = if (responseCode in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream
            }

            val response = newFixedLengthResponse(
                Response.Status.lookup(responseCode),
                conn.contentType ?: "text/plain",
                responseStream,
                conn.contentLength.toLong()
            )

            // 将目标服务器的所有响应头转发给客户端
            conn.headerFields.forEach { (key, value) ->
                if (key != null && value != null && key.equals("Set-Cookie", ignoreCase = true)) {
                    value.forEach { cookie ->
                        response.addHeader("kano-cookie", cookie)  // 转发 Set-Cookie
                    }
                }
            }

            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With")

            response
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR, "text/plain", "Proxy error: ${e.message}"
            )
        }
    }

    // 静态文件处理逻辑
    // 添加一个变量保存 context 的 assets
    private val assetManager = context.assets
    private val context_app = context

    fun getStatusCode(urlStr: String): Int {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connect()
            connection.responseCode // 返回状态码
        } catch (e: Exception) {
            e.printStackTrace()
            -1 // 表示请求失败
        } finally {
            connection.disconnect()
        }
    }

    private fun serveStaticFile(uri: String): Response {
        val path = if (uri == "/") "index.html" else uri.removePrefix("/")
        val assetPath = path

        return try {
            val inputStream = assetManager.open(assetPath)
            val mime = getMimeTypeForFile(path)
            newChunkedResponse(Response.Status.OK, mime, inputStream)
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "404 Not Found: ${e.message}"
            )
        }
    }

    data class MyStorageInfo(
        val path: String, val totalBytes: Long, val availableBytes: Long
    )

}