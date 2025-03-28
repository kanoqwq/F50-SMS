import fi.iki.elonen.NanoHTTPD
import android.content.Context
import android.util.Log
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class WebServer(context: Context, port: Int) : NanoHTTPD(port) {

    private val targetServer = "http://192.168.0.1"  // 目标服务器地址

    override fun serve(session: IHTTPSession?): Response {
        val method = session?.method.toString()
        val uri = session?.uri ?: "/"

        // 获取查询参数
        val queryString = session?.queryParameterString
        val fullUrl = if (queryString.isNullOrEmpty()) {
            "$targetServer$uri"
        } else {
            "$targetServer$uri?$queryString"
        }

        // 处理 OPTIONS 请求
        if (method == "OPTIONS") {
            val response = newFixedLengthResponse(Response.Status.OK, "text/plain", "")
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With")
            return response
        }

        Log.d("kanokano", fullUrl)

        // 构造目标 URL
        return try {
            val url = URL(fullUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            // 复制请求头
            session?.headers?.forEach { (key, value) ->
                if (!key.equals("host", ignoreCase = true)) { // 跳过 Host 头，避免影响目标服务器
                    conn.setRequestProperty(key, value)
                }
            }
            conn.setRequestProperty("Referer", "http://192.168.0.1") // 添加 Referer 头

            // 处理 POST 请求体
            if (method == "POST" || method == "PUT") {
                val contentLength = session?.headers?.get("content-length")?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    // 手动读取请求体
                    val inputStream = session?.inputStream
                    val requestBody = ByteArray(contentLength)
                    if (inputStream != null) {
                        inputStream.read(requestBody)
                    }

                    // 将请求体转换为字符串
                    val requestBodyStr = String(requestBody, Charsets.UTF_8)
                    Log.d("kanokano", "Request Length: ${requestBodyStr.length}")
                    Log.d("kanokano", "Request Body: $requestBodyStr")

                    // 解析 URL 编码格式的请求体
                    val params = parseUrlEncoded(requestBodyStr)
                    Log.d("kanokano", "Parsed Body: $params")

                    // 发送请求体到目标服务器
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Length", requestBodyStr.toByteArray().size.toString())
                    conn.outputStream.use { it.write(requestBodyStr.toByteArray()) }
                }
            }

            conn.connect()

            val responseCode = conn.responseCode
            val responseStream: InputStream = if (responseCode in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream
            }

            val response = newFixedLengthResponse(
                Response.Status.lookup(responseCode),
                conn.contentType ?: "text/plain",
                responseStream,
                conn.contentLength.toLong()
            )

            // 将目标服务器的所有响应头转发给客户端
            conn.headerFields.forEach { (key, value) ->
                if (key != null && value != null && key.equals("Set-Cookie", ignoreCase = true)) {
                    value.forEach { cookie ->
                        response.addHeader("kano-cookie", cookie)  // 转发 Set-Cookie
                    }
                }
            }

            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With")

            response
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Proxy error: ${e.message}")
        }
    }

    // 解析 URL 编码的请求体
    fun parseUrlEncoded(data: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val pairs = data.split("&")

        for (pair in pairs) {
            val keyValue = pair.split("=")
            if (keyValue.size == 2) {
                val key = keyValue[0]
                val value = keyValue[1]
                params[key] = java.net.URLDecoder.decode(value, Charsets.UTF_8.name())  // 解码
            }
        }

        return params
    }
}