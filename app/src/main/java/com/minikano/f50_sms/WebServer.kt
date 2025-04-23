package com.minikano.f50_sms

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.Calendar
import java.util.Random

class WebServer(context: Context, port: Int,gatewayIp: String) : NanoHTTPD(port) {

    private val targetServer = "http://$gatewayIp"  // 目标服务器地址
    private val targetServerIP = gatewayIp  // 目标服务器地址
    private val PREFS_NAME = "kano_ZTE_store"

    private val oneMB = 1024 * 1024
    private val random = SecureRandom()
    private val randomBlock = ByteArray(oneMB).also {
        random.nextBytes(it)
    }
    private fun getChunkCount(param: String?): Int {
        val default = 4
        val max = 1024

        return param?.toIntOrNull()?.let {
            when {
                it <= 0 -> default
                it > max -> max
                else -> it
            }
        } ?: default
    }


    override fun serve(session: IHTTPSession?): Response {
        val method = session?.method.toString()
        val uri = session?.uri?.removePrefix("/api") ?: "/"
        //cpu温度
        if (method == "GET" && uri == "/temp") {
            return try {
                val temp = ShellKano.executeShellFromAssetsSubfolder(context_app,"shell/temp.sh")
                val temp1 =  ShellKano.runShellCommand("cat /sys/class/thermal/thermal_zone1/temp")
                Log.d("kano_ZTE_LOG", "获取CPU温度成功: $temp")
                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"temp":${temp?:temp1}}"""
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
                val stat1 = ShellKano.runShellCommand("cat /proc/stat") ?: throw Exception("stat1没有数据")
                val (total1, idle1) = parseCpuStat(stat1) ?:  throw Exception("parseCpuStat执行失败")
                val stat2 = ShellKano.runShellCommand("cat /proc/stat") ?: throw Exception("stat2没有数据")
                val (total2, idle2) = parseCpuStat(stat2) ?: throw Exception("parseCpuStat执行失败")
                val totalDiff = total2 - total1
                val idleDiff = idle2 - idle1
                val usage = if (totalDiff > 0) (totalDiff - idleDiff).toFloat() / totalDiff else 0f

                Log.d("kano_ZTE_LOG", "CPU 使用率：%.2f%%".format(usage * 100))
                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"cpu":${usage * 100}}"""
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
                val usage = parseMeminfo(info)
                Log.d("kano_ZTE_LOG", "内存使用率：%.2f%%".format(usage * 100))
                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"mem":${usage * 100}}"""
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
                    Response.Status.OK,
                    "application/json",
                    """{"enabled":$ADB_IP_ENABLED}"""
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
                    val AT_command_arr = rawParams["command"] ?: throw Exception("qeury 缺少 command 参数")
                    val AT_command = AT_command_arr[0]
                    Log.d("kano_ZTE_LOG", "AT_command 传入参数：${AT_command}")

                    //复制依赖
                    val assetManager = context_app.assets
                    val inputStream_adb = assetManager.open("shell/adb")
                    val fileName2 = File("shell/adb").name
                    val outFile_adb = File(context_app.cacheDir, fileName2)

                    try {
                        inputStream_adb.use { input ->
                            FileOutputStream(outFile_adb).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }catch(e:Exception){
                        Log.d("kano_ZTE_LOG", "adb文件已存在， 无需复制")
                    }

                    outFile_adb.setExecutable(true)
                    Log.d("kano_ZTE_LOG", "adb-outFile：${outFile_adb.absolutePath}")

                    Log.d("kano_ZTE_LOG", "adbPath：${outFile_adb.absolutePath}")

                    //AT+CGEQOSRDP=1
                    if(!(AT_command.toString()).startsWith("AT+")){
                        throw  Exception("解析失败，AT字符串 需要以 “AT+” 开头")
                    }

                    val adb_command = "${outFile_adb.absolutePath} disconnect"
                    val adb_result = runShellCommand(adb_command,context_app)
                    Log.d("kano_ZTE_LOG", "adb_执行命令：$adb_command")
                    Log.d("kano_ZTE_LOG", "adb_result：$adb_result")

                    Thread.sleep(1000)//小睡一下

                    fun click_stage1 (){
                        //打开工程模式活动
                        repeat(3){
                            val Eng_result = runShellCommand("${outFile_adb.absolutePath} shell am start -n com.sprd.engineermode/.EngineerModeActivity",context_app)?:throw Exception("工程模式活动打开失败")
                            Log.d("kano_ZTE_LOG", "工程模式打开结果：$Eng_result")
                        }

                        Thread.sleep(200)
                        val res_debug_log_btn = ShellKano.parseUiDumpAndClick(
                            "DEBUG&LOG",
                            outFile_adb.absolutePath,
                            context_app
                        )
                        if (res_debug_log_btn == -1) throw Exception("点击 DEBUG&LOG 失败")

                        val res_send_at_cmd = ShellKano.parseUiDumpAndClick(
                            "Send AT Command",
                            outFile_adb.absolutePath,
                            context_app
                        )
                        if (res_send_at_cmd == -1) throw Exception("点击 Send AT Command Tab 失败")
                    }

                    var fallBackTime = 0
                    try{
                        click_stage1()
                    }
                    catch (e:Exception){
                        //返回
                        repeat(10) {
                            runShellCommand("${outFile_adb.absolutePath} shell input keyevent KEYCODE_BACK", context_app)
                        }
                        Thread.sleep(1000)
                        if(fallBackTime++<2) {
                            click_stage1()
                        }
                    }

                    //输入AT指令，点击发送
                    val escapedCommand = AT_command.replace("\"", "\\\"")
                    val result = fillInputAndSend(escapedCommand,outFile_adb.absolutePath,context_app,"com.sprd.engineermode:id/result_text","SEND")

                    val AT_result ="Command result:$result"

                    val cleanedResult = AT_result
                    .substringAfter("Command result:")
                        .lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.isNotEmpty() }
                        ?: "无结果"                          // 去掉首尾空格

                    val jsonResult = """{"result":"$cleanedResult"}"""

                    writer.write(jsonResult)
                } catch (e: Exception) {
                    writer.write("""{"error":"AT指令执行错误：${e.message}"}""")
                } finally {
                    writer.flush()
                    pipedOutput.close()
                }
            }

            val response = newChunkedResponse(
                Response.Status.OK,
                "application/json",
                pipedInput
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
                    val path_command_arr = rawParams["path"] ?: throw Exception("qeury 缺少 path 参数")
                    val path_command = path_command_arr[0]
                    Log.d("kano_ZTE_LOG", "path 传入参数：${path_command}")

                    //复制依赖
                    val assetManager = context_app.assets
                    val inputStream_adb = assetManager.open("shell/adb")
                    val fileName_adb = File("shell/adb").name
                    val outFile_adb = File(context_app.cacheDir, fileName_adb)

                    val smb = assetManager.open("shell/smb.conf")
                    val fileName_smb = File("shell/smb.conf").name
                    val outFile_smb = File(context_app.getExternalFilesDir(null), fileName_smb)

                    //复制smb到外部存储
                    try {
                        smb.use { input ->
                            FileOutputStream(outFile_smb).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.d("kano_ZTE_LOG", "复制到 ${outFile_smb.absolutePath} 成功")
                    } catch (e: Exception) {
                        Log.e("kano_ZTE_LOG", "复制失败：${e.message}")
                    }

                    try {
                        inputStream_adb.use { input ->
                            FileOutputStream(outFile_adb).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.d("kano_ZTE_LOG", "复制到 ${outFile_adb.absolutePath} 成功")
                    }catch(e:Exception){
                        Log.d("kano_ZTE_LOG", "依赖文件已存在， 无需复制")
                    }


                    outFile_adb.setExecutable(true)
                    Log.d("kano_ZTE_LOG", "adb-outFile：${outFile_adb.absolutePath}")
                    Log.d("kano_ZTE_LOG", "adbPath：${outFile_adb.absolutePath}")

                    val adb_command = "${outFile_adb.absolutePath} disconnect"
                    val adb_result = runShellCommand(adb_command,context_app)
                    Log.d("kano_ZTE_LOG", "adb_执行命令：$adb_command")
                    Log.d("kano_ZTE_LOG", "adb_result：$adb_result")

                    Thread.sleep(1000)//小睡一下

                    //复制smb到sdcard
                    val smb_res = runShellCommand("${outFile_adb.absolutePath} shell cp ${outFile_smb.absolutePath} /sdcard/${outFile_smb.name}",context_app)?:throw Exception("工程模式活动打开失败")
                    Log.d("kano_ZTE_LOG", "执行：${outFile_adb.absolutePath} shell cp ${outFile_smb.absolutePath} /sdcard/${outFile_smb.name}：$smb_res")


                    fun click_stage1(){
                        //打开工程模式活动
                        repeat(3){
                            val Eng_result = runShellCommand("${outFile_adb.absolutePath} shell am start -n com.sprd.engineermode/.EngineerModeActivity",context_app)?:throw Exception("工程模式活动打开失败")
                            Log.d("kano_ZTE_LOG", "工程模式打开结果：$Eng_result")
                        }
                        Thread.sleep(200)
                        val res_debug_log_btn = ShellKano.parseUiDumpAndClick("DEBUG&LOG",outFile_adb.absolutePath,context_app)
                        if(res_debug_log_btn == -1) throw Exception("点击 DEBUG&LOG 失败")
                        if(res_debug_log_btn == 0) {
                            val res = ShellKano.parseUiDumpAndClick(
                                "Adb shell",
                                outFile_adb.absolutePath,
                                context_app
                            )
                            if (res == -1) throw Exception("点击 Adb Shell 按钮失败")
                        }
                    }
                    var fallBackTime = 0
                    try{
                        click_stage1()
                    }
                    catch (e:Exception){
                        //返回
                        repeat(10) {
                            runShellCommand("${outFile_adb.absolutePath} shell input keyevent KEYCODE_BACK", context_app)
                        }
                        Thread.sleep(1000)
                        if(fallBackTime++<2) {
                            click_stage1()
                        }
                    }

                    //输入指令，点击发送
                    var jsonResult = """{"result":"执行成功"}"""
                    try {
                        val escapedCommand =
                            "toybox cp /sdcard/${outFile_smb.name} /data/samba/etc/smb.conf".replace(
                                "\"",
                                "\\\""
                            )
                        fillInputAndSend(
                            escapedCommand,
                            outFile_adb.absolutePath,
                            context_app,
                            "",
                            "START"
                        )
                    }catch (e:Exception){
                        jsonResult = """{"result":"执行失败"}"""
                    }
                    writer.write(jsonResult)
                } catch (e: Exception) {
                    writer.write("""{"error":"AT指令执行错误：${e.message}"}""")
                } finally {
                    writer.flush()
                    pipedOutput.close()
                }
            }

            val response = newChunkedResponse(
                Response.Status.OK,
                "application/json",
                pipedInput
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
                if(enabled == true){
                    sharedPrefs.edit()
                        .putString("ADB_IP", host)
                        .putString("ADMIN_PWD",password)
                        .putString("ADB_IP_ENABLED","true")
                        .apply()
                }else{
                    sharedPrefs.edit().remove("ADB_IP").remove("ADMIN_PWD")
                        .putString("ADB_IP_ENABLED","false")
                        .apply()
                }

                Log.d("kano_ZTE_LOG", "保存结果：ADB_IP:${
                    sharedPrefs.getString("ADB_IP", "")
                } ADMIN_PWD:${
                    sharedPrefs.getString("ADMIN_PWD", "")
                }")

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
                val batteryLevel: Int = getBatteryPercentage()// 充电状态
                val packageManager = context_app.packageManager
                val packageName = context_app.packageName
                val versionName = packageManager.getPackageInfo(packageName, 0).versionName

                Log.d("kano_ZTE_LOG", "型号与电量：$model $batteryLevel")

                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"app_ver":"$versionName","model":"$model","battery":"$batteryLevel"}"""
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
                val ckSize = getChunkCount(parms["ckSize"]?.firstOrNull())
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
                headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0, s-maxage=0"
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

                val response = newChunkedResponse(Response.Status.OK, "application/octet-stream", stream)

                headers.forEach { (k, v) -> response.addHeader(k, v) }

                response
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "测速出错： ${e.message}")
                val response = newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    """{"error":"测速出错"}"""
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }
        }

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

        if (method == "GET" && uri == "/storage_and_dailyData") {
            return try {
                //内部存储
                val internalStorage: File = context_app.filesDir
                val statFs = StatFs(internalStorage.absolutePath)
                val totalSize = (statFs.blockSizeLong * statFs.blockCountLong)
                val availableSize =(statFs.blockSizeLong * statFs.availableBlocksLong)
                val usedSize = totalSize - availableSize

                val dailyData = (getTodayDataUsage(context_app))

                //外部存储
                val ex_storage_info = getRemovableStorageInfo(context_app)
                var ex_storage_total_size = ex_storage_info?.totalBytes?:0
                var ex_storage_avalible_size = ex_storage_info?.availableBytes?:0
                var ex_storage_used_size = ex_storage_total_size - ex_storage_avalible_size


                Log.d("kano_ZTE_LOG","日用流量：$dailyData")
                Log.d("kano_ZTE_LOG","内部存储：$usedSize/$totalSize")
                Log.d("kano_ZTE_LOG","外部存储：${(ex_storage_info?.availableBytes?:0)}/${(ex_storage_info?.totalBytes?:0)}")

                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    """{"daily_data":$dailyData,"internal_available_storage":$availableSize,"internal_used_storage":$usedSize,"internal_total_storage":$totalSize,"external_total_storage":${(ex_storage_info?.totalBytes?:0)},"external_used_storage":$ex_storage_used_size,"external_available_storage":${(ex_storage_info?.availableBytes?:0)}}""".trimIndent()
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
                    val params = parseUrlEncoded(requestBodyStr)
                    Log.d("kano_ZTE_LOG", "Parsed Body: $params")

                    // 发送请求体到目标服务器
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Length", requestBodyStr.toByteArray().size.toString())
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
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Proxy error: ${e.message}")
        }
    }

    // 静态文件处理逻辑
    // 添加一个变量保存 context 的 assets
    private val assetManager = context.assets
    private val context_app = context

    //获取电池电量
    private fun getBatteryPercentage(): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context_app.registerReceiver(null, filter) ?: return -1

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        return ((level / scale.toFloat()) * 100).toInt()
    }

    private fun serveStaticFile(uri: String): Response {
        val path = if (uri == "/") "index.html" else uri.removePrefix("/")
        val assetPath = "$path"

        return try {
            val inputStream = assetManager.open(assetPath)
            val mime = getMimeTypeForFile(path)
            newChunkedResponse(Response.Status.OK, mime, inputStream)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found: ${e.message}")
        }
    }

    // 解析 URL 编码的请求体
    private fun parseUrlEncoded(data: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val pairs = data.split("&")

        for (pair in pairs) {
            val keyValue = pair.split("=")
            if (keyValue.size == 2) {
                val key = keyValue[0]
                val value = keyValue[1]
                params[key] = java.net.URLDecoder.decode(value, Charsets.UTF_8.name())  // 解码
            }
        }

        return params
    }

    //获取内存信息
    private fun parseMeminfo(meminfo: String): Float {
        val memMap = mutableMapOf<String, Long>()

        meminfo.lines().forEach { line ->
            val parts = line.split(Regex("\\s+"))
            if (parts.size >= 2) {
                val key = parts[0].removeSuffix(":")
                val value = parts[1].toLongOrNull() ?: return@forEach
                memMap[key] = value
            }
        }

        val total = memMap["MemTotal"] ?: return 0f
        val free = memMap["MemFree"] ?: 0
        val cached = memMap["Cached"] ?: 0
        val buffers = memMap["Buffers"] ?: 0

        val used = total - free - cached - buffers
        return used.toFloat() / total
    }

    private fun parseCpuStat(raw: String): Pair<Long, Long>? {
        val line = raw.lines().firstOrNull { it.startsWith("cpu ") } ?: return null
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 8) return null

        val user = parts[1].toLongOrNull() ?: return null
        val nice = parts[2].toLongOrNull() ?: return null
        val system = parts[3].toLongOrNull() ?: return null
        val idle = parts[4].toLongOrNull() ?: return null
        val iowait = parts[5].toLongOrNull() ?: 0
        val irq = parts[6].toLongOrNull() ?: 0
        val softirq = parts[7].toLongOrNull() ?: 0

        val total = user + nice + system + idle + iowait + irq + softirq
        val idleAll = idle + iowait
        return Pair(total, idleAll)
    }

    fun getTodayDataUsage(
        context: Context,
    ): Long {
        val networkStatsManager =
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        val startTime = getStartOfDayMillis()
        val endTime = System.currentTimeMillis()

        var totalBytes = 0L

        try {
            val summary = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            val bucket = NetworkStats.Bucket()
            summary.getNextBucket(bucket)
            totalBytes = bucket.rxBytes + bucket.txBytes
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return totalBytes
    }

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    data class MyStorageInfo(
        val path: String,
        val totalBytes: Long,
        val availableBytes: Long
    )

    fun getRemovableStorageInfo(context: Context): MyStorageInfo? {
        val dirs = context.getExternalFilesDirs(null)
        for (file in dirs) {
            if (file != null) {
                val path = file.absolutePath

                // 判断不是内置路径
                if (!path.contains("/storage/emulated/0")) {
                    val statFs = StatFs(path)
                    val total = statFs.blockSizeLong * statFs.blockCountLong
                    val available = statFs.blockSizeLong * statFs.availableBlocksLong

                    return MyStorageInfo(
                        path = path,
                        totalBytes = total,
                        availableBytes = available
                    )
                }
            }
        }
        return null
    }

    //超级低能代码，不建议使用
    fun getMaxTemperature(): Int? {
        val temperatures = mutableListOf<Int>()

        // 遍历 zone0 到 zone30
        for (i in 0..25) {
            val zone = "/sys/class/thermal/thermal_zone$i/temp"
            val temp = ShellKano.runShellCommand("cat $zone")

            if (!temp.isNullOrEmpty()) {
                try {
                    temperatures.add(temp.toInt())  // 添加有效的温度值到列表
                } catch (e: NumberFormatException) {
                    // 如果温度值无法解析为整数，可以忽略该值
                    continue
                }
            }
        }

        // 如果没有有效的温度值，返回 null
        if (temperatures.isEmpty()) {
            return null
        }

        // 对温度值排序并取最大值
        return temperatures.sortedDescending().first()
    }
}