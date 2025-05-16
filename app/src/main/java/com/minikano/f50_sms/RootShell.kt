package com.minikano.f50_sms

import android.util.Log
import android.net.LocalSocket
import android.net.LocalSocketAddress
import java.io.*

object RootShell {

    fun sendCommandToSocket(command: String,socketPath: String): String? {
        val socket = LocalSocket()
        val socketAddress = LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM)

        try {
            socket.connect(socketAddress)

            val outputStream = BufferedWriter(OutputStreamWriter(socket.outputStream))
            val inputStream = BufferedReader(InputStreamReader(socket.inputStream))

            // 发送命令
            outputStream.write(command)
            outputStream.write("\n")
            outputStream.write("echo __END__\n") // 标记结尾
            outputStream.flush()

            // 读取响应
            val result = StringBuilder()
            while (true) {
                val line = inputStream.readLine() ?: break
                if (line.trim() == "__END__") break
                result.appendLine(line)
            }

            return result.toString()

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("kano_ZTE_LOG", "Socket Error: ${e.message}")
            return null
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
    }
}