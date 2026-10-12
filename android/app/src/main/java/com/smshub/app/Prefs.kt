package com.smshub.app

import android.content.Context
import android.os.Build
import java.util.Locale

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("smshub", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString(KEY_SERVER_URL, "") ?: ""
        set(value) = sp.edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()

    var lastUploadAt: Long
        get() = sp.getLong(KEY_LAST_UPLOAD, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_UPLOAD, value).apply()

    var lastStatus: String
        get() = sp.getString(KEY_LAST_STATUS, "尚未上传") ?: "尚未上传"
        set(value) = sp.edit().putString(KEY_LAST_STATUS, value).apply()

    /** 已捕获的最新短信时间戳（ContentObserver 增量捕获用） */
    var lastSmsDate: Long
        get() = sp.getLong(KEY_LAST_SMS_DATE, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_SMS_DATE, value).apply()

    /** 追加一条诊断日志（保留最近 30 条） */
    fun log(event: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lines = (sp.getString(KEY_LOGS, "") ?: "").split("\n")
            .filter { it.isNotBlank() }.toMutableList()
        lines.add(0, "$ts $event")
        sp.edit().putString(KEY_LOGS, lines.take(30).joinToString("\n")).apply()
    }

    fun getLogs(): String = sp.getString(KEY_LOGS, "") ?: ""

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank()

    companion object {
        /** 全平台统一的设备标识：厂商 + 型号，首字母大写 */
        val deviceName: String
            get() = "${Build.MANUFACTURER} ${Build.MODEL}"
                .replaceFirstChar { it.uppercase(Locale.getDefault()) }

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_LAST_UPLOAD = "last_upload_at"
        private const val KEY_LAST_STATUS = "last_status"
        private const val KEY_LAST_SMS_DATE = "last_sms_date"
        private const val KEY_LOGS = "logs"
    }
}
