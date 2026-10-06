package com.yunlian.app.ui.login

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import com.yunlian.app.ui.SnackbarController
import com.yunlian.app.ui.rememberGlobalSnackbarHostState
import com.yunlian.app.ui.TechSnackbarHost
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yunlian.app.ui.viewmodel.XunleiAccountViewModel

/**
 * 迅雷网盘登录页：账号密码登录，触发安全验证后再输入短信验证码。
 * 短信无法发送时使用迅雷官方验证页完成应用内安全验证。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XunleiLoginScreen(
    viewModel: XunleiAccountViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onVerify: (url: String, deviceId: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val step = viewModel.loginStep
    val error = viewModel.loginError
    val smsSent = viewModel.smsSent
    // collectAsState 订阅账号：登录成功后 account 变非空，必触发重组 → 自动关闭登录页
    val account by viewModel.xunleiAccount.collectAsState()

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var verificationMobile by rememberSaveable { mutableStateOf("") }
    var smsCode by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val mobile = username.trim().takeIf { it.matches(Regex("""1\d{10}""")) }
        ?: verificationMobile.trim()

    // 登录错误提示
    LaunchedEffect(error) {
        error?.let {
            SnackbarController.show(it)
            viewModel.consumeLoginError()
        }
    }
    // 登录成功后自动关闭登录页（短信/密码任一方式成功，账号非空即关闭）
    LaunchedEffect(account) {
        if (account != null) onSaved()
    }

    BackHandler { onBack() }

    // 全局 Snackbar 宿主
    val snackbarHostState = rememberGlobalSnackbarHostState()
    val primaryLoginButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF263B75),
        contentColor = Color(0xFFEAF0FF),
        disabledContainerColor = Color(0xFF18213D),
        disabledContentColor = Color(0xFF7883A6)
    )
    val primaryLoginButtonBorder = BorderStroke(1.dp, Color(0xFF49629D))

    Scaffold(
        snackbarHost = { TechSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("迅雷网盘登录", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (step?.needSms == true) "短信验证" else "登录迅雷网盘",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Text(
                text = if (step?.needSms == true) {
                    if (smsSent) "账号密码登录触发安全验证，验证码已发送至 $mobile"
                    else "账号密码登录触发安全验证，请点击下方「发送验证码」"
                } else {
                    "使用迅雷账号登录，支持解析与下载分享文件"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (step == null || !step.needSms) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("手机号 / 邮箱") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    }
                )
                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = username.isNotBlank() && password.isNotBlank(),
                    colors = primaryLoginButtonColors,
                    border = primaryLoginButtonBorder
                ) { Text("登录") }
            } else {
                if (!username.trim().matches(Regex("""1\d{10}"""))) {
                    OutlinedTextField(
                        value = verificationMobile,
                        onValueChange = { verificationMobile = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("账号绑定的手机号") },
                        leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }
                OutlinedTextField(
                    value = smsCode,
                    onValueChange = { smsCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("短信验证码") },
                    leadingIcon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
                if (!smsSent) {
                    // 进入界面不会自动发送验证码：主按钮「发送验证码」提示用户主动获取
                    FilledTonalButton(
                        onClick = {
                            viewModel.sendSms(mobile)
                            SnackbarController.show("验证码已发送")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = mobile.matches(Regex("""1\d{10}"""))
                    ) { Text("发送验证码") }
                } else {
                    TextButton(
                        onClick = {
                            viewModel.sendSms(mobile)
                            SnackbarController.show("验证码已发送")
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("重新发送验证码") }
                }
                Button(
                    onClick = {
                        viewModel.loginWithSms(
                            mobile, smsCode,
                            step.smsCreditKey, step.smsToken
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = mobile.matches(Regex("""1\d{10}""")) && smsCode.isNotBlank(),
                    colors = primaryLoginButtonColors,
                    border = primaryLoginButtonBorder
                ) { Text("验证并登录") }
                Text(
                    text = "若始终收不到短信，请确认手机号正确，或稍后重试 / 切换网络",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // 短信发不出时的应用内验证兜底（应用内 WebView 承载验证页；核心验证仍走自有短信流）
                if (step.reviewUrl.isNotBlank()) {
                    TextButton(
                        onClick = {
                            // 用与登录请求一致的设备签名（deviceSign = div101.xxx）：
                            // 验证页会把 URL 里的 deviceid 原样当 devicesign 用，
                            // 必须与 v3/login 的 devicesign 字段一致，否则报"登录信息已过期"
                            onVerify(step.reviewUrl, com.yunlian.app.data.network.XunleiDeviceFingerprint.deviceSign())
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "短信收不到？应用内验证",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "应用内完成验证后，将自动重新登录",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://i.xunlei.com/xluser/validate/findpwd_acc.html")
                            )
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "未设置密码，点我前往设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

        }
    }
}
