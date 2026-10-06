package com.yunlian.app

import android.app.Application
import com.yunlian.app.crash.CrashHandler
import com.yunlian.app.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YunlianApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        // 迅雷动态设备指纹：首次启动生成并持久化（开源分发后每台设备独立指纹）
        com.yunlian.app.data.network.XunleiDeviceFingerprint.init(this)
        // 进程被关闭时协程已不存在；冷启动后将遗留的等待中/下载中任务显示为已暂停，
        // 用户点击继续时再由 DownloadManager 完成当日验证并从 part 文件断点续传。
        applicationScope.launch {
            AppDatabase.get(this@YunlianApp).downloadTaskDao().markInterruptedAsPaused()
        }
    }
}
