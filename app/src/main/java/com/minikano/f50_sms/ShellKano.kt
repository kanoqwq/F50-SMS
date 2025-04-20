package com.minikano.f50_sms

import android.content.Context
import android.util.Log
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
            val cacheFile = getUIFile(adbPath,context)
            val file = File(cacheFile.absolutePath)

            var countDown = 100
            while (true) {
                countDown --
                if(file.exists() || countDown <= 0){
                    Log.d("kano_ZTE_LOG","kano_ui.xml文件已经找到：${file.absolutePath}")
                    break
                }
                else{
                    Thread.sleep(30)
                }
            }

            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
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
            context: Context
        ): String {
            val cacheFile = getUIFile(adbPath,context)
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(cacheFile)
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

                    runShellCommand("$adbPath shell input tap $tapX $tapY", context)
                    Log.d("kano_ZTE_LOG", "点击输入框坐标：$tapX,$tapY")

                    Thread.sleep(200) // 稍等软键盘弹出

                    // 输入文本
                    val escapedInput = inputText.replace(" ", "%s")
                    runShellCommand("$adbPath shell input text \"$escapedInput\"", context)
                    Log.d("kano_ZTE_LOG", "输入文本：$inputText")
                    inputClicked = true
                    break
                }
            }

            if (!inputClicked) throw Exception("未找到 EditText 输入框")

            //找到SEND按钮点击
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val attrs = node.attributes
                val text = attrs.getNamedItem("text")?.nodeValue ?: ""
                val bounds = attrs.getNamedItem("bounds")?.nodeValue ?: continue

                if (text.equals("SEND", ignoreCase = true)) {
                    val regex = Regex("""\[(\d+),(\d+)\]\[(\d+),(\d+)\]""")
                    val match = regex.find(bounds) ?: continue
                    val (x1, y1, x2, y2) = match.destructured
                    val tapX = (x1.toInt() + x2.toInt()) / 2
                    val tapY = (y1.toInt() + y2.toInt()) / 2

                    runShellCommand("$adbPath shell input tap $tapX $tapY", context)
                    Log.d("kano_ZTE_LOG", "点击 SEND 坐标：$tapX,$tapY")

                    //继续检测result
                    val res = getTextFromUIByResourceId(adbPath,context)

                    //返回就完事了
                    repeat(10) {
                        runShellCommand("$adbPath shell input keyevent KEYCODE_BACK", context)
                    }
                    return res[0]
                }
            }

            throw Exception("未找到 SEND 按钮")
        }

        fun getTextFromUIByResourceId(adbPath: String, context: Context): List<String> {
            val cacheFile = getUIFile(adbPath, context)
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(cacheFile)
            val nodes = doc.getElementsByTagName("node")

            val resultTexts = mutableListOf<String>()

            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val attrs = node.attributes

                val resourceId = attrs.getNamedItem("resource-id")?.nodeValue ?: continue
                if (resourceId == "com.sprd.engineermode:id/result_text") {
                    val text = attrs.getNamedItem("text")?.nodeValue ?: ""
                    resultTexts.add(text)
                }
            }

            Log.d("kano_ZTE_LOG", "共找到${resultTexts.size}条 result_text 文本：$resultTexts")
            return resultTexts
        }

        //获取UI文件
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