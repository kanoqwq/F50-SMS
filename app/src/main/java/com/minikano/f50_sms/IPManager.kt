package com.minikano.f50_sms

import android.content.Context
import android.util.Log

object IPManager {

    /**
     * 获取当前 WiFi 连接的 IPv4 网关地址
     * @param context 应用上下文
     * @return 网关地址（如 192.168.1.1），获取失败返回 null
     */
    fun getWifiGatewayIp(context: Context): String? {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val dhcpInfo = wifiManager?.dhcpInfo
            val gatewayInt = dhcpInfo?.gateway ?: return null
            val gatewayIp = intToIp(gatewayInt)

            if (gatewayIp == "0.0.0.0") null else gatewayIp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getHotspotGatewayIp(setPort:String?): String? {
        try {
            val process = Runtime.getRuntime().exec("ip route")
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.destroy()

            val regex = Regex("""([0-9.]+/\d+)\s+dev\s+(\w+)\s+.*src\s+([0-9.]+)""")

            regex.findAll(output).forEach { match ->
                val iface = match.groupValues[2]
                val ip = match.groupValues[3]

                // 过滤掉不太可能是热点的接口
                if (iface.startsWith("br") || iface.startsWith("ap")) {
                    Log.d("kano_ZTE_LOG", "IPManager 获取热点IP：$ip:$setPort")
                    if(setPort != null){
                        return "$ip:$setPort" // 找到热点网关 IP
                    }
                    return ip // 找到热点网关 IP
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}