package com.yunlian.app.data.network

import java.net.URLDecoder

/** 网盘平台 */
enum class SharePlatform { QUARK, UC, XUNLEI }

/**
 * 解析结果：share_id + 提取码 + 平台。
 */
data class ParsedShare(
    val shareId: String,
    val pwd: String?,
    val platform: SharePlatform
)

/**
 * 从分享链接或整段分享文案中提取 share_id 与提取码。
 * 支持：pan.quark.cn/s/xxx（夸克）、drive.uc.cn/s/xxx（UC）、pan.xunlei.com/s/xxx（迅雷）
 */
object ShareLinkParser {

    private val urlRegex = Regex("""https?://[^\s]+""")
    private val quarkShareIdRegex = Regex("""pan\.quark\.cn/s/([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
    private val ucShareIdRegex = Regex("""drive\.uc\.cn/s/([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
    private val xunleiShareIdRegex = Regex("""pan\.xunlei\.com/s/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
    private val pwdInUrlRegex = Regex("""[?&]pwd=([^&#\s]{1,96})""", RegexOption.IGNORE_CASE)
    private val pwdInTextRegex = Regex("""(?:提取码|访问码|密码)[：:]\s*([A-Za-z0-9_-]{1,32})""", RegexOption.IGNORE_CASE)

    fun parse(text: String): ParsedShare? {
        val url = urlRegex.find(text.trim())?.value
            ?.trimEnd('。', '，', ',', '；', ';', ')', ']', '}', '"', '\'')
            ?: return null
        // 夸克链接
        quarkShareIdRegex.find(url)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(url)?.groupValues?.getOrNull(1)
                ?.let { URLDecoder.decode(it, "UTF-8") }
                ?: pwdInTextRegex.find(text)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.QUARK)
        }
        // UC 链接
        ucShareIdRegex.find(url)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(url)?.groupValues?.getOrNull(1)
                ?.let { URLDecoder.decode(it, "UTF-8") }
                ?: pwdInTextRegex.find(text)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.UC)
        }
        // 迅雷链接
        xunleiShareIdRegex.find(url)?.groupValues?.getOrNull(1)?.let { sid ->
            val pwd = pwdInUrlRegex.find(url)?.groupValues?.getOrNull(1)
                ?.let { URLDecoder.decode(it, "UTF-8") }
                ?: pwdInTextRegex.find(text)?.groupValues?.getOrNull(1)
            return ParsedShare(shareId = sid, pwd = pwd, platform = SharePlatform.XUNLEI)
        }
        return null
    }
}
