package com.smshub.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 开机后拉起监控服务并补传队列里的短信 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        MonitorService.start(context)
        UploadWorker.enqueue(context)
    }
}
