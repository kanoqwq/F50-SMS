package com.minikano.f50_sms

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

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

        fun executeShellFromAssetsSubfolderWithArgs(
            context: Context,
            assetSubPath: String,
            vararg args: String
        ): String? {
            return try {
                val assetManager = context.assets
                val inputStream = assetManager.open(assetSubPath)
                val fileName = File(assetSubPath).name
                val outFile = File(context.cacheDir, fileName)

                inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }

                outFile.setExecutable(true)

                val command = ArrayList<String>().apply {
                    add(outFile.absolutePath)
                    addAll(args)
                }

                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

                output
            } catch (e: Exception) {
                Log.d("kano_ZTE_LOG", "adb执行出错：${e.message}")
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