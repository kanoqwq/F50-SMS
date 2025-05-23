package com.minikano.f50_sms

import android.content.Context
import android.net.Uri
import android.util.Log
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
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val sms_forward_method = sharedPrefs.getString("kano_sms_forward_method", "") ?: ""
            if(sms_forward_method =="SMTP") {
                forwardByEmail(lastSms, context)
            }
            else if(sms_forward_method == "CURL"){
                forwardSmsByCurl(lastSms,context)
            }
        } else {
            Log.d("kano_ZTE_LOG", "无新短信，短信是否${minute}分钟内：$withinMin,短信是否为新：$isNew")
        }
    }

    //通过curl转发
    fun forwardSmsByCurl(sms_data:SmsInfo?,context: Context) {
        if (sms_data == null) return
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val originalCurl = sharedPrefs.getString("kano_sms_curl", null)
        if (originalCurl.isNullOrEmpty()) {
            Log.e("kano_ZTE_LOG", "curl 配置错误：kano_sms_curl 为空")
            return
        }

        Log.d("kano_ZTE_LOG", "开始转发短信...（CURL）")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        val smsText = """${sms_data!!.body.trimStart()}
        📩 来自：${sms_data!!.address}
        ⏰ 时间：${formatter.format(Instant.ofEpochMilli(sms_data!!.timestamp))}
        """.trimIndent()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

        //替换并发送
        val replacedCurl = originalCurl.replace("{{sms}}", smsText)
        KanoCURL(context).send(replacedCurl)
    }

    //通过SMTP邮件转发
    fun forwardByEmail(sms_data:SmsInfo?,context: Context) {
        if (sms_data == null) return
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

        Log.d("kano_ZTE_LOG", "开始转发短信...(SMTP)")

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        val previewText = sms_data!!.body.trimStart().let {
            if (it.length > 37) it.take(37) + "…" else it
        }
        smtpClient.sendEmail(
            to = smtpTo,
            subject = previewText,
            body = """${sms_data!!.body.trimStart()}
            📩 <b>来自：</b>${sms_data!!.address}
            ⏰ <b>时间：</b>${formatter.format(Instant.ofEpochMilli(sms_data!!.timestamp))}
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