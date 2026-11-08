package com.oceaniat.sms2mail

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val time: String,
    val sender: String,
    val code: String,
    val ok: Boolean,
    val message: String
)

/**
 * 简单的最近转发记录（存在 SharedPreferences，最多 80 条）。
 * 用来在界面上确认「收到短信了吗 / 发出去了吗 / 失败原因是什么」。
 */
object LogStore {

    private const val SP = "sms2mail_logs"
    private const val KEY = "list"
    private const val MAX = 80
    private val lock = Any()

    fun add(context: Context, sender: String, code: String, ok: Boolean, message: String) {
        synchronized(lock) {
            val sp = context.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
            val old = try {
                JSONArray(sp.getString(KEY, "[]") ?: "[]")
            } catch (e: Exception) {
                JSONArray()
            }
            val out = JSONArray()
            out.put(
                JSONObject().apply {
                    put("t", now())
                    put("s", sender)
                    put("c", code)
                    put("ok", ok)
                    put("m", message)
                }
            )
            for (i in 0 until old.length()) {
                if (out.length() >= MAX) break
                val item = old.optJSONObject(i) ?: continue
                out.put(item)
            }
            sp.edit().putString(KEY, out.toString()).apply()
        }
    }

    fun all(context: Context): List<LogEntry> {
        val sp = context.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val arr = try {
            JSONArray(sp.getString(KEY, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
        val result = ArrayList<LogEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            result.add(
                LogEntry(
                    time = o.optString("t", ""),
                    sender = o.optString("s", ""),
                    code = o.optString("c", ""),
                    ok = o.optBoolean("ok", false),
                    message = o.optString("m", "")
                )
            )
        }
        return result
    }

    fun render(context: Context): String {
        val list = all(context)
        if (list.isEmpty()) return "暂无记录"
        return list.joinToString("\n") { e ->
            val mark = if (e.ok) "✓" else "✗"
            val head = "$mark  ${e.time}  ${e.sender}" + if (e.code.isNotEmpty()) "  验证码 ${e.code}" else ""
            "$head\n    ${e.message}"
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            context.applicationContext
                .getSharedPreferences(SP, Context.MODE_PRIVATE)
                .edit().putString(KEY, "[]").apply()
        }
    }

    private fun now(): String =
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(Date())
}
