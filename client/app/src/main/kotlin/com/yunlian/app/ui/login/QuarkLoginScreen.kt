package com.yunlian.app.ui.login

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import com.yunlian.app.ui.SnackbarController
import com.yunlian.app.ui.rememberGlobalSnackbarHostState
import com.yunlian.app.ui.TechSnackbarHost
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yunlian.app.data.network.QuarkConstants
import com.yunlian.app.ui.viewmodel.QuarkAccountViewModel
import kotlinx.coroutines.launch

/**
 * 夸克网盘登录页：
 * - 顶部标题栏：返回按钮 + 保存按钮（登录完成后点击，提取 Cookie 并校验保存）
 * - 主体：WebView 加载夸克网盘官网，由用户手动登录
 * - 进入页面时弹登录教程
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuarkLoginScreen(
    viewModel: QuarkAccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // 登录教程弹窗：进入页面即展示一次
    var showTutorial by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showTutorial = true }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            settings.setSupportZoom(true)          // 支持缩放
            settings.builtInZoomControls = true    // 启用内置缩放机制（双指缩放）
            settings.displayZoomControls = false   // 隐藏屏幕上的缩放按钮（只保留手势）

            settings.useWideViewPort = true        // 支持 viewport 标签
            settings.loadWithOverviewMode = true

            settings.userAgentString = QuarkConstants.USER_AGENT
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(QuarkConstants.LOGIN_URL)
        }
    }

    // 页面销毁时释放 WebView
    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }

    // 系统返回键 → 返回主页（保存中禁用）
    BackHandler(enabled = !isSaving) { onBack() }

    // 全局 Snackbar 宿主
    val snackbarHostState = rememberGlobalSnackbarHostState()

    Scaffold(
        snackbarHost = { TechSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("夸克网盘登录", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { if (!isSaving) onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val cookie = CookieManager.getInstance()
                                    .getCookie(QuarkConstants.COOKIE_DOMAIN)
                                    .orEmpty()
                                val saved = viewModel.saveQuarkAccount(cookie)
                                isSaving = false
                                if (saved) {
                                    SnackbarController.show("登录成功")
                                    onSaved()
                                } else {
                                    SnackbarController.show("未检测到登录态，请先完成登录")
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "扫码登录成功后，请点击右上角「保存」完成登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    // 登录教程弹窗
    if (showTutorial) {
        AlertDialog(
            onDismissRequest = { showTutorial = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("登录教程") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. 在下方网页中登录夸克账号",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "2. 登录完成后点右上角「保存」，自动提取 Cookie",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "3. Cookie 约 30 天有效，失效后需重新登录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorial = false }) { Text("知道了") }
            }
        )
    }

}
