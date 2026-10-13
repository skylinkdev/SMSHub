package com.smshub.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 监听新短信广播：不直接解析 PDU 入库（广播里拿不到接收卡的 sub_id，
 * 直接入库会抢占去重，导致接收号码丢失），而是延迟后从收件箱增量捕获，
 * 由 SmsCapture 统一带上接收卡号码写入队列。
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // 借短信广播的豁免窗口拉起前台服务，保证监控持续在线
        MonitorService.start(context)
        Prefs(context).log("收到短信广播，准备从收件箱捕获")

        // 等系统把短信写入收件箱后再增量捕获（双保险：ContentObserver 也会触发）
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(1500)
                SmsCapture.check(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
