package com.minikano.f50_sms

import android.content.Context
import android.net.Uri
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SmsInfo(val address: String, val body: String, val timestamp: Long)

object SmsPoll {
    private var lastSms: SmsInfo? = null

    //store
    private val PREFS_NAME = "kano_ZTE_store"

    fun checkNewSmsAndSend(context: Context) {
        val sms = getLatestSms(context) ?: return

        val now = System.currentTimeMillis()
        val minute = 2
        val withinMin = now - sms.timestamp <= minute * 60 * 1000
        val isNew = lastSms == null || sms != lastSms

        if (withinMin && isNew) {
            Log.d("kano_ZTE_LOG", "收到新短信: ${sms.address} - ${sms.body}")
            lastSms = sms
            // 在这里做转发处理
            forwardByEmail(context)
        } else {
            Log.d("kano_ZTE_LOG", "无新短信，短信是否${minute}分钟内：$withinMin,短信是否为新：$isNew")
        }
    }

    //TODO：通过API转发
    private fun forwardSmsToServer(address: String, body: String) {
        Thread {
            try {
                val url = URL("https://your-server.com/api/sms")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val json = """
                {
                    "sender": "${address.replace("\"", "\\\"")}",
                    "message": "${body.replace("\"", "\\\"")}"
                }
                """.trimIndent()

                conn.outputStream.use { os ->
                    os.write(json.toByteArray())
                }

                val responseCode = conn.responseCode
                Log.d("kano_ZTE_LOG", "转发完成，状态码: $responseCode")
            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "转发失败", e)
            }
        }.start()
    }

    //通过SMTP邮件转发
    private fun forwardByEmail(context: Context) {
        if (lastSms == null) return
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val smtpHost = sharedPrefs.getString("kano_smtp_host", null)
        if (smtpHost.isNullOrEmpty()) {
            Log.e("kano_ZTE_LOG", "SMTP 配置错误：kano_smtp_host 为空")
            return
        }

        val smtpTo = sharedPrefs.getString("kano_smtp_to", null)
        if (smtpTo.isNullOrEmpty()) {
            Log.e("kano_ZTE_LOG", "SMTP 配置错误：kano_smtp_to 为空")
            return
        }

        val smtpPort = sharedPrefs.getString("kano_smtp_port", null)
        if (smtpPort.isNullOrEmpty()) {
            Log.e("kano_ZTE_LOG", "SMTP 配置错误：kano_smtp_port 为空")
            return
        }

        val username = sharedPrefs.getString("kano_smtp_username", null)
        if (username.isNullOrEmpty()) {
            Log.e("kano_ZTE_LOG", "SMTP 配置错误：kano_smtp_username 为空")
            return
        }

        val password = sharedPrefs.getString("kano_smtp_password", null)
        if (password.isNullOrEmpty()) {
            Log.e("kano_ZTE_LOG", "SMTP 配置错误：kano_smtp_password 为空")
            return
        }

        val smtpClient = KanoSMTP(smtpHost, smtpPort, username, password)

        Log.d("kano_ZTE_LOG", "开始转发短信...")

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        val previewText = lastSms!!.body.trimStart().let {
            if (it.length > 37) it.take(37) + "…" else it
        }
        smtpClient.sendEmail(
            to = smtpTo,
            subject = previewText,
            body = """${lastSms!!.body.trimStart()}
            📩 <b>来自：</b>${lastSms!!.address}
            ⏰ <b>时间：</b>${formatter.format(Instant.ofEpochMilli(lastSms!!.timestamp))}
            <div style="text-align=center"><i>Powered by <a href="">UFI-TOOLS</a></i></div>
            """.trimIndent()
        )
    }

    fun getLatestSms(context: Context): SmsInfo? {
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")
        val sortOrder = "date DESC"

        return try {
            val cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)
            cursor?.use {
                if (it.moveToFirst()) {
                    val address = it.getString(it.getColumnIndexOrThrow("address"))
                    val body = it.getString(it.getColumnIndexOrThrow("body"))
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))
                    SmsInfo(address, body, date)
                } else null
            }
        } catch (e: Exception) {
            Log.e("kano_ZTE_LOG", "没有短信权限，读不到短信呢", e)
            null
        }
    }
}