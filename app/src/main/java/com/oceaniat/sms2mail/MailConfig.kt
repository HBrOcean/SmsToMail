package com.oceaniat.sms2mail

/**
 * 转发配置。
 *
 * 默认值针对 163 邮箱：smtp.163.com + 465 端口 + 隐式 SSL。
 * password 填的是「客户端授权码」，不是邮箱登录密码。
 */
data class MailConfig(
    val enabled: Boolean = true,
    val smtpHost: String = "smtp.163.com",
    val smtpPort: Int = 465,
    val ssl: Boolean = true,
    /** 发件邮箱，同时也是 SMTP 登录账号，例如 xxx@163.com */
    val username: String = "",
    /** 授权码 */
    val password: String = "",
    val fromName: String = "短信转发",
    /** 收件邮箱，多个用英文逗号分隔 */
    val to: String = "",
    /** true = 所有短信都转发；false = 只转发疑似验证码/命中关键词的短信 */
    val forwardAll: Boolean = false,
    /** 关键词，英文或中文逗号分隔 */
    val keywords: String = "验证码,校验码,动态码,动态密码,确认码,激活码,code,verification"
) {
    fun recipients(): List<String> =
        to.split(",", ";", "，", "；", " ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun keywordList(): List<String> =
        keywords.split(",", "，", ";", "；")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun isReady(): Boolean =
        smtpHost.isNotBlank() && username.isNotBlank() && password.isNotBlank() && recipients().isNotEmpty()
}
