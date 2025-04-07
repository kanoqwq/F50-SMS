package com.minikano.f50_sms

import android.util.Log
import android.webkit.JavascriptInterface

class JSInterface {
    private var value: String = ""
    @JavascriptInterface
    fun setValue(value: String) {
        Log.d("JSInterface", "JS 传来的值是：$value")
        // 你可以把这个值保存到 App 的变量、SharedPreferences、全局类等
        this.value = value
    }
    fun getValue(): String {
        return this.value
    }
}