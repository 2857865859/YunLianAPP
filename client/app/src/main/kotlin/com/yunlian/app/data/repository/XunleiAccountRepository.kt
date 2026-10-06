package com.yunlian.app.data.repository

import com.yunlian.app.data.db.XunleiAccountDao
import com.yunlian.app.data.db.XunleiAccountEntity
import com.yunlian.app.data.network.XunleiApi
import com.yunlian.app.data.network.XunleiLoginStep
import kotlinx.coroutines.flow.Flow

/**
 * 迅雷账号仓库：账号+密码登录（可能触发短信验证）→ 换 token 落库。
 */
class XunleiAccountRepository(
    private val dao: XunleiAccountDao,
    private val api: XunleiApi
) {

    fun observeAccount(): Flow<XunleiAccountEntity?> = dao.observeAccount()

    suspend fun getAccount(): XunleiAccountEntity? = dao.getAccount()

    /** 保存官方网页登录页签发的 Token，不接触用户账号密码。 */
    suspend fun saveWebSession(
        accessToken: String,
        refreshToken: String,
        nickname: String
    ): Boolean {
        if (accessToken.isBlank() || api.jwtExp(accessToken) <= System.currentTimeMillis() / 1000) {
            return false
        }
        val deviceId = XunleiApi.newDeviceId()
        api.cacheUserId(accessToken)
        val captchaToken = api.initCaptcha(
            deviceId = deviceId,
            username = "",
            action = "GET:/drive/v1/about"
        ).orEmpty()
        dao.upsert(
            XunleiAccountEntity(
                accessToken = accessToken,
                refreshToken = refreshToken,
                deviceId = deviceId,
                captchaToken = captchaToken,
                nickname = nickname.ifBlank { "迅雷用户" }
            )
        )
        return true
    }

    /** 账号密码登录；返回登录步骤（needSms=true 表示需短信验证，携带 smsCreditKey/smsToken） */
    suspend fun loginWithPassword(
        username: String,
        password: String
    ): XunleiLoginStep {
        // 必须用官方真实设备 ID（devicesign 配套），否则 userinfo_expired
        val deviceId = XunleiApi.newDeviceId()
        // 官方首次登录：v3/login 不带任何 creditkey/captcha，返回 review_panel(1007) 触发短信验证
        return api.loginWithPassword(username, password, deviceId)
    }

    /** 发送短信验证码（登录触发 review_panel 后调用） */
    suspend fun sendSms(mobile: String): XunleiLoginStep {
        val deviceId = XunleiApi.newDeviceId()
        return api.sendSms(mobile, deviceId)
    }

    /** 短信验证码登录并换取 token，成功落库返回 true */
    suspend fun loginWithSms(
        mobile: String,
        smsCode: String,
        creditKey: String,
        smsToken: String
    ): Boolean {
        val deviceId = XunleiApi.newDeviceId()
        val step = api.smsLogin(mobile, smsCode, creditKey, smsToken, deviceId)
        if (step.sessionId.isBlank()) return false
        // 换 token 前先 initCaptcha 拿 captcha_token（官方时序：smslogin → captcha/init → signin/token）
        val captchaToken = api.initCaptcha(deviceId, mobile) ?: ""
        val tokens = api.exchangeToken(step.sessionId, deviceId, captchaToken) ?: return false
        val nickname = api.fetchNickname(tokens.first, deviceId)
            ?: step.nickname.takeUnless { it.contains("****") }
            ?: "迅雷用户"
        dao.upsert(
            XunleiAccountEntity(
                id = "xunlei",
                accessToken = tokens.first,
                refreshToken = tokens.second,
                deviceId = deviceId,
                captchaToken = captchaToken,
                nickname = nickname
            )
        )
        return true
    }

    /** 密码登录成功后用 sessionID 换 token 落库 */
    suspend fun finishLogin(
        step: XunleiLoginStep,
        username: String
    ): Boolean {
        if (step.sessionId.isBlank()) return false
        val deviceId = XunleiApi.newDeviceId()
        val captchaToken = api.initCaptcha(deviceId, username) ?: ""
        val tokens = api.exchangeToken(step.sessionId, deviceId, captchaToken) ?: return false
        val nickname = api.fetchNickname(tokens.first, deviceId)
            ?: step.nickname.takeUnless { it.contains("****") }
            ?: "迅雷用户"
        dao.upsert(
            XunleiAccountEntity(
                id = "xunlei",
                accessToken = tokens.first,
                refreshToken = tokens.second,
                deviceId = deviceId,
                captchaToken = captchaToken,
                nickname = nickname
            )
        )
        return true
    }

    /** 刷新 token 后更新 accessToken/refreshToken（deviceId/captchaToken 保持不变） */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        val acc = dao.getAccount() ?: return
        dao.upsert(acc.copy(accessToken = accessToken, refreshToken = refreshToken))
    }

    /** 修正旧版本短信登录保存的脱敏手机号，并同步最新迅雷昵称。 */
    suspend fun refreshNickname() {
        val account = dao.getAccount() ?: return
        val nickname = api.fetchNickname(account.accessToken, account.deviceId)
            ?: if (account.nickname.contains("****")) "迅雷用户" else return
        if (nickname != account.nickname) dao.upsert(account.copy(nickname = nickname))
    }

    suspend fun logout() {
        dao.clear()
    }
}
