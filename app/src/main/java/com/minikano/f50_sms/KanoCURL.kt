package com.minikano.f50_sms

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
class KanoCURL(private val context: Context) {
    // 防止重复发送
    private val isSending = AtomicBoolean(false)

    fun send(command:String) {
        // 如果已经在发送中，则直接返回
        if (!isSending.compareAndSet(false, true)) {
            Log.w("kano_ZTE_LOG_Curl", "curl正在请求中，忽略重复请求")
            return
        }
        Thread {
            try {
                Log.w("kano_ZTE_LOG_Curl", "正在执行curl命令:$command")
                val args = KanoUtils.parseShellArgs(command.replace("curl", ""))
                val result = ShellKano.executeShellFromAssetsSubfolderWithArgs(
                    context,
                    "shell/curl",
                    *args.toTypedArray(),
                    timeoutMs = 10000
                )?: throw Exception("runShellCommand为null")
                Log.w("kano_ZTE_LOG_Curl", "执行curl命令结果：$result")
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG_Curl", "curl请求失败: ${e.message}", e)
            } finally {
                isSending.set(false)
            }
        }.start()
    }
}