package com.minikano.f50_sms

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.StatFs
import com.minikano.f50_sms.WebServer.MyStorageInfo
import java.util.Calendar
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class KanoUtils {
    companion object {

        //获取电池电量
        fun getBatteryPercentage(context: Context): Int {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter) ?: return -1

            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            return ((level / scale.toFloat()) * 100).toInt()
        }

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
                            path = path, totalBytes = total, availableBytes = available
                        )
                    }
                }
            }
            return null
        }

        //不建议使用
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

         fun getStartOfDayMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
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
                val summary = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_MOBILE, null, startTime, endTime
                )
                totalBytes = summary.rxBytes + summary.txBytes
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return totalBytes
        }

        // 解析 URL 编码的请求体
         fun parseUrlEncoded(data: String): Map<String, String> {
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
         fun parseMeminfo(meminfo: String): Float {
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

         fun parseCpuStat(raw: String): Pair<Long, Long>? {
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

        fun getChunkCount(param: String?): Int {
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

        fun copyToClipboard(context: Context, label: String, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
        }

        fun copyFileToFilesDir(context: Context, path: String, skipIfExists: Boolean = true): File? {
            val assetManager = context.assets
            val fileName = File(path).name
            val outFile = File(context.filesDir, fileName)

            // 如果是追加模式且目标文件已存在，则直接返回该文件，避免干扰可执行文件的运行
            if (skipIfExists && outFile.exists()) {
                Log.d("kano_ZTE_LOG", "文件已存在，跳过复制：${outFile.absolutePath}")
                return outFile
            }

            val input = try {
                assetManager.open(path)
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "assets 中不存在文件: $path")
                return null
            }

            return try {
                Log.d("kano_ZTE_LOG", "开始复制 $fileName 到 ${context.filesDir}（skipIfExists？：$skipIfExists）")
                input.use { ins ->
                    FileOutputStream(outFile, skipIfExists).use { out ->
                        ins.copyTo(out)
                    }
                }
                Log.d("kano_ZTE_LOG", "复制 $fileName 成功 -> ${outFile.absolutePath}")
                outFile
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "复制 $fileName 失败: ${e.message}")
                null
            }
        }

        fun parseShellArgs(command: String): List<String> {
            val matcher = Regex("""(".*?"|\S+)""")
            return matcher.findAll(command).map {
                it.value.trim('"') // 去除引号
            }.toList()
        }

        fun adaptIPChange(context: Context,userTouched: Boolean = false, onIpChanged: ((String) -> Unit)? = null) {
            val prefs = context.getSharedPreferences("kano_ZTE_store", Context.MODE_PRIVATE)
            val ip_add = prefs.getString("gateway_ip", null)
            val need_auto_ip = prefs.getString("auto_ip_enabled", true.toString())
            val currentIp = IPManager.getHotspotGatewayIp("8080")

            if ((ip_add != null && need_auto_ip == "true") || userTouched) {
                Log.d("kano_ZTE_LOG", "自动检测IP网关:$currentIp")
                if(currentIp == null){
                    Log.d("kano_ZTE_LOG", "自动检测IP网关失败")
                    Toast.makeText(context, "自动检测IP网关失败...", Toast.LENGTH_SHORT).show()
                    return
                }
                if ((currentIp != ip_add) || userTouched) {
                    if(userTouched){
                        Log.d("kano_ZTE_LOG", "用户点击，自动检测IP网关")
                        Toast.makeText(context, "自动检测IP网关~", Toast.LENGTH_SHORT).show()
                    }else{
                        Log.d("kano_ZTE_LOG", "检测到本地IP网关变动，自动修改IP网关为:$currentIp")
                        Toast.makeText(context, "检测到本地IP网关变动，自动修改IP网关为:$currentIp", Toast.LENGTH_SHORT).show()
                    }
                    prefs.edit().putString("gateway_ip", currentIp).apply()
                    if (currentIp != null) {
                        onIpChanged?.invoke(currentIp)
                    } // 通知 Compose 更新 UI
                }
            }else if(need_auto_ip == "true"){
                //说明可能是第一次启动
                prefs.edit().putString("gateway_ip", currentIp).apply()
                Log.d("kano_ZTE_LOG", "可能是第一次启动，自动修改IP网关为:$currentIp")
            }
        }

        fun copyAssetToExternalStorage(context: Context, assetPath: String, skipIfExists: Boolean = false): File? {
            val fileName = File(assetPath).name
            val outFile = File(context.getExternalFilesDir(null), fileName)

            // 如果是追加模式且目标文件已存在，则直接返回该文件，避免干扰可执行文件的运行
            if (skipIfExists && outFile.exists()) {
                Log.d("kano_ZTE_LOG", "外部文件已存在，跳过复制：${outFile.absolutePath}")
                return outFile
            }

            val input = try {
                context.assets.open(assetPath)
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "assets 中不存在文件: $assetPath")
                return null
            }

            return try {
                Log.d("kano_ZTE_LOG", "开始复制 $fileName 到外部存储目录（skipIfExists?：$skipIfExists）")
                input.use { ins ->
                    FileOutputStream(outFile, skipIfExists).use { out ->
                        ins.copyTo(out)
                    }
                }
                Log.d("kano_ZTE_LOG", "复制成功 -> ${outFile.absolutePath}")
                outFile
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "复制失败: ${e.message}")
                null
            }
        }
    }
}