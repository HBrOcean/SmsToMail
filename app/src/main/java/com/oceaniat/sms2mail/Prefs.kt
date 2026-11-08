package com.oceaniat.sms2mail

import android.content.Context

/**
 * 配置读写。存放于应用私有 SharedPreferences（其他应用无法读取）。
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("sms2mail", Context.MODE_PRIVATE)

    fun load(): MailConfig = MailConfig(
        enabled = sp.getBoolean("enabled", true),
        smtpHost = sp.getString("smtpHost", "smtp.163.com") ?: "smtp.163.com",
        smtpPort = sp.getInt("smtpPort", 465),
        ssl = sp.getBoolean("ssl", true),
        username = sp.getString("username", "") ?: "",
        password = sp.getString("password", "") ?: "",
        fromName = sp.getString("fromName", "短信转发") ?: "短信转发",
        to = sp.getString("to", "") ?: "",
        forwardAll = sp.getBoolean("forwardAll", false),
        keywords = sp.getString("keywords", MailConfig().keywords) ?: MailConfig().keywords
    )

    fun save(cfg: MailConfig) {
        sp.edit()
            .putBoolean("enabled", cfg.enabled)
            .putString("smtpHost", cfg.smtpHost)
            .putInt("smtpPort", cfg.smtpPort)
            .putBoolean("ssl", cfg.ssl)
            .putString("username", cfg.username)
            .putString("password", cfg.password)
            .putString("fromName", cfg.fromName)
            .putString("to", cfg.to)
            .putBoolean("forwardAll", cfg.forwardAll)
            .putString("keywords", cfg.keywords)
            .apply()
    }
}
