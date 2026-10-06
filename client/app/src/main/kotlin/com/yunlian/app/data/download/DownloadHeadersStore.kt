package com.yunlian.app.data.download

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 使用 Android Keystore 加密下载恢复所需的 Cookie/Referer 等请求头。 */
class DownloadHeadersStore {
    fun encrypt(headers: Map<String, String>): String {
        if (headers.isEmpty()) return "{}"
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(DownloadHeadersJson.encode(headers).toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    fun decrypt(value: String): Map<String, String> {
        if (value.isBlank() || value == "{}") return emptyMap()
        if (!value.startsWith(PREFIX)) return emptyMap()
        return runCatching {
            val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
            require(payload.size > IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, payload.copyOfRange(0, IV_SIZE))
            )
            DownloadHeadersJson.decode(
                String(cipher.doFinal(payload.copyOfRange(IV_SIZE, payload.size)), Charsets.UTF_8)
            )
        }.getOrDefault(emptyMap())
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "yunx_download_headers_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFIX = "enc:v1:"
        const val IV_SIZE = 12
    }
}

internal object DownloadHeadersJson {
    fun encode(headers: Map<String, String>): String = JSONObject().apply {
        headers.forEach { (name, value) -> put(name, value) }
    }.toString()

    fun decode(json: String): Map<String, String> {
        val obj = JSONObject(json)
        return obj.keys().asSequence().associateWith { obj.getString(it) }
    }
}
