package com.minikano.f50_sms

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter

object IPManager {

    /**
     * 获取当前 WiFi 连接的 IPv4 网关地址
     * @param context 应用上下文
     * @return 网关地址（如 192.168.1.1），获取失败返回 null
     */
    fun getWifiGatewayIp(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if(!wifiManager.isWifiEnabled) return null
            val connectionInfo = wifiManager.connectionInfo

            // 获取当前连接的SSID（即Wi-Fi的名称）
            val speed = connectionInfo.linkSpeed
            // 如果 speed 是有效的，说明当前已连接到 Wi-Fi
            if(speed == -1) return null

            val dhcpInfo = wifiManager.dhcpInfo
            if (dhcpInfo != null) {
                Formatter.formatIpAddress(dhcpInfo.gateway)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}