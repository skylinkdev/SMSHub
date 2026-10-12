package com.smshub.app

import android.content.Context
import android.provider.Telephony
import com.smshub.app.db.AppDatabase
import com.smshub.app.db.PendingMessage

/**
 * 增量捕获：查询收件箱中时间戳晚于 lastSmsDate 的短信写入队列。
 * 首次调用只记录水位线，不回捞（回捞用「同步历史短信」按钮）。
 */
object SmsCapture {

    suspend fun check(context: Context) {
        val prefs = Prefs(context)
        val watermark = prefs.lastSmsDate
        val now = System.currentTimeMillis()
        if (watermark == 0L) {
            prefs.lastSmsDate = now
            prefs.log("监控初始化，水位线已设定")
            return
        }

        val list = mutableListOf<PendingMessage>()
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(watermark.toString()),
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < 50) {
                    list.add(
                        PendingMessage(
                            address = cursor.getString(0) ?: "unknown",
                            body = cursor.getString(1) ?: "",
                            date = cursor.getLong(2)
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            prefs.log("读取收件箱失败：${e.message}")
            return
        }

        if (list.isNotEmpty()) {
            AppDatabase.get(context).dao().insertAll(list)
            prefs.lastSmsDate = maxOf(list.maxOf { it.date }, watermark)
            prefs.log("捕获 ${list.size} 条新短信，加入上传队列")
            UploadWorker.enqueue(context)
        }
    }
}
