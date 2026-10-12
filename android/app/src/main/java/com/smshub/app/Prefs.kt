package com.smshub.app

import android.content.Context

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("smshub", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString(KEY_SERVER_URL, "") ?: ""
        set(value) = sp.edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()

    var deviceKey: String
        get() = sp.getString(KEY_DEVICE_KEY, "") ?: ""
        set(value) = sp.edit().putString(KEY_DEVICE_KEY, value.trim()).apply()

    var lastUploadAt: Long
        get() = sp.getLong(KEY_LAST_UPLOAD, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_UPLOAD, value).apply()

    var lastStatus: String
        get() = sp.getString(KEY_LAST_STATUS, "尚未上传") ?: "尚未上传"
        set(value) = sp.edit().putString(KEY_LAST_STATUS, value).apply()

    var deviceName: String
        get() = sp.getString(KEY_DEVICE_NAME, "") ?: ""
        set(value) = sp.edit().putString(KEY_DEVICE_NAME, value).apply()

    /** 已完成注册时对应的服务器地址，不一致则需要重新注册 */
    var registeredUrl: String
        get() = sp.getString(KEY_REGISTERED_URL, "") ?: ""
        set(value) = sp.edit().putString(KEY_REGISTERED_URL, value).apply()

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

    /** 安装标识：首次读取时生成 UUID，重装清零 */
    val installId: String
        get() {
            var id = sp.getString(KEY_INSTALL_ID, null)
            if (id.isNullOrBlank()) {
                id = java.util.UUID.randomUUID().toString()
                sp.edit().putString(KEY_INSTALL_ID, id).apply()
            }
            return id
        }

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && deviceKey.isNotBlank()

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_KEY = "device_key"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_INSTALL_ID = "install_id"
        private const val KEY_REGISTERED_URL = "registered_url"
        private const val KEY_LAST_UPLOAD = "last_upload_at"
        private const val KEY_LAST_STATUS = "last_status"
        private const val KEY_LAST_SMS_DATE = "last_sms_date"
        private const val KEY_LOGS = "logs"
    }
}
