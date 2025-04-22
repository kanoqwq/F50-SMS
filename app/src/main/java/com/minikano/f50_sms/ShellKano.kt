package com.minikano.f50_sms

import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import org.w3c.dom.Document
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import javax.xml.parsers.DocumentBuilderFactory

class ShellKano {
    companion object {
        fun runShellCommand(command: String?): String? {
            val output = StringBuilder()
            try {
                val process = Runtime.getRuntime().exec(command)
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
                val env = arrayOf("HOME=${context.cacheDir.absolutePath}")

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
        fun parseUiDumpAndClick(targetText: String,adbPath:String,context: Context): Number {
            val cacheFile = getUiDoc(adbPath,context)

            val doc = cacheFile
            Log.d("kano_ZTE_LOG","doc 读取 结果：${doc.getElementsByTagName("node")}")

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
                    val result = runShellCommand("$adbPath shell input tap $tapX $tapY",context)?:throw Exception("执行 input tap 失败")
                    Log.d("kano_ZTE_LOG","input tap 点击 坐标：$tapX,$tapY 结果：${result} ")
                    return 0
                }else if(text.contains("AT Command:")){
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
            btnName:String
        ): String {
            val doc = getUiDoc(adbPath,context)
            val nodes = doc.getElementsByTagName("node")

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

                    repeat(3){
                        runShellCommand("$adbPath shell input tap $tapX $tapY", context)
                        Log.d("kano_ZTE_LOG", "点击输入框坐标：$tapX,$tapY")
                    }

                    Thread.sleep(200) // 稍等软键盘弹出

                    // 输入文本
                    val escapedInput = inputText.replace(" ", "%s")
                    runShellCommand("$adbPath shell input text \"$escapedInput\"", context)
                    Log.d("kano_ZTE_LOG", "输入文本：$inputText")
                    inputClicked = true
                    if(escapedInput.length>20){
                        Thread.sleep(500) // 稍等输入完毕
                    }
                    break
                }
            }

            if (!inputClicked) throw Exception("未找到 EditText 输入框")

            //fix：输入框输入完成后ui界面可能会变动（比如按钮位置会被挤下去）需要重新获取UI
            val nodes_after = getUiDoc(adbPath,context).getElementsByTagName("node")
            //找到按钮点击
            for (i in 0 until nodes_after.length) {
                val node = nodes_after.item(i)
                val attrs = node.attributes
                val text = attrs.getNamedItem("text")?.nodeValue ?: ""
                val bounds = attrs.getNamedItem("bounds")?.nodeValue ?: continue

                if (text.equals(btnName, ignoreCase = true)) {
                    val regex = Regex("""\[(\d+),(\d+)\]\[(\d+),(\d+)\]""")
                    val match = regex.find(bounds) ?: continue
                    val (x1, y1, x2, y2) = match.destructured
                    val tapX = (x1.toInt() + x2.toInt()) / 2
                    val tapY = (y1.toInt() + y2.toInt()) / 2
                    runShellCommand("$adbPath shell input tap $tapX $tapY", context)
                    Log.d("kano_ZTE_LOG", "点击 $btnName 坐标：$tapX,$tapY")
                    //继续检测result
                    if(resId != "") {
                        val res = getTextFromUIByResourceId(resId, adbPath, context)
                        Thread.sleep(50) // 稍等
                        //返回就完事了
                        repeat(10) {
                            runShellCommand("$adbPath shell input keyevent KEYCODE_BACK", context)
                        }
                        return res[0]
                    }else{
                        Thread.sleep(50) // 稍等输入完毕
                        //返回就完事了
                        repeat(10) {
                            runShellCommand("$adbPath shell input keyevent KEYCODE_BACK", context)
                        }
                        return ""
                    }
                }
            }

            throw Exception("未找到 $btnName 按钮")
        }

        private fun getTextFromUIByResourceId(resId:String,adbPath: String, context: Context): List<String> {
            val doc = getUiDoc(adbPath,context)
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

            Log.d("kano_ZTE_LOG", "根据：$resId 共找到${resultTexts.size}条 result_text 文本：$resultTexts")
            return resultTexts
        }

        //获取UI 到app cache
        private fun getUIFile(adbPath: String,context: Context):File{
            if (adbPath.isEmpty()) throw Exception("需要 adbPath")

            // 清除旧的 UI 文件
            runShellCommand("$adbPath shell rm /sdcard/kano_ui.xml", context)
            Thread.sleep(100)

            // dump 当前界面
            runShellCommand("$adbPath shell uiautomator dump /sdcard/kano_ui.xml", context)
                ?: throw Exception("uiautomator dump 失败")

            // pull 到本地
            val cacheFile = File(context.cacheDir, "kano_ui.xml")
            val pullResult = runShellCommand(
                "$adbPath pull /sdcard/kano_ui.xml ${cacheFile.absolutePath}",
                context
            ) ?: throw Exception("pull kano_ui.xml 失败")
            Log.d("kano_ZTE_LOG", "adb pull 执行结果: $pullResult")

            if (!cacheFile.exists()) throw Exception("kano_ui.xml 文件不存在")
            return cacheFile
        }

        //获取UI
        private fun getUiDoc(adbPath: String, context: Context): Document {
            if (adbPath.isEmpty()) throw Exception("需要 adbPath")

            // 清除旧的 XML
            runShellCommand("$adbPath shell rm /sdcard/kano_ui.xml", context)
            Thread.sleep(100)

            // dump 当前 UI
            runShellCommand("$adbPath shell uiautomator dump /sdcard/kano_ui.xml", context)
                ?: throw Exception("uiautomator dump 失败")

            // 使用 cat 读取文件内容（无需 pull 到本地）
            val xmlContent = runShellCommand("$adbPath shell cat /sdcard/kano_ui.xml", context)
                ?: throw Exception("cat kano_ui.xml 失败")

            // 解析 XML 字符串为 Document
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val inputStream = xmlContent.byteInputStream()
            return builder.parse(inputStream)
        }


        fun executeShellFromAssetsSubfolderWithArgs(
            context: Context,
            assetSubPath: String,
            vararg args: String
        ): String? {
            return try {
                // 复制文件到 cache 目录
                val assetManager = context.assets
                val inputStream = assetManager.open(assetSubPath)
                val fileName = File(assetSubPath).name
                val outFile = File(context.cacheDir, fileName)

                inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 设置可执行权限
                outFile.setExecutable(true)

                // 拼接命令
                val command = ArrayList<String>().apply {
                    add(outFile.absolutePath)
                    addAll(args)
                }

                // 设置 HOME 环境变量
                val env = arrayOf("HOME=${context.cacheDir.absolutePath}")

                // 构建 ProcessBuilder
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true) // 合并错误输出
                    .apply {
                        environment().put("HOME", context.cacheDir.absolutePath)
                    }
                    .start()

                // 读取输出内容
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

//                Log.d("kano_ZTE_LOG", "执行命令：${command.joinToString(" ")}")
//                Log.d("kano_ZTE_LOG", "命令输出：$output")

                output
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "executeShellFromAssetsSubfolderWithArgs 执行出错：${e.message}")
                e.printStackTrace()
                null
            }
        }

        fun executeShellFromAssetsSubfolder(context: Context, assetSubPath: String): String? {
            try {
                val assetManager = context.assets

                // 从 assets/shell/ 复制，比如 assetSubPath = "shell/test.sh"
                val inputStream = assetManager.open(assetSubPath)
                val outFile = File(context.cacheDir, "temp_script.sh")

                inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 加上执行权限（可能需要 root）
                outFile.setExecutable(true)

                // 执行脚本
                val process = Runtime.getRuntime().exec(outFile.absolutePath)

                // 读取输出
                val reader = process.inputStream.bufferedReader()
                val output = reader.readText()

                process.waitFor()

                return output
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "adb执行出错：${e.message}")
                e.printStackTrace()
            }

            return null
        }
    }
}