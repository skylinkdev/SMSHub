package com.smshub.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 前台服务：保持进程存活，并通过 ContentObserver 监听短信数据库变化。
 * 广播被 ROM 拦截时，只要服务存活依然能捕获新短信。
 * 同时每 30 秒轮询一次下行发送任务（Web/URL 创建的短信由这里发出）。
 */
class MonitorService : Service() {

    private var observer: ContentObserver? = null
    private val handler = Handler(Looper.getMainLooper())

    private val sendPoller = object : Runnable {
        override fun run() {
            CoroutineScope(Dispatchers.IO).launch {
                SendManager.pollOnce(applicationContext)
            }
            handler.postDelayed(this, SEND_POLL_INTERVAL_MS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        registerObserver()
        // 服务（重启时补一次增量检查，防漏
        CoroutineScope(Dispatchers.IO).launch { SmsCapture.check(applicationContext) }
        // 启动下行发送轮询
        handler.removeCallbacks(sendPoller)
        handler.post(sendPoller)
        return START_STICKY
    }

    private fun registerObserver() {
        if (observer != null) return
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                CoroutineScope(Dispatchers.IO).launch {
                    SmsCapture.check(applicationContext)
                }
            }
        }
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI, true, obs
        )
        observer = obs
        Prefs(this).log("短信数据库观察器已注册")
    }

    override fun onDestroy() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        handler.removeCallbacks(sendPoller)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "monitor"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "短信监控", NotificationManager.IMPORTANCE_MIN)
            )
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("SMSHub 运行中")
            .setContentText("正在监控新短信并同步到服务器")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val SEND_POLL_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, MonitorService::class.java)
                )
            } catch (e: Exception) {
                // 后台启动受限时忽略：收到短信时 SmsReceiver 会再次尝试拉起
            }
        }
    }
}
