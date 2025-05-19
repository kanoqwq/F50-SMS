package com.minikano.f50_sms
import android.content.Context
import android.util.Log
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.concurrent.atomic.AtomicBoolean

class KanoSMTP(
    private val smtpHost: String,
    private val smtpPort: String,
    private val username: String,
    private val password: String,
) {
    // 防止重复发送
    private val isSending = AtomicBoolean(false)

    fun sendEmail(to: String, subject: String, body: String) {
        // 如果已经在发送中，则直接返回
        if (!isSending.compareAndSet(false, true)) {
            Log.w("kano_ZTE_LOG", "邮件正在发送中，忽略重复发送")
            return
        }

        Thread {
            try {
                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.host"] = smtpHost
                props["mail.smtp.port"] = smtpPort

                if (smtpPort == "465") {
                    props["mail.smtp.ssl.enable"] = "true"
                    props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                } else {
                    props["mail.smtp.starttls.enable"] = "true"
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(username, password)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(username))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                    setSubject(subject)
                    setText(body)
                }

                Log.d("kano_ZTE_LOG", "开始发送邮件...")
                Transport.send(message)
                Log.d("kano_ZTE_LOG", "$username 邮件发送成功")

            } catch (e: Exception) {
                Log.e("kano_ZTE_LOG", "$username 邮件发送失败: ${e.message}", e)
            } finally {
                isSending.set(false)
            }
        }.start()
    }
}