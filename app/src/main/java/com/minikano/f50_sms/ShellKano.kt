package com.minikano.f50_sms

import android.content.ComponentCallbacks
import android.content.Context
import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class ShellKano {
    companion object {

        fun runShellCommand(command: String?, escaped: Boolean = false): String? {
            val output = StringBuilder()
            try {
                var process = Runtime.getRuntime().exec(command)
                if (escaped) {
                    process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                }
                val reader = BufferedReader(
                    InputStreamReader(process.inputStream)
                )

                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    output.append(line).append("\n")
                }

                reader.close()
                process.waitFor()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return null
            }

            return output.toString().trim { it <= ' ' }
        }

        fun runShellCommand(command: String?, context: Context): String? {
            val output = StringBuilder()
            try {
                // 设置 HOME 环境变量
                val env = arrayOf("HOME=${context.filesDir.absolutePath}")

                // 启动进程（传入环境变量）
                val process = Runtime.getRuntime().exec(command, env)

                val reader = BufferedReader(
                    InputStreamReader(process.inputStream)
                )
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                reader.close()
                process.waitFor()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return null
            }

            return output.toString().trim { it <= ' ' }
        }

        /**
         * adb寻找ui控件然后点击
         * @return 1 0 -1
         * 1表示已经在AT发送界面了
         * 0表示执行点击成功
         * -1表示执行失败 没有找到任何文本
         */
        fun parseUiDumpAndClick(targetText: String, adbPath: String, context: Context): Number {
            val cacheFile = getUiDoc(adbPath, context)

            val doc = cacheFile
            Log.d("kano_ZTE_LOG", "doc 读取 结果：${doc.getElementsByTagName("node")}")

            //tap逻辑
            val nodes = doc.getElementsByTagName("node")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val attrs = node.attributes
                val text = attrs.getNamedItem("text")?.nodeValue ?: ""
                Log.d("kano_ZTE_LOG", "Node text: '$text'")
                if (text.contains(targetText)) {
                    val bounds = attrs.getNamedItem("bounds")?.nodeValue ?: continue
                    val regex = Regex("""\[(\d+),(\d+)\]\[(\d+),(\d+)\]""")
                    val match = regex.find(bounds) ?: continue
                    val (x1, y1, x2, y2) = match.destructured
                    val tapX = (x1.toInt() + x2.toInt()) / 2
                    val tapY = (y1.toInt() + y2.toInt()) / 2
                    val result = runShellCommand("$adbPath -s localhost shell input tap $tapX $tapY", context)
                        ?: throw Exception("执行 input tap 失败")
                    Log.d("kano_ZTE_LOG", "input tap 点击 坐标：$tapX,$tapY 结果：${result} ")
                    return 0
                } else if (text.contains("AT Command:")) {
                    //说明已经在AT页面了
                    return 1
                }
            }
            return -1
        }

        /**
         * 填写input内容然后发送AT指令
         */
        fun fillInputAndSend(
            inputText: String,
            adbPath: String,
            context: Context,
            resId: String,
            btnName: List<String>,
            needBack: Boolean = true,
            useClipBoard:Boolean = false
        ): String {
            val doc = getUiDoc(adbPath, context)
            val nodes = doc.getElementsByTagName("node")
            val escapedInput = inputText.replace(" ", "%s")
            //复制文本到剪贴板
            KanoUtils.copyToClipboard(context,"sambaCommand",inputText)

            // 寻找输入框
            var inputClicked = false
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val attrs = node.attributes
                val clazz = attrs.getNamedItem("class")?.nodeValue ?: ""
                val bounds = attrs.getNamedItem("bounds")?.nodeValue ?: continue

                if (clazz == "android.widget.EditText") {
                    val regex = Regex("""\[(\d+),(\d+)\]\[(\d+),(\d+)\]""")
                    val match = regex.find(bounds) ?: continue
                    val (x1, y1, x2, y2) = match.destructured
                    val tapX = (x1.toInt() + x2.toInt()) / 2
                    val tapY = (y1.toInt() + y2.toInt()) / 2

                    repeat(3) {
                        runShellCommand("$adbPath -s localhost shell input tap $tapX $tapY", context)
                        Log.d("kano_ZTE_LOG", "点击输入框坐标：$tapX,$tapY")
                    }

                    // 输入文本
                    if(!useClipBoard) {
                        Thread.sleep(200) // 稍等软键盘弹出
                        runShellCommand("$adbPath -s localhost shell input text \"$escapedInput\"", context)
                        Log.d("kano_ZTE_LOG", "输入文本：$inputText")
                        inputClicked = true
                        if (escapedInput.length > 20) {
                            Thread.sleep(500) // 稍等输入完毕
                        }
                        break
                    } else {
                        runShellCommand("$adbPath -s localhost shell input keyevent KEYCODE_PASTE", context)
                        Log.d("kano_ZTE_LOG", "读取剪贴板，输入文本：$inputText")
                        inputClicked = true
                        Thread.sleep(666) // 稍等输入完毕
                        break
                    }
                }
            }

            if (!inputClicked) throw Exception("未找到 EditText 输入框")

            fun getBtnAndClick(nodes_after: NodeList): String? {
                //找到按钮点击
                for (i in 0 until nodes_after.length) {
                    val node = nodes_after.item(i)
                    val attrs = node.attributes
                    val text = attrs.getNamedItem("text")?.nodeValue ?: ""
                    val bounds = attrs.getNamedItem("bounds")?.nodeValue ?: continue

                    if (btnName.any { it.equals(text, ignoreCase = true) }) {
                        val regex = Regex("""\[(\d+),(\d+)\]\[(\d+),(\d+)\]""")
                        val match = regex.find(bounds) ?: continue
                        val (x1, y1, x2, y2) = match.destructured
                        val tapX = (x1.toInt() + x2.toInt()) / 2
                        val tapY = (y1.toInt() + y2.toInt()) / 2
                        runShellCommand("$adbPath -s localhost shell input tap $tapX $tapY", context)
                        Log.d("kano_ZTE_LOG", "点击 ${btnName.joinToString(", ")} 坐标：$tapX,$tapY")
                        //继续检测result
                        if (resId != "") {
                            val res = getTextFromUIByResourceId(resId, adbPath, context)
                            if (needBack) {
                                //返回就完事了
                                Thread.sleep(800) // 稍等输入完毕
                                repeat(10) {
                                    runShellCommand(
                                        "$adbPath -s localhost shell input keyevent KEYCODE_BACK",
                                        context
                                    )
                                }
                            }
                            return res[0]
                        } else {
                            if (needBack) {
                                //返回就完事了
                                Thread.sleep(800) // 稍等输入完毕
                                repeat(10) {
                                    runShellCommand(
                                        "$adbPath -s localhost shell input keyevent KEYCODE_BACK",
                                        context
                                    )
                                }
                            }
                            return ""
                        }
                    }
                }
                return null
            }

            var res:String? = null

            for (i in 0 until 10){
                val nodes_after = getUiDoc(adbPath, context).getElementsByTagName("node")
                var temp = getBtnAndClick(nodes_after)
                if(temp != null){
                    res = temp
                    break
                }
            }

            if(res != null){
                return res as String
            }

            throw Exception("未找到 ${btnName.joinToString(", ")} 按钮")
        }

        fun createShellScript(context: Context, fileName: String, scriptContent: String): File {
            val scriptFile = File(context.getExternalFilesDir(null), fileName)

            try{
                // 如果文件已存在，删除旧文件
                if (scriptFile.exists()) {
                    scriptFile.delete()
                }
            } catch (e:Exception){
                Log.d("kano_ZTE_LOG", "删除脚本出错：${e.message}")
            }

            // 写入内容（writeText 本身就是覆盖写入）
            scriptFile.writeText(scriptContent)

            // 设置执行权限（某些设备需要删除后重新设置权限才生效）
            scriptFile.setExecutable(true)

            return scriptFile
        }

        private fun getTextFromUIByResourceId(
            resId: String,
            adbPath: String,
            context: Context
        ): List<String> {
            val doc = getUiDoc(adbPath, context)
            val nodes = doc.getElementsByTagName("node")

            val resultTexts = mutableListOf<String>()

            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val attrs = node.attributes

                val resourceId = attrs.getNamedItem("resource-id")?.nodeValue ?: continue
                if (resourceId == resId) {
                    val text = attrs.getNamedItem("text")?.nodeValue ?: ""
                    resultTexts.add(text)
                }
            }

            Log.d(
                "kano_ZTE_LOG",
                "根据：$resId 共找到${resultTexts.size}条 result_text 文本：$resultTexts"
            )
            return resultTexts
        }

        //获取UI
        private fun getUiDoc(adbPath: String, context: Context, maxRetry: Int = 3): Document {
            if (adbPath.isEmpty()) throw Exception("需要 adbPath")

            repeat(maxRetry) { attempt ->
                try {

                    // 清除旧的 XML
                    runShellCommand("$adbPath -s localhost shell rm /sdcard/kano_ui.xml", context)
                    Thread.sleep(200)

                    // dump 当前 UI
                    runShellCommand("$adbPath -s localhost shell uiautomator dump /sdcard/kano_ui.xml", context)
                        ?: throw Exception("uiautomator dump 失败")

                    Thread.sleep(300)

                    // cat 读取 XML 内容
                    val xmlContent = runShellCommand("$adbPath -s localhost shell cat /sdcard/kano_ui.xml", context)
                        ?: throw Exception("cat kano_ui.xml 失败")

                    if (!xmlContent.trim().endsWith("</hierarchy>")) {
                        Log.w("kano_ZTE_LOG", "UI XML 不完整，第 ${attempt + 1} 次尝试")
                        Thread.sleep(200)
                        return@repeat
                    }

                    // 转换为 Document
                    val factory = DocumentBuilderFactory.newInstance()
                    val builder = factory.newDocumentBuilder()
                    val inputStream = xmlContent.byteInputStream()
                    return builder.parse(inputStream)
                } catch (e: Exception) {
                    Log.e("kano_ZTE_LOG", "解析 UI XML 失败，第 ${attempt + 1} 次：${e.message}")
                    Thread.sleep(200)
                }
            }

            throw Exception("多次尝试后仍无法获取完整的 UI dump")
        }


        fun killProcessByName(processKeyword: String) {
            try {
                val psProcess = ProcessBuilder("ps").start()
                val output = psProcess.inputStream.bufferedReader().readText()

                val lines = output.lines()
                for (line in lines) {
                    if (line.contains(processKeyword)) {
                        val tokens = line.trim().split(Regex("\\s+"))
                        if (tokens.size > 1) {
                            val pid = tokens[1]
                            Log.w("kano_ZTE_LOG", "匹配到进程: $line，准备 kill -9 $pid")
                            try {
                                ProcessBuilder("kill", "-9", pid).start().waitFor()
                                Log.w("kano_ZTE_LOG", "已 kill -9 $pid")
                            } catch (e: Exception) {
                                Log.e("kano_ZTE_LOG", "kill -9 $pid 失败: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "killProcessByName 执行失败: ${e.message}")
            }
        }

        fun executeShellFromAssetsSubfolderWithArgs(
            context: Context,
            assetSubPath: String,
            vararg args: String,
            timeoutMs: Long = 20000,  // 默认最多等20秒
            onTimeout: (() -> Unit)? = null  // 超时回调
        ): String? {
            return try {
                val assetManager = context.assets
                val inputStream = assetManager.open(assetSubPath)
                val fileName = File(assetSubPath).name
                val outFile = File(context.filesDir, fileName)

                if (!outFile.exists()) {
                    inputStream.use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d("kano_ZTE_LOG", "${outFile} 文件复制完成")
                } else {
                    Log.d("kano_ZTE_LOG", "${outFile} 文件已存在，无需复制")
                }

                outFile.setExecutable(true)

                val command = ArrayList<String>().apply {
                    add(outFile.absolutePath)
                    addAll(args)
                }

                Log.d("kano_ZTE_LOG", "执行命令: ${command.joinToString(" ")}")

                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .apply {
                        environment()["HOME"] = context.filesDir.absolutePath
                    }
                    .start()

                // 启动线程读输出
                val outputBuilder = StringBuilder()
                val readerThread = Thread {
                    try {
                        process.inputStream.bufferedReader().forEachLine {
                            outputBuilder.appendLine(it)
                        }
                    } catch (e: Exception) {
                        Log.w("kano_ZTE_LOG", "读取进程输出异常：${e.message}")
                    }
                }
                readerThread.start()

                // 最多等待 timeoutMs 毫秒
                val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)

                if (!finished) {
                    Log.w("kano_ZTE_LOG", "执行超时，强制销毁进程")
                    process.destroy()

                    // 调用回调
                    onTimeout?.invoke()
                }

                readerThread.join(100) // 最多等 100ms 等输出读完
                outputBuilder.toString().trim()

            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "执行异常: ${e.message}")
                null
            }
        }

        //检测adb存活
        fun ensureAdbAlive(context: Context): Boolean {
            try {
                val adbPath = "shell/adb"

                // 第一次检测
                var result = executeShellFromAssetsSubfolderWithArgs(context, adbPath, "devices")
                Log.d("kano_ZTE_LOG", "adb device 执行状态：$result")

                if (result?.contains("localhost:5555\tdevice") == true) {
                    Log.d("kano_ZTE_LOG", "adb存活，无需启动")
                    return true
                }

                Log.w("kano_ZTE_LOG", "adb无设备或已退出，尝试启动")

                // 重启 ADB server
                executeShellFromAssetsSubfolderWithArgs(context, adbPath, "kill-server")
                Thread.sleep(1000)
                executeShellFromAssetsSubfolderWithArgs(context, adbPath, "connect", "localhost")

                // 等待最多 10 秒，设备变为 "device"
                val maxWaitMs = 10_000
                val interval = 500
                var waited = 0

                while (waited < maxWaitMs) {
                    result = executeShellFromAssetsSubfolderWithArgs(context, adbPath, "devices")
                    Log.d("kano_ZTE_LOG", "等待 ADB 启动中：$result")
                    if (result?.contains("localhost:5555\tdevice") == true) {
                        Log.d("kano_ZTE_LOG", "ADB连接成功")
                        return true
                    }
                    Thread.sleep(interval.toLong())
                    waited += interval
                }

                Log.e("kano_ZTE_LOG", "等待ADB device超时")
                return false
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "检测/启动ADB失败: ${e.message}")
                return false
            }
        }

        fun executeShellFromAssetsSubfolder(context: Context, assetSubPath: String): String? {
            try {
                val assetManager = context.assets

                // 从 assets/shell/ 复制，比如 assetSubPath = "shell/test.sh"
                val inputStream = assetManager.open(assetSubPath)
                val outFile = File(context.filesDir, "temp_script.sh")

                inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }

                outFile.setExecutable(true)

                val process = Runtime.getRuntime().exec(outFile.absolutePath)

                val reader = process.inputStream.bufferedReader()
                val output = reader.readText()

                process.waitFor()

                return output
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "执行出错：${e.message}")
                e.printStackTrace()
            }

            return null
        }
    }
}