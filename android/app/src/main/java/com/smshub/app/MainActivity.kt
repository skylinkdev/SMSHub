package com.smshub.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.smshub.app.db.AppDatabase
import com.smshub.app.db.PendingMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshPermissionStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        val etServerUrl = findViewById<TextInputEditText>(R.id.etServerUrl)
        val etDeviceKey = findViewById<TextInputEditText>(R.id.etDeviceKey)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvPending = findViewById<TextView>(R.id.tvPending)
        val tvPermission = findViewById<TextView>(R.id.tvPermission)

        etServerUrl.setText(prefs.serverUrl)
        etDeviceKey.setText(prefs.deviceKey)

        fun saveConfig(): Boolean {
            prefs.serverUrl = etServerUrl.text?.toString().orEmpty()
            prefs.deviceKey = etDeviceKey.text?.toString().orEmpty()
            if (!prefs.isConfigured) {
                Toast.makeText(this, "请填写服务器地址和设备密钥", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        // 保存配置
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            if (saveConfig()) {
                Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
            }
        }

        // 测试连接
        findViewById<Button>(R.id.btnPing).setOnClickListener {
            if (!saveConfig()) return@setOnClickListener
            tvStatus.text = "测试中…"
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    ApiClient.ping(prefs.serverUrl, prefs.deviceKey)
                }
                tvStatus.text = when (result) {
                    is ApiClient.Result.Success -> result.message
                    is ApiClient.Result.RetryableError -> result.message
                    is ApiClient.Result.FatalError -> result.message
                }
            }
        }

        // 申请短信权限
        findViewById<Button>(R.id.btnPermission).setOnClickListener {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        }

        // 同步历史短信（最近 200 条收件箱）
        findViewById<Button>(R.id.btnSyncHistory).setOnClickListener {
            if (!saveConfig()) return@setOnClickListener
            lifecycleScope.launch {
                val count = withContext(Dispatchers.IO) { syncHistory() }
                Toast.makeText(
                    this@MainActivity,
                    if (count >= 0) "已加入上传队列：$count 条" else "请先授予读取短信权限",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // 忽略电池优化（保证后台稳定）
        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        // 待上传数量实时刷新
        lifecycleScope.launch {
            AppDatabase.get(this@MainActivity).dao().countFlow().collect { count ->
                tvPending.text = "待上传：$count 条"
            }
        }

        refreshPermissionStatus()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
        refreshStatus()
    }

    private fun refreshStatus() {
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val last = if (prefs.lastUploadAt > 0)
            "最近上传：${timeFmt.format(Date(prefs.lastUploadAt))}"
        else "尚未上传"
        tvStatus.text = "$last\n${prefs.lastStatus}"
    }

    private fun refreshPermissionStatus() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        findViewById<TextView>(R.id.tvPermission).text =
            if (granted) "短信权限：已授予" else "短信权限：未授予（点击上方按钮申请）"
    }

    /** 读取收件箱最近 200 条写入队列，服务端按唯一键去重 */
    private fun syncHistory(): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return -1

        val list = mutableListOf<PendingMessage>()
        contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            ),
            null, null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            var count = 0
            while (cursor.moveToNext() && count < 200) {
                list.add(
                    PendingMessage(
                        address = cursor.getString(0) ?: "unknown",
                        body = cursor.getString(1) ?: "",
                        date = cursor.getLong(2),
                        type = if (cursor.getInt(3) == Telephony.Sms.MESSAGE_TYPE_SENT)
                            "sent" else "received"
                    )
                )
                count++
            }
        }
        if (list.isNotEmpty()) {
            kotlinx.coroutines.runBlocking {
                AppDatabase.get(this@MainActivity).dao().insertAll(list)
            }
            UploadWorker.enqueue(this)
        }
        return list.size
    }
}
