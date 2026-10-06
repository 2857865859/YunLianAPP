package com.yunlian.app.ui.login

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yunlian.app.data.network.XunleiConstants
import com.yunlian.app.ui.SnackbarController
import com.yunlian.app.ui.rememberGlobalSnackbarHostState
import com.yunlian.app.ui.TechSnackbarHost
import com.yunlian.app.ui.viewmodel.XunleiAccountViewModel
import kotlinx.coroutines.launch
import org.json.JSONTokener
import org.json.JSONObject

/** 官方网页登录优先；密码登录仅作为兼容入口。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun XunleiWebLoginScreen(
    viewModel: XunleiAccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onVerify: (url: String, deviceId: String) -> Unit = { _, _ -> }
) {
    var usePasswordLogin by remember { mutableStateOf(false) }
    if (usePasswordLogin) {
        XunleiLoginScreen(
            viewModel = viewModel,
            onBack = { usePasswordLogin = false },
            onSaved = onSaved,
            onVerify = onVerify
        )
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = rememberGlobalSnackbarHostState()
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = DESKTOP_USER_AGENT
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            setInitialScale(20)
            val loginWebView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(loginWebView, true)
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                    view?.postDelayed({
                        view.evaluateJavascript(
                            "(document.documentElement.scrollWidth - window.innerWidth).toString()"
                        ) { width ->
                            width.trim('"').toIntOrNull()?.let { view.scrollTo(it, 0) }
                        }
                    }, 1200)
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(XunleiConstants.WEB_LOGIN_URL)
        }
    }

    DisposableEffect(Unit) { onDispose { webView.destroy() } }
    BackHandler(enabled = !isSaving) { onBack() }

    Scaffold(
        snackbarHost = { TechSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("迅雷网盘登录", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { usePasswordLogin = true },
                        enabled = !isSaving
                    ) {
                        Icon(Icons.Outlined.Key, contentDescription = "账号密码登录")
                    }
                    TextButton(
                        enabled = !isSaving,
                        onClick = {
                            isSaving = true
                            webView.evaluateJavascript(XUNLEI_WEB_SESSION_SCRIPT) { raw ->
                                val session = parseXunleiWebSession(raw)
                                if (session == null) {
                                    isSaving = false
                                    SnackbarController.show("未检测到登录态，请先完成网页登录")
                                } else {
                                    scope.launch {
                                        val saved = viewModel.saveWebSession(
                                            session.accessToken,
                                            session.refreshToken,
                                            session.nickname
                                        )
                                        isSaving = false
                                        if (saved) {
                                            SnackbarController.show("登录成功")
                                            onSaved()
                                        } else {
                                            SnackbarController.show("登录凭证无效或已过期，请重新登录")
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("保存")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "登录成功后，请点击右上角「保存」完成登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private data class XunleiWebSession(
    val accessToken: String,
    val refreshToken: String,
    val nickname: String
)

private fun parseXunleiWebSession(raw: String): XunleiWebSession? = runCatching {
    val decoded = JSONTokener(raw).nextValue() as? String ?: return@runCatching null
    val json = JSONObject(decoded)
    val accessToken = json.optString("accessToken")
    if (accessToken.isBlank()) return@runCatching null
    XunleiWebSession(
        accessToken = accessToken,
        refreshToken = json.optString("refreshToken"),
        nickname = json.optString("nickname")
    )
}.getOrNull()

private const val XUNLEI_WEB_SESSION_SCRIPT = """
(function() {
  try {
    var root = window.${'$'}nuxt && window.${'$'}nuxt.${'$'}store && window.${'$'}nuxt.${'$'}store.state;
    var user = root && root.users && root.users.curUser;
    if (!user) return '';
    return JSON.stringify({
      accessToken: user.accessToken || user.access_token || '',
      refreshToken: user.refresh_token || user.refreshToken || '',
      nickname: user.nick_name || user.nickname || ''
    });
  } catch (e) { return ''; }
})()
"""

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
