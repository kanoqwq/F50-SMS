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

//        fun executeShellFromAssetsSubfolderWithArgs(
//            context: Context,
//            assetSubPath: String,
//            vararg args: String
//        ): String? {
//            return try {
//                val assetManager = context.assets
//                val inputStream = assetManager.open(assetSubPath)
//                val fileName = File(assetSubPath).name
//                val outFile = File(context.cacheDir, fileName)
//
//                inputStream.use { input ->
//                    FileOutputStream(outFile).use { output ->
//                        input.copyTo(output)
//                    }
//                }
//
//                outFile.setExecutable(true)
//
//                val command = ArrayList<String>().apply {
//                    add(outFile.absolutePath)
//                    addAll(args)
//                }
//
//                val process = ProcessBuilder(command)
//                    .redirectErrorStream(true)
//                    .start()
//
//                val output = process.inputStream.bufferedReader().readText()
//                process.waitFor()
//
//                output
//            } catch (e: Exception) {
//                Log.d("kano_ZTE_LOG", "executeShellFromAssetsSubfolderWithArgs 执行出错：${e.message}")
//                e.printStackTrace()
//                null
//            }
//        }

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