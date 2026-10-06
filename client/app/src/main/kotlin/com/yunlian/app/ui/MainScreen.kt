package com.yunlian.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.yunlian.app.ui.theme.TechBackground
import com.yunlian.app.ui.theme.TechCyan
import com.yunlian.app.ui.theme.TechPurple
import com.yunlian.app.ui.theme.TechSurface
import com.yunlian.app.ui.theme.TechTextPrimary
import com.yunlian.app.ui.theme.TechTextSecondary
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.res.Configuration
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.yunlian.app.data.db.AppDatabase
import com.yunlian.app.data.db.DownloadTaskEntity
import com.yunlian.app.data.download.ChunkDownloader
import com.yunlian.app.data.download.DownloadManager
import com.yunlian.app.data.network.QuarkApi
import com.yunlian.app.data.network.UCApi
import com.yunlian.app.data.network.XunleiApi
import com.yunlian.app.data.prefs.SettingsRepository
import com.yunlian.app.data.repository.QuarkAccountRepository
import com.yunlian.app.data.repository.QuarkResolveRepository
import com.yunlian.app.data.repository.UCAccountRepository
import com.yunlian.app.data.repository.UCResolveRepository
import com.yunlian.app.data.repository.XunleiAccountRepository
import com.yunlian.app.data.repository.XunleiResolveRepository
import com.yunlian.app.ui.login.QuarkLoginScreen
import com.yunlian.app.ui.login.UCLoginScreen
import com.yunlian.app.ui.login.XunleiLoginScreen
import com.yunlian.app.ui.login.XunleiVerifyWebViewScreen
import com.yunlian.app.ui.navigation.MainTab
import com.yunlian.app.ui.screens.AboutScreen
import com.yunlian.app.ui.screens.DownloadScreen
import com.yunlian.app.ui.screens.DriveScreen
import com.yunlian.app.ui.screens.ResolveScreen
import com.yunlian.app.ui.screens.SettingsScreen
import com.yunlian.app.ui.viewmodel.DownloadViewModel
import com.yunlian.app.ui.viewmodel.DriveQuotaViewModel
import com.yunlian.app.ui.viewmodel.QuarkAccountViewModel
import com.yunlian.app.ui.viewmodel.QuarkCloudViewModel
import com.yunlian.app.ui.viewmodel.ResolveViewModel
import com.yunlian.app.ui.viewmodel.UCCoudViewModel
import com.yunlian.app.ui.viewmodel.UCAccountViewModel
import com.yunlian.app.ui.viewmodel.XunleiAccountViewModel
import com.yunlian.app.ui.viewmodel.XunleiCloudViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yunlian.app.data.network.HttpClients

/**
 * 主页框架：
 * - 四个主页面使用各自的页面内科技横幅；
 * - 导航 Tab（解析 / 网盘 / 下载 / 设置）：竖屏为底部导航栏（NavigationBar），横屏切换为侧边导航栏（NavigationRail）；
 * - 通过 SaveableStateHolder 保存各页面状态，切换 Tab 再切回来不会重置；
 * - 夸克登录页全屏覆盖展示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(downloadAuthorizer: suspend () -> Boolean = { true }) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Resolve) }
    var showQuarkLogin by rememberSaveable { mutableStateOf(false) }
    var showUCLogin by rememberSaveable { mutableStateOf(false) }
    var showXunleiLogin by rememberSaveable { mutableStateOf(false) }
    var showXunleiVerify by rememberSaveable { mutableStateOf(false) }
    var xunleiVerifyUrl by rememberSaveable { mutableStateOf("") }
    var xunleiVerifyDeviceId by rememberSaveable { mutableStateOf("") }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val saveableStateHolder = rememberSaveableStateHolder()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 横屏时使用侧边导航栏（NavigationRail），竖屏保持底部导航栏（NavigationBar）
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val api = remember { QuarkApi() }
    val ucApi = remember { UCApi() }
    val xunleiApi = remember { XunleiApi() }
    val db = remember { AppDatabase.get(context) }
    val settings = remember { SettingsRepository(context) }
    // 启动时同步「忽略 SSL 证书」开关（设置页隐藏菜单持久化，全局客户端即时生效）
    LaunchedEffect(Unit) { HttpClients.ignoreSsl = settings.ignoreSslCert }
    val repository = remember {
        QuarkAccountRepository(db.quarkAccountDao(), api)
    }
    val quarkResolveRepository = remember { QuarkResolveRepository(api) }
    val ucRepository = remember {
        UCAccountRepository(db.ucAccountDao(), ucApi)
    }
    val xunleiRepository = remember {
        XunleiAccountRepository(db.xunleiAccountDao(), xunleiApi)
    }
    // Android 13+：只在用户首次开始下载时申请通知权限，避免冷启动无上下文弹窗。
    var pendingNotificationPermission by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingNotificationPermission?.complete(granted)
        pendingNotificationPermission = null
    }
    val requestDownloadNotificationPermission: suspend () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            val deferred = CompletableDeferred<Boolean>()
            pendingNotificationPermission = deferred
            withContext(Dispatchers.Main) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            deferred.await()
        }
    }
    // 下载管理器：OkHttp 分片下载器 + Room 任务持久化 + 可配置线程数（设置页动态生效）
    // 下载客户端由全局 HttpClients 统一管理（大 Dispatcher 保障分片并发，不锁死 CDN host；
    // 并支持隐藏菜单「忽略 SSL 证书」开关，抓包调试时即时生效，无需重启）
    val downloadManager = remember {
        DownloadManager(
            context = context,
            dao = db.downloadTaskDao(),
            downloader = ChunkDownloader({ HttpClients.downloadClient() }),
            threadProvider = settings::downloadThreads,
            // 自定义下载保存目录（SAF tree Uri），设置页可选，动态生效
            saveDirProvider = { settings.downloadDirUri },
            // 网络与下载策略（设置页可调，动态生效）：并发任务数 / 全局限速 / 失败重试
            concurrencyProvider = { settings.maxConcurrentDownloads },
            speedLimitProvider = { settings.downloadSpeedLimit },
            retryCountProvider = { settings.downloadRetryCount },
            // 锁屏保持下载 / 通知栏速度开关
            keepWhenLockedProvider = { settings.keepDownloadWhenLocked },
            showSpeedProvider = { settings.notificationShowSpeed },
            authorizationProvider = {
                requestDownloadNotificationPermission()
                downloadAuthorizer()
            },
            persistentCleanupHandler = { cleanupId ->
                repository.getFreshCookie()?.let { cookie ->
                    quarkResolveRepository.cleanupTempDir(cleanupId, cookie)
                } ?: throw IllegalStateException("夸克登录已失效，无法清理临时目录")
            }
        )
    }
    // Android 9- 写公共 Download 需要 WRITE_EXTERNAL_STORAGE 运行时授权：
    // 下载完成保存前由 DownloadManager.storagePermissionProvider 触发动态申请，授权后自动继续保存
    var pendingStoragePermission by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingStoragePermission?.complete(granted)
        pendingStoragePermission = null
    }
    downloadManager.storagePermissionProvider = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Android 10+ MediaStore 无需存储权限
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            val deferred = CompletableDeferred<Boolean>()
            pendingStoragePermission = deferred
            withContext(Dispatchers.Main) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            deferred.await()
        }
    }
    val viewModel: QuarkAccountViewModel = viewModel(
        factory = QuarkAccountViewModel.Factory(repository)
    )
    val ucViewModel: UCAccountViewModel = viewModel(
        factory = UCAccountViewModel.Factory(ucRepository)
    )
    val xunleiViewModel: XunleiAccountViewModel = viewModel(
        factory = XunleiAccountViewModel.Factory(xunleiRepository)
    )
    // 夸克云盘浏览：作为网盘 Tab 内容展示（非全屏），cookie 从数据库读取（避免 StateFlow 初始值为空的竞态）；
    // 下载前经 getFreshCookie 惰性刷新 __puus（修复 AlistGo/alist#830 下载 412）
    val quarkCloudViewModel: QuarkCloudViewModel = viewModel(
        factory = QuarkCloudViewModel.Factory(
            api,
            { repository.getFreshCookie() },
            downloadManager
        )
    )
    // UC 网盘云盘浏览：点击已登录的 UC 卡片打开（cookie 从数据库读取）；
    // 取链前经 getFreshCookie 惰性刷新 __puus（与夸克同源，修复取链/直链过期失败）
    val ucCloudViewModel: UCCoudViewModel = viewModel(
        factory = UCCoudViewModel.Factory(
            ucApi,
            { ucRepository.getFreshCookie() },
            downloadManager
        )
    )
    // 迅雷 access_token 过期（401 unauthenticated）自动刷新：refresh_token 换新并持久化（对齐官方 /v1/auth/token 抓包）
    xunleiApi.refreshTokenProvider = { deviceId ->
        val acc = xunleiRepository.getAccount()
        if (acc == null || acc.refreshToken.isBlank()) null
        else xunleiApi.refreshToken(acc.refreshToken, deviceId)?.also { (at, nrt) ->
            xunleiRepository.updateTokens(at, nrt)
        }
    }
    // 迅雷云盘浏览：点击已登录的迅雷卡片打开（access_token/设备指纹/captcha 从数据库读取）
    val xunleiCloudViewModel: XunleiCloudViewModel = viewModel(
        factory = XunleiCloudViewModel.Factory(
            xunleiApi,
            { xunleiRepository.getAccount()?.accessToken },
            { xunleiRepository.getAccount()?.deviceId },
            { xunleiRepository.getAccount()?.captchaToken },
            downloadManager
        )
    )
    // 网盘空间详情：网盘页顶部「空间总览」展示 6 平台容量使用
    val driveQuotaViewModel: DriveQuotaViewModel = viewModel(
        factory = DriveQuotaViewModel.Factory(
            api, { repository.getAccount()?.cookie },
            ucApi, { ucRepository.getAccount()?.cookie },
            xunleiApi,
            { xunleiRepository.getAccount()?.accessToken },
            { xunleiRepository.getAccount()?.deviceId },
            { xunleiRepository.getAccount()?.captchaToken }
        )
    )
    val xunleiResolveRepository = remember {
        XunleiResolveRepository(
            api = xunleiApi,
            accountProvider = { xunleiRepository.getAccount()?.accessToken },
            deviceIdProvider = { xunleiRepository.getAccount()?.deviceId },
            captchaProvider = { xunleiRepository.getAccount()?.captchaToken },
            // token 过期（含导入恢复后旧 token 过期）自动用 refresh_token 刷新并持久化
            refreshProvider = {
                val acc = xunleiRepository.getAccount()
                if (acc == null || acc.refreshToken.isBlank()) null
                else xunleiApi.refreshToken(acc.refreshToken, acc.deviceId)?.also { (at, nrt) ->
                    xunleiRepository.updateTokens(at, nrt)
                }
            }
        )
    }
    val resolveViewModel: ResolveViewModel = viewModel(
        factory = ResolveViewModel.Factory(
            repository,
            quarkResolveRepository,
            ucRepository,
            UCResolveRepository(ucApi),
            xunleiRepository,
            xunleiResolveRepository,
            downloadManager
        )
    )
    val downloadViewModel: DownloadViewModel = viewModel(
        factory = DownloadViewModel.Factory(downloadManager)
    )
    val quarkAccount by viewModel.quarkAccount.collectAsState()
    val ucAccount by ucViewModel.ucAccount.collectAsState()
    val xunleiAccount by xunleiViewModel.xunleiAccount.collectAsState()
    LaunchedEffect(xunleiAccount?.accessToken) {
        if (xunleiAccount != null) xunleiCloudViewModel.loadRoot()
    }

    // 首次下载引导：锁屏保持下载默认开启，但新用户未加入「忽略电池优化」白名单 →引导一次
    var showBatteryGuide by remember { mutableStateOf(false) }
    var batteryGuideShown by remember { mutableStateOf(false) }

    // 解析页发起下载后，自动切换到「下载」Tab
    LaunchedEffect(resolveViewModel.downloadStarted) {
        if (resolveViewModel.downloadStarted) {
            currentTab = MainTab.Download
            resolveViewModel.consumeDownloadStarted()
        }
    }

    // 首次下载任务启动：锁屏保持下载默认开启但未豁免电池优化 →引导一次。
    // 监听任务状态而非 downloadStarted，覆盖解析页/网盘页/手动添加等所有下载入口。
    LaunchedEffect(Unit) {
        downloadViewModel.tasks.collect { tasks ->
            if (!batteryGuideShown && tasks.any {
                    it.status == DownloadTaskEntity.STATUS_DOWNLOADING ||
                        it.status == DownloadTaskEntity.STATUS_PENDING
                }
            ) {
                batteryGuideShown = true
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (settings.keepDownloadWhenLocked &&
                    pm?.isIgnoringBatteryOptimizations(context.packageName) != true
                ) {
                    showBatteryGuide = true
                }
            }
        }
    }

    // 夸克登录页：全屏覆盖
    if (showQuarkLogin) {
        QuarkLoginScreen(
            viewModel = viewModel,
            onBack = { showQuarkLogin = false },
            onSaved = { showQuarkLogin = false }
        )
        return
    }

    // UC 登录页：全屏覆盖
    if (showUCLogin) {
        UCLoginScreen(
            viewModel = ucViewModel,
            onBack = { showUCLogin = false },
            onSaved = { showUCLogin = false }
        )
        return
    }

    // 迅雷登录页：账号密码登录，触发安全验证后进入短信验证码流程
    if (showXunleiLogin) {
        XunleiLoginScreen(
            viewModel = xunleiViewModel,
            onBack = { showXunleiLogin = false },
            onSaved = { showXunleiLogin = false },
            onVerify = { url, deviceId ->
                // 应用内验证：登录页让位，切到验证 WebView 全屏承载（不再跳外部浏览器）
                xunleiVerifyUrl = url
                xunleiVerifyDeviceId = deviceId
                showXunleiLogin = false
                showXunleiVerify = true
            }
        )
        return
    }

    // 迅雷验证页（应用内 WebView 承载验证面板）：全屏覆盖（兜底承载，核心验证仍走自有短信流）
    if (showXunleiVerify) {
        XunleiVerifyWebViewScreen(
            verifyUrl = xunleiVerifyUrl,
            deviceId = xunleiVerifyDeviceId,
            onResult = { success, _ ->
                showXunleiVerify = false
                showXunleiLogin = true // 回到登录页
                if (success) {
                    // 设备已验证受信任：自动重试账号密码登录
                    SnackbarController.show("验证完成，正在自动登录…")
                    xunleiViewModel.retryLoginAfterVerify()
                } else {
                    SnackbarController.show("验证未完成，请重试")
                }
            },
            onBack = {
                showXunleiVerify = false
                showXunleiLogin = true // 返回登录页短信步骤
            }
        )
        return
    }

    // 折叠标题状态提升到本层：跨页面共享，页面切换时折叠/展开状态保持不变
    // 用 exitUntilCollapsed（默认实现，含松手吸附）：滚动时标题先收起再滚内容；
    // 向上滚动回顶部过程中标题保持收起，只有列表到达最顶部后继续下拉（overscroll）才重新展开
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    // 全局 Snackbar 宿主（Material3，替换原 Toast 提示）
    val snackbarHostState = rememberGlobalSnackbarHostState()

    // 主框架与全屏覆盖层（关于页）放在同一 Box：覆盖层带过渡动画
    Box(modifier = Modifier.fillMaxSize()) {
    // 四个主页面均使用页面内科技横幅，不再显示独立大标题栏。
    val topBarContent: @Composable () -> Unit = {}
    // Tab 内容区（竖屏 / 横屏共用）：每个页面独立保存状态，切换 Tab 再切回来不丢失；带 Material3 过渡动画（按 Tab 顺序决定方向）
    val tabContent: @Composable () -> Unit = {
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                // 根据 Tab 顺序决定滑动方向：向右切（新Tab在右边）→ 新页从右滑入；向左切反向
                val forward = targetState.ordinal > initialState.ordinal
                if (forward) {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 4 })
                        .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -it / 4 })
                } else {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 4 })
                        .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { it / 4 })
                }
            },
            label = "mainTab"
        ) { tab ->
            saveableStateHolder.SaveableStateProvider(tab) {
                when (tab) {
                    MainTab.Resolve -> ResolveScreen(
                        scrollBehavior,
                        resolveViewModel,
                        quarkCloudViewModel,
                        xunleiCloudViewModel,
                        ucCloudViewModel
                    )
                    MainTab.Drive -> DriveScreen(
                        scrollBehavior = scrollBehavior,
                        quarkAccount = quarkAccount,
                        ucAccount = ucAccount,
                        xunleiAccount = xunleiAccount,
                        quarkCloudViewModel = quarkCloudViewModel,
                        ucCloudViewModel = ucCloudViewModel,
                        xunleiCloudViewModel = xunleiCloudViewModel,
                        driveQuotaViewModel = driveQuotaViewModel,
                        onQuarkLogin = { showQuarkLogin = true },
                        onQuarkLogout = { viewModel.logout() },
                        onDownloadStarted = { currentTab = MainTab.Download },
                        onUCLogin = { showUCLogin = true },
                        onUCLogout = { ucViewModel.logout() },
                        onXunleiLogin = { showXunleiLogin = true },
                        onXunleiLogout = { xunleiViewModel.logout() }
                    )
                    MainTab.Download -> DownloadScreen(downloadViewModel)
                    MainTab.Settings -> SettingsScreen(
                        scrollBehavior = scrollBehavior,
                        downloadThreads = settings.downloadThreads,
                        onThreadsChange = { settings.downloadThreads = it },
                        onAboutClick = { showAbout = true }
                    )
                }
            }
        }
    }

    if (isLandscape) {
        // 横屏：左侧侧边导航栏（NavigationRail）+ 右侧顶栏 & 内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 竖屏由 Scaffold 提供主题背景；横屏手动布局需显式设置，否则露出窗口默认白色
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                MainNavigationRail(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    topBarContent()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        tabContent()
                    }
                }
            }
            // 全局 Snackbar（横屏无底部栏，悬浮底部居中）
            TechSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    } else {
        // 竖屏：Scaffold + 底部导航栏
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = TechBackground,
            snackbarHost = { TechSnackbarHost(snackbarHostState) },
            topBar = { topBarContent() },
            bottomBar = {
                MainBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                tabContent()
            }
        }
    }

    // 关于云析：叠加覆盖层（淡入 + 轻微缩放过渡）
    AnimatedVisibility(
        visible = showAbout,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
        exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f),
        modifier = Modifier.fillMaxSize()
    ) {
        AboutScreen(
            onBack = { showAbout = false }
        )
    }

    }

    // 首次下载引导：加入「忽略电池优化」白名单（锁屏保持下载生效的前提）
    if (showBatteryGuide) {
        AlertDialog(
            onDismissRequest = { showBatteryGuide = false },
            title = { Text("保持后台下载") },
            text = {
                Text(
                    text = "「锁屏后保持下载」已开启，但应用尚未加入「忽略电池优化」白名单，息屏后可能被系统中断下载。是否前往系统设置？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryGuide = false
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    }
                ) { Text("前往设置") }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryGuide = false }) { Text("暂不") }
            }
        )
    }

}

/**
 * 底部导航栏（竖屏）：4 个主 Tab（解析 / 网盘 / 下载 / 设置）。
 */
@Composable
private fun MainBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = TechBackground,
        tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
    ) {
        MainTab.values().forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title
                    )
                },
                label = { Text(tab.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TechCyan,
                    selectedTextColor = TechTextPrimary,
                    indicatorColor = TechPurple.copy(alpha = .28f),
                    unselectedIconColor = TechTextSecondary,
                    unselectedTextColor = TechTextSecondary
                )
            )
        }
    }
}

/**
 * 侧边导航栏（横屏）：同 4 个主 Tab，未选中项只显示图标，节省横向空间。
 */
@Composable
private fun MainNavigationRail(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationRail {
        MainTab.values().forEach { tab ->
            NavigationRailItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title
                    )
                },
                label = { Text(tab.title) },
                alwaysShowLabel = currentTab == tab
            )
        }
    }
}
