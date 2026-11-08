package com.oceaniat.sms2mail

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 真正的发信任务。用 WorkManager 排队，好处是：
 *  - 短信广播返回后进程可以被回收，任务不会丢
 *  - 断网时自动等网络，联网后重试
 */
class SendMailWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val cfg = Prefs(applicationContext).load()
        if (!cfg.enabled) return@withContext Result.success()

        val sender = inputData.getString(KEY_SENDER).orEmpty()
        val code = inputData.getString(KEY_CODE).orEmpty()
        val subject = inputData.getString(KEY_SUBJECT).orEmpty()
        val body = inputData.getString(KEY_BODY).orEmpty()

        try {
            MailSender.send(cfg, subject, body)
            LogStore.add(
                applicationContext, sender, code, true,
                "已发送到 ${cfg.recipients().joinToString(", ")}"
            )
            Result.success()
        } catch (e: Exception) {
            val msg = MailSender.friendlyError(e)
            LogStore.add(applicationContext, sender, code, false, msg)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_SENDER = "sender"
        const val KEY_CODE = "code"
        const val KEY_SUBJECT = "subject"
        const val KEY_BODY = "body"
        const val WORK_NAME = "send_sms_mail"
    }
}
