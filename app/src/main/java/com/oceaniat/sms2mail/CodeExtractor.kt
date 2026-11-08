package com.oceaniat.sms2mail

/**
 * 从短信正文里猜验证码。
 * 先找关键词后面的数字（最可靠），再退化为「全文唯一的 4~8 位数字」。
 */
object CodeExtractor {

    private val keywordRegex = Regex(
        "(?:验证码|校验码|动态码|动态密码|确认码|激活码|短信密码|登录码|verification\\s*code|code|otp|pin)" +
            "\\D{0,16}?(\\d{4,8})",
        RegexOption.IGNORE_CASE
    )

    private val anyCode = Regex("(?<!\\d)(\\d{4,8})(?!\\d)")

    fun extract(body: String): String {
        keywordRegex.find(body)?.let { m ->
            val v = m.groupValues.getOrNull(1).orEmpty()
            if (v.isNotEmpty()) return v
        }
        val cands = anyCode.findAll(body).map { it.groupValues[1] }.distinct().toList()
        val six = cands.filter { it.length == 6 }
        if (six.size == 1) return six[0]
        if (cands.size == 1) return cands[0]
        return ""
    }

    fun hitAnyKeyword(body: String, keywords: List<String>): Boolean =
        keywords.any { it.isNotBlank() && body.contains(it, ignoreCase = true) }
}
