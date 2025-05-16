package com.minikano.f50_sms

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import jcifs.smb.SmbFile
import jcifs.context.SingletonContext

object SmbThrottledRunner {
    private val running = AtomicBoolean(false)

    fun runOnceInThread() {
        if (running.get()) {
            Log.d("kano_ZTE_LOG", "SMB 命令正在执行中，跳过")
            return
        }

        running.set(true)

        Thread {
            try {
                Log.d("kano_ZTE_LOG", "开始执行 SMB 命令")

                val context = SingletonContext.getInstance()
                val smbFile = SmbFile("smb://localhost/root/", context)

                if (smbFile.exists()) {
                    Log.d("kano_ZTE_LOG", "路径存在")
                } else {
                    Log.d("kano_ZTE_LOG", "路径不存在")
                }
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "SMB命令错误：${e.message}")
            } finally {
                running.set(false)
                Log.d("kano_ZTE_LOG", "SMB 命令执行完成")
            }
        }.start()
    }
}