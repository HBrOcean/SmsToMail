package com.oceaniat.sms2mail

import java.io.UnsupportedEncodingException
import java.util.Date
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * 用 JavaMail 直连 SMTP 发信（163：smtp.163.com:465 隐式 SSL）。
 * 全部在主线程之外调用。
 */
object MailSender {

    fun send(cfg: MailConfig, subject: String, body: String) {
        if (!cfg.isReady()) {
            throw IllegalArgumentException("配置不完整：发件邮箱 / 授权码 / 收件邮箱 都要填")
        }

        val props = Properties().apply {
            put("mail.transport.protocol", "smtp")
            put("mail.smtp.host", cfg.smtpHost)
            put("mail.smtp.port", cfg.smtpPort.toString())
            put("mail.smtp.auth", "true")
            if (cfg.ssl) {
                // 465 端口：建立连接时就直接用 SSL
                put("mail.smtp.ssl.enable", "true")
            } else {
                // 587 端口：明文连接后用 STARTTLS 升级
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }
            put("mail.smtp.connectiontimeout", "20000")
            put("mail.smtp.timeout", "30000")
            put("mail.smtp.writetimeout", "30000")
            // 部分网络环境下证书链不完整，避免因此发不出去
            put("mail.smtp.ssl.trust", cfg.smtpHost)
        }

        val auth = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication =
                PasswordAuthentication(cfg.username, cfg.password)
        }

        val session = Session.getInstance(props, auth)

        val msg = MimeMessage(session)
        msg.setFrom(makeFrom(cfg))
        msg.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(cfg.recipients().joinToString(","), false)
        )
        msg.setSubject(subject, "UTF-8")
        msg.setText(body, "UTF-8")
        msg.setHeader("X-Mailer", "SmsToMail")
        msg.sentDate = Date()

        Transport.send(msg)
    }

    private fun makeFrom(cfg: MailConfig): InternetAddress =
        try {
            InternetAddress(cfg.username, cfg.fromName, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            InternetAddress(cfg.username)
        }

    /** 把 JavaMail 的原始报错翻译成能照着做的中文提示 */
    fun friendlyError(t: Throwable): String {
        val raw = (t.message ?: t.javaClass.simpleName).trim()
        return when {
            raw.contains("535") || raw.contains("Authentication credentials") ->
                "认证失败（535）：授权码不对，或邮箱没开启 SMTP 服务。163 邮箱需在网页版「设置 → POP3/SMTP/IMAP」开启 SMTP 并生成授权码（不是登录密码）。"
            raw.contains("553") ->
                "被拒绝（553）：发件邮箱必须与 SMTP 登录账号完全一致（163 的硬性要求）。"
            raw.contains("550") || raw.contains("554") ->
                "被服务器拒绝（550/554）：收件邮箱可能不合法，或发信频率触发了限制。"
            raw.contains("UnknownHost") ->
                "域名解析失败：SMTP 服务器地址写错了，或当前网络不可用。"
            raw.contains("Connect") || raw.contains("timeout") || raw.contains("SocketTimeout") ->
                "连接超时：检查网络 / 服务器地址 / 端口（163 用 smtp.163.com:465 并勾选 SSL）。"
            else -> "发送失败：$raw"
        }
    }
}
