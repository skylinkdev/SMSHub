package com.smshub.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * 前台服务：保持进程存活，防止国产 ROM 冻结进程导致收不到短信广播。
 * 显示一条低优先级常驻通知。
 */
class MonitorService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "monitor"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "短信监控", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("SMSHub 运行中")
            .setContentText("正在监控新短信并同步到服务器")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001

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
