package com.minikano.f50_sms

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import kotlin.system.exitProcess

class JSInterface(private val context: Context) {
    private var value: String = ""

    @JavascriptInterface
    fun setValue(value: String) {
        Log.d("kano_ZTE_LOG", "JS 传来的值是：$value")
        this.value = value
    }

    @JavascriptInterface
    fun getValue(): String {
        return this.value
    }

    @JavascriptInterface
    fun exit() {
        exitProcess(0)
    }

    @JavascriptInterface
    fun getVersion(): String {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        return versionName ?: "未知版本"
    }
}