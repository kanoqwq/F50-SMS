package com.minikano.f50_sms

import java.io.BufferedReader
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
    }
}