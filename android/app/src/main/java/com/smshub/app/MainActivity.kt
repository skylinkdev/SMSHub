package com.smshub.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvPending = findViewById<TextView>(R.id.tvPending)

        etServerUrl.setText(prefs.serverUrl)

        fun saveUrl(): Boolean {
            prefs.serverUrl = etServerUrl.text?.toString().orEmpty()
            if (prefs.serverUrl.isBlank()) {
                Toast.makeText(this, "请填写服务器地址", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        // 保存配置（仅保存服务器地址，设备按名称自动识别）
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            if (!saveUrl()) return@setOnClickListener
            tvStatus.text = "验证服务器中…"
            lifecycleScope.launch {
                val msg = withContext(Dispatchers.IO) {
                    when (val r = ApiClient.ping(prefs.serverUrl, Prefs.deviceName, OwnNumber.get(this@MainActivity))) {
                        is ApiClient.Result.Success -> r.message
                        is ApiClient.Result.RetryableError -> r.message
                        is ApiClient.Result.FatalError -> r.message
                    }
                }
                tvStatus.text = msg
                refreshDeviceInfo()
            }
        }

        // 测试连接
        findViewById<Button>(R.id.btnPing).setOnClickListener {
            if (!saveUrl()) return@setOnClickListener
            tvStatus.text = "连接中…"
            lifecycleScope.launch {
                val msg = withContext(Dispatchers.IO) {
                    when (val r = ApiClient.ping(prefs.serverUrl, Prefs.deviceName, OwnNumber.get(this@MainActivity))) {
                        is ApiClient.Result.Success -> r.message
                        is ApiClient.Result.RetryableError -> r.message
                        is ApiClient.Result.FatalError -> r.message
                    }
                }
                tvStatus.text = msg
                refreshDeviceInfo()
            }
        }

        // 申请权限：收/读短信 + 电话状态（读取本机号码）+ 通知（前台服务）
        findViewById<Button>(R.id.btnPermission).setOnClickListener {
            val perms = mutableListOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS
            )
            if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionLauncher.launch(perms.toTypedArray())
        }

        // 打开 App 即启动前台监控服务
        MonitorService.start(this)

        // 同步历史短信（最近 200 条）
        findViewById<Button>(R.id.btnSyncHistory).setOnClickListener {
            if (!saveUrl()) return@setOnClickListener
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

        // 读取最近一条短信（测试读取能力，不上传）
        findViewById<Button>(R.id.btnReadLast).setOnClickListener {
            showLastSms()
        }

        // 发送测试消息（验证上传链路）
        findViewById<Button>(R.id.btnTest).setOnClickListener {
            if (!saveUrl()) return@setOnClickListener
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.get(this@MainActivity).dao().insertAll(
                        listOf(
                            PendingMessage(
                                address = "SMSHub",
                                body = "测试消息 ${timeFmt.format(Date())}",
                                date = System.currentTimeMillis()
                            )
                        )
                    )
                    UploadWorker.enqueue(this@MainActivity)
                }
                Toast.makeText(this@MainActivity, "测试消息已加入队列", Toast.LENGTH_SHORT).show()
            }
        }

        // 待上传数量实时刷新
        lifecycleScope.launch {
            AppDatabase.get(this@MainActivity).dao().countFlow().collect { count ->
                tvPending.text = "待上传：$count 条"
            }
        }

        refreshPermissionStatus()
        refreshDeviceInfo()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
        refreshDeviceInfo()
        refreshStatus()
    }

    private fun refreshDeviceInfo() {
        findViewById<TextView>(R.id.tvDeviceInfo).text =
            if (prefs.isConfigured) "设备：${Prefs.deviceName}"
            else "设备：${Prefs.deviceName}（填写服务器地址后自动上线）"
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
        val logs = prefs.getLogs()
        findViewById<TextView>(R.id.tvLog).text =
            if (logs.isBlank()) "暂无日志" else logs
    }

    /** 读取并展示最近一条短信（仅本地展示，用于验证读取权限与能力） */
    private fun showLastSms() {
        val tv = findViewById<TextView>(R.id.tvLastSms)
        tv.visibility = android.view.View.VISIBLE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            tv.text = "未授予读取短信权限，请先点击「申请短信权限」"
            return
        }
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
            if (cursor.moveToFirst()) {
                val address = cursor.getString(0) ?: "unknown"
                val body = cursor.getString(1) ?: ""
                val date = cursor.getLong(2)
                val type = if (cursor.getInt(3) == Telephony.Sms.MESSAGE_TYPE_SENT)
                    "发件" else "收件"
                tv.text = "最近一条（$type）\n号码：$address\n时间：${timeFmt.format(Date(date))}\n内容：${body.take(200)}"
                prefs.log("读取最近一条短信成功：$address")
                return
            }
        }
        tv.text = "没有读到短信（收件箱为空或读取失败）"
        prefs.log("读取最近一条短信：收件箱为空")
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
