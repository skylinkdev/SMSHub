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

    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && deviceKey.isNotBlank()

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_KEY = "device_key"
        private const val KEY_LAST_UPLOAD = "last_upload_at"
        private const val KEY_LAST_STATUS = "last_status"
    }
}
