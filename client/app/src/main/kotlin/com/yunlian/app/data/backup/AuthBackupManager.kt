package com.yunlian.app.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.yunlian.app.data.db.QuarkAccountDao
import com.yunlian.app.data.db.QuarkAccountEntity
import com.yunlian.app.data.db.UCAccountDao
import com.yunlian.app.data.db.UCAccountEntity
import com.yunlian.app.data.db.XunleiAccountDao
import com.yunlian.app.data.db.XunleiAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 网盘认证信息备份：把已登录的平台（夸克/UC/迅雷）凭证打包为 JSON，
 * 可导出到下载目录并在另一台设备导入恢复。
 */
class AuthBackupManager(
    private val quarkDao: QuarkAccountDao,
    private val ucDao: UCAccountDao,
    private val xunleiDao: XunleiAccountDao
) {

    private companion object {
        const val APP_TAG = "yunx_auth_backup"
        const val VERSION = 1
    }

    /**
     * 导出网盘认证（强制 AES-GCM 加密）：
     * @param password 至少 8 位的备份口令
     * @param onlyLoggedIn true=仅导出凭证可用的已登录平台；false=导出数据库里全部绑定记录
     * @return 明文 JSON 或 Base64 密文
     */
    suspend fun export(password: String? = null, onlyLoggedIn: Boolean = true): String =
        withContext(Dispatchers.IO) {
            require(!password.isNullOrBlank() && password.length >= 8) { "备份口令至少 8 位" }
            val json = exportJson(onlyLoggedIn)
            AuthCrypto.encrypt(json, password)
        }

    /** 导出所有已登录平台为 JSON 字符串；无已登录平台时返回空 accounts */
    suspend fun exportJson(onlyLoggedIn: Boolean = true): String = withContext(Dispatchers.IO) {
        val accounts = JSONArray()
        quarkDao.getAccount()?.let { a ->
            if (!onlyLoggedIn || a.cookie.isNotBlank()) accounts.put(
                JSONObject()
                    .put("platform", "quark")
                    .put("cookie", a.cookie)
                    .put("nickname", a.nickname)
                    .put("updatedAt", a.updatedAt)
            )
        }
        ucDao.getAccount()?.let { a ->
            if (!onlyLoggedIn || a.cookie.isNotBlank()) accounts.put(
                JSONObject()
                    .put("platform", "uc")
                    .put("cookie", a.cookie)
                    .put("nickname", a.nickname)
                    .put("updatedAt", a.updatedAt)
            )
        }
        xunleiDao.getAccount()?.let { a ->
            if (!onlyLoggedIn || a.accessToken.isNotBlank()) accounts.put(
                JSONObject()
                    .put("platform", "xunlei")
                    .put("accessToken", a.accessToken)
                    .put("refreshToken", a.refreshToken)
                    .put("deviceId", a.deviceId)
                    .put("captchaToken", a.captchaToken)
                    .put("nickname", a.nickname)
                    .put("updatedAt", a.updatedAt)
            )
        }
        JSONObject()
            .put("app", APP_TAG)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("accounts", accounts)
            .toString(2)
    }

    /**
     * 导入认证内容（可选 AES 解密）：
     * @param password 非空时先解密（密码错误抛异常）；null/空按明文 JSON 解析
     * @return 成功恢复的平台数；文件不合法抛异常
     */
    suspend fun import(content: String, password: String? = null): Int = withContext(Dispatchers.IO) {
        val json = if (password.isNullOrBlank()) content else AuthCrypto.decrypt(content, password)
        importJson(json)
    }

    /** 导入 JSON，恢复各平台凭证；返回成功恢复的平台数；文件不合法抛异常 */
    suspend fun importJson(json: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        if (root.optString("app") != APP_TAG) {
            throw IllegalArgumentException("不是有效的云链助手认证备份文件")
        }
        val accounts = root.optJSONArray("accounts") ?: return@withContext 0
        var count = 0
        for (i in 0 until accounts.length()) {
            val obj = accounts.optJSONObject(i) ?: continue
            when (obj.optString("platform")) {
                "quark" -> {
                    val c = obj.optString("cookie")
                    if (c.isNotBlank()) {
                        quarkDao.upsert(
                            QuarkAccountEntity(
                                id = "quark", cookie = c,
                                nickname = obj.optString("nickname"),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        ); count++
                    }
                }
                "uc" -> {
                    val c = obj.optString("cookie")
                    if (c.isNotBlank()) {
                        ucDao.upsert(
                            UCAccountEntity(
                                id = "uc", cookie = c,
                                nickname = obj.optString("nickname"),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        ); count++
                    }
                }
                "xunlei" -> {
                    val t = obj.optString("accessToken")
                    if (t.isNotBlank()) {
                        xunleiDao.upsert(
                            XunleiAccountEntity(
                                id = "xunlei", accessToken = t,
                                refreshToken = obj.optString("refreshToken"),
                                deviceId = obj.optString("deviceId"),
                                captchaToken = obj.optString("captchaToken"),
                                nickname = obj.optString("nickname"),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        ); count++
                    }
                }
            }
        }
        count
    }

    /**
     * 把备份内容保存到公共下载目录（Android 10+ 走 MediaStore 无需权限）。
     * @param encrypted true 时文件名为 .yunlian（加密备份），否则 .json（明文）
     */
    suspend fun saveToDownloads(context: Context, content: String, encrypted: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val fileName = if (encrypted) {
                    "yunlian_auth_backup_${timestamp()}.yunlian"
                } else {
                    "yunlian_auth_backup_${timestamp()}.json"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(
                            MediaStore.Downloads.MIME_TYPE,
                            if (encrypted) "application/octet-stream" else "application/json"
                        )
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri: Uri = context.contentResolver
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return@runCatching false
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    } ?: return@runCatching false
                    true
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
                    true
                }
            }.getOrDefault(false)
        }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}
