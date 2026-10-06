package com.yunlian.app.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.yunlian.app.data.network.XunleiConstants
import com.yunlian.app.ui.rememberGlobalSnackbarHostState
import com.yunlian.app.ui.TechSnackbarHost

private class XunleiVerifyBridge(
    private val isTrustedPage: () -> Boolean,
    private val callback: (Boolean, String) -> Unit
) {
    @JavascriptInterface
    fun onVerifyResult(resultJson: String) {
        if (!isTrustedPage()) return
        Handler(Looper.getMainLooper()).post { callback(true, resultJson) }
    }

    @JavascriptInterface
    fun close() {
        if (!isTrustedPage()) return
        Handler(Looper.getMainLooper()).post { callback(false, "user_closed") }
    }
}

private fun isTrustedXunleiUrl(url: String?): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (uri.scheme.equals("xlaccsdk01", ignoreCase = true)) return true
    if (!uri.scheme.equals("https", ignoreCase = true)) return false
    val host = uri.host?.lowercase() ?: return false
    return host == "xunlei.com" || host.endsWith(".xunlei.com") ||
        host == "xunlei.cn" || host.endsWith(".xunlei.cn")
}

/**
 * 迅雷验证页应用内承载（V3 · 实测修复）。
 *
 * 真正根因（反编译 vertifyPhone.js + 无头 Chromium 实测）：
 * 1) init 配置必须带非空 IFRAME_BOX_ID，否则 modifyConfig 里
 *    `"" == IFRAME_BOX_ID && !this.isMobileSDK()` 会调用包里根本不存在的
 *    isMobileSDK() 抛 TypeError → init 中止 → showPanel 永不执行 → 空白；
 * 2) 页面用 parseQueryString(location.href) 从 URL 读 deviceid（不读 init 配置），
 *    必须把 deviceid 拼进 URL，否则点"获取验证码"报 deviceid不能为空。
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("JavascriptInterface") // Bridge methods are annotated; lint cannot resolve the remembered concrete instance.
@Composable
fun XunleiVerifyWebViewScreen(
    verifyUrl: String,
    deviceId: String,
    onResult: (success: Boolean, extra: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var currentPageTrusted by remember { mutableStateOf(false) }

    val bridge = remember(onResult) {
        XunleiVerifyBridge(
            isTrustedPage = { currentPageTrusted },
            callback = onResult
        )
    }

    val webView = remember(verifyUrl) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
            settings.userAgentString = XunleiConstants.APP_UA
            addJavascriptInterface(bridge, "XLJSWebViewBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                    currentPageTrusted = isTrustedXunleiUrl(url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                    currentPageTrusted = isTrustedXunleiUrl(url)
                    // 核心修复：主动调用 window.XlCaptcha.init(config) 唤醒页面渲染
                    if (currentPageTrusted) {
                        view?.evaluateJavascript(buildInitScript(deviceId), null)
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url != null && url.startsWith("xlaccsdk01://")) {
                        onResult(true, url)
                        return true
                    }
                    return !isTrustedXunleiUrl(url)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString()
                    if (url != null && url.startsWith("xlaccsdk01://")) {
                        onResult(true, url)
                        return true
                    }
                    return !isTrustedXunleiUrl(url)
                }
            }
            webChromeClient = WebChromeClient()
            // 【修复点 1】页面用 parseQueryString(location.href) 读 deviceid，
            // 必须把 deviceid 拼进 URL，否则发短信会报 "deviceid不能为空"。
            val initialUrl = withDeviceId(verifyUrl, deviceId)
            if (isTrustedXunleiUrl(initialUrl)) {
                loadUrl(initialUrl)
            } else {
                post { onResult(false, "untrusted_verify_url") }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { webView.destroy() } }
    BackHandler { onBack() }
    val snackbarHostState = rememberGlobalSnackbarHostState()

    Scaffold(
        snackbarHost = { TechSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("迅雷安全验证", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * 把 deviceid 安全地拼到 reviewurl 后面。
 * 页面只认 URL 里的 deviceid（init 配置里的 deviceid 它不读）。
 */
private fun withDeviceId(url: String, deviceId: String): String {
    if (deviceId.isBlank()) return url
    val sep = if (url.contains('?')) '&' else '?'
    return "$url${sep}deviceid=${Uri.encode(deviceId)}"
}

/**
 * 核心修复脚本（V3）：
 * 1) 设置 window.env（顶层 WebView 中 window.parent === window，等效 parent.env）；
 * 2) 【关键】init 配置必须带 IFRAME_BOX_ID（非空），
 *    否则 modifyConfig 会执行 this.isMobileSDK() 抛错，init 中止 → 永久空白；
 * 3) 防御性注入 isMobileSDK 桩（即便未来版本不短路也不至于崩）；
 * 4) 轮询等待 window.XlCaptcha.init 就绪后调用，带 VERTIFYSUCCFUNC 回传成功结果；
 * 5) __xunleiInited 守卫避免重复 init。
 */
private fun buildInitScript(deviceId: String): String {
    val pkg = "ANDROID-com.xunlei.downloadprovider"   // 必须与 reviewurl 里的 appName 一致
    val cv = XunleiConstants.APP_CLIENT_VERSION      // 8.31.0.9726
    return """
        (function(){
          try {
            window.env = 'android';
            window.appid = '40';
            window.appName = '$pkg';
            window.clientVersion = '$cv';
            window.deviceid = '$deviceId';
            window.platformVersion = '10';
            window.event = 'login3';
          } catch(e) {}
          function fire(){
            if (window.__xunleiInited) return true;
            if (window.XlCaptcha && typeof window.XlCaptcha.init === 'function') {
              try {
                // 防御：补齐缺失的原生方法（包里未定义 isMobileSDK）
                if (typeof window.XlCaptcha.isMobileSDK !== 'function') {
                  window.XlCaptcha.isMobileSDK = function(){ return false; };
                }
                window.XlCaptcha.init({
                  appid: '40',
                  appName: '$pkg',
                  clientVersion: '$cv',
                  deviceid: '$deviceId',
                  event: 'login3',
                  platformVersion: '10',
                  IFRAME_BOX_ID: 'captch-wrap',
                  VERTIFYSUCCFUNC: function(res){
                    try { window.XLJSWebViewBridge.onVerifyResult(JSON.stringify(res || {})); } catch(e){}
                  }
                });
                window.__xunleiInited = true;
                return true;
              } catch(e) { console.error('XlCaptcha.init failed', e); }
            }
            return false;
          }
          if (!fire()) {
            var t = setInterval(function(){ if (fire()) { clearInterval(t); } }, 120);
            setTimeout(function(){ clearInterval(t); }, 10000);
          }
        })();
    """.trimIndent()
}
