package com.minikano.f50_sms

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import jcifs.smb.SmbFile
import jcifs.context.SingletonContext

object SmbThrottledRunner {
    private val running = AtomicBoolean(false)
    var gatewayIP : String = "192.168.0.1"
    private val PREF_GATEWAY_IP = "gateway_ip"
    private val PREFS_NAME = "kano_ZTE_store"

    fun runOnceInThread(context: Context) {
        if (running.get()) {
            Log.d("kano_ZTE_LOG", "SMB 命令正在执行中，跳过")
            return
        }

        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gatewayIP = sharedPrefs.getString(PREF_GATEWAY_IP, "localhost:445")
        val host = gatewayIP?.substringBefore(":")

        running.set(true)

        Thread {
            try {

                Log.d("kano_ZTE_LOG", "开始执行 SMB 命令,连接到：\"smb://$host/root/\"")

                val ctx = SingletonContext.getInstance()
                val smbFile = SmbFile("smb://$host/root/", ctx)

                if (smbFile.exists()) {
                    Log.d("kano_ZTE_LOG", "SMB路径存在")
                } else {
                    Log.d("kano_ZTE_LOG", "SMB路径不存在")
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