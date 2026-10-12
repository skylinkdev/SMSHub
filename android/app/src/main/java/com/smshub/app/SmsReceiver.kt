package com.smshub.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.smshub.app.db.AppDatabase
import com.smshub.app.db.PendingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 监听新短信广播 → 合并多段短信 → 写入本地队列 → 触发上传。
 * 即使进程随后被回收，WorkManager 也会保证上传最终执行。
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        // 借短信广播的豁免窗口拉起前台服务，保证监控持续在线
        MonitorService.start(context)

        // 多段短信按发件人合并成一条
        val merged = parts
            .groupBy { it.originatingAddress ?: "unknown" }
            .map { (address, list) ->
                PendingMessage(
                    address = address,
                    body = list.joinToString("") { it.messageBody ?: "" },
                    date = list.first().timestampMillis,
                    type = "received"
                )
            }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.get(context).dao().insertAll(merged)
                UploadWorker.enqueue(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
