package com.yunlian.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yunlian.app.data.network.QuarkApi
import com.yunlian.app.data.network.UCApi
import com.yunlian.app.data.network.XunleiApi
import com.yunlian.app.data.network.model.QuotaInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 网盘空间详情 ViewModel：并发加载夸克、UC、迅雷的容量使用情况。
 * 网盘页顶部「空间总览」展示用。
 */
class DriveQuotaViewModel(
    private val quarkApi: QuarkApi,
    private val quarkCookie: suspend () -> String?,
    private val ucApi: UCApi,
    private val ucCookie: suspend () -> String?,
    private val xunleiApi: XunleiApi,
    private val xunleiToken: suspend () -> String?,
    private val xunleiDeviceId: suspend () -> String?,
    private val xunleiCaptcha: suspend () -> String?
) : ViewModel() {

    private val _quarkQuota = MutableStateFlow<QuotaInfo?>(null)
    val quarkQuota: StateFlow<QuotaInfo?> = _quarkQuota.asStateFlow()

    private val _ucQuota = MutableStateFlow<QuotaInfo?>(null)
    val ucQuota: StateFlow<QuotaInfo?> = _ucQuota.asStateFlow()

    private val _xunleiQuota = MutableStateFlow<QuotaInfo?>(null)
    val xunleiQuota: StateFlow<QuotaInfo?> = _xunleiQuota.asStateFlow()

    /** 是否加载中 */
    val loading = MutableStateFlow(false)

    /** 并发加载全部已登录平台的空间（各平台独立请求，互不阻塞；未登录平台自动跳过） */
    fun loadAll() {
        if (loading.value) return // 防止下拉刷新与进入页面初始化重复触发
        loading.value = true
        viewModelScope.launch {
            coroutineScope {
                // 夸克
                launch {
                    val qc = quarkCookie()
                    if (qc != null) {
                        _quarkQuota.value = runCatching { quarkApi.getQuota(qc) }.getOrNull()
                    }
                }
                // UC
                launch {
                    val uc = ucCookie()
                    if (uc != null) {
                        _ucQuota.value = runCatching { ucApi.getQuota(uc) }.getOrNull()
                    }
                }
                // 迅雷
                launch {
                    val xl = xunleiToken()
                    if (xl != null) {
                        val deviceId = xunleiDeviceId() ?: ""
                        val captcha = xunleiCaptcha() ?: ""
                        _xunleiQuota.value = runCatching { xunleiApi.getQuota(xl, deviceId, captcha) }.getOrNull()
                    }
                }
            }
            loading.value = false
        }
    }

    class Factory(
        private val quarkApi: QuarkApi,
        private val quarkCookie: suspend () -> String?,
        private val ucApi: UCApi,
        private val ucCookie: suspend () -> String?,
        private val xunleiApi: XunleiApi,
        private val xunleiToken: suspend () -> String?,
        private val xunleiDeviceId: suspend () -> String?,
        private val xunleiCaptcha: suspend () -> String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DriveQuotaViewModel(
                quarkApi, quarkCookie,
                ucApi, ucCookie,
                xunleiApi, xunleiToken, xunleiDeviceId, xunleiCaptcha
            ) as T
    }
}
