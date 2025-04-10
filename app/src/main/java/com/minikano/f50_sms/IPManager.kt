package com.minikano.f50_sms

import android.content.Context

object IPManager {

    /**
     * 获取当前 WiFi 连接的 IPv4 网关地址
     * @param context 应用上下文
     * @return 网关地址（如 192.168.1.1），获取失败返回 null
     */
    fun getWifiGatewayIp(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val dhcpInfo = wifiManager?.dhcpInfo
            val gatewayInt = dhcpInfo?.gateway ?: return null
            val gatewayIp = intToIp(gatewayInt)

            if (gatewayIp == "0.0.0.0") null else gatewayIp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}