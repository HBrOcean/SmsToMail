package com.oceaniat.sms2mail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 短信到达时被系统唤起（不需要 App 常驻前台/后台服务）。
 * 这里只做解析 + 判断 + 入队，实际发信交给 SendMailWorker。
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val cfg = Prefs(context).load()
        if (!cfg.enabled) return

        val parts = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            null
        } ?: return
        if (parts.isEmpty()) return

        // 长短信会被拆成多条（multipart），按发件人拼回完整正文
        val merged = LinkedHashMap<String, StringBuilder>()
        for (p in parts) {
            val from = p.originatingAddress?.takeIf { it.isNotBlank() } ?: "未知号码"
            merged.getOrPut(from) { StringBuilder() }.append(p.messageBody ?: "")
        }

        for ((from, sb) in merged) {
            val body = sb.toString().trim()
            if (body.isEmpty()) continue
            handle(context, cfg, from, body)
        }
    }

    private fun handle(context: Context, cfg: MailConfig, from: String, body: String) {
        val code = CodeExtractor.extract(body)
        val hitKeyword = CodeExtractor.hitAnyKeyword(body, cfg.keywordList())

        if (!cfg.forwardAll && !hitKeyword && code.isEmpty()) {
            LogStore.add(context, from, "", false, "已忽略：未命中关键词，也不像验证码短信")
            return
        }

        val stamp = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date())
        val subject =
            if (code.isNotEmpty()) "【验证码 $code】$from  $stamp"
            else "【短信】$from  $stamp"

        val mailBody = buildString {
            append("发件人：").append(from).append('\n')
            append("时间：")
                .append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date()))
                .append('\n')
            if (code.isNotEmpty()) append("验证码：").append(code).append('\n')
            append("\n—— 短信原文 ——\n").append(body).append('\n')
        }

        val data = Data.Builder()
            .putString(SendMailWorker.KEY_SENDER, from)
            .putString(SendMailWorker.KEY_CODE, code)
            .putString(SendMailWorker.KEY_SUBJECT, subject)
            .putString(SendMailWorker.KEY_BODY, mailBody)
            .build()

        val request = OneTimeWorkRequestBuilder<SendMailWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
        LogStore.add(context, from, code, true, "收到短信，已加入发送队列")
    }
}
