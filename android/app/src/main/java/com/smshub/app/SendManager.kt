package com.smshub.app

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager

/**
 * 轮询并执行下行发送任务：/api/v1/outbox 拉取 → SmsManager 发送 → 回执上报。
 * 所有方法均为阻塞式，请在 Dispatchers.IO 中调用。
 */
object SendManager {

    fun pollOnce(context: Context) {
        val prefs = Prefs(context)
        if (!prefs.isConfigured) return

        val tasks = ApiClient.fetchOutbox(prefs.serverUrl, Prefs.deviceName) ?: return
        for (task in tasks) {
            val error = sendSms(context, task.to, task.body, task.simSlot)
            val ok = error == null
            val reported = ApiClient.reportOutbox(
                prefs.serverUrl, Prefs.deviceName, task.id, ok, error
            )
            prefs.log(
                if (ok) "已发送到 ${task.to}" + if (reported) "" else "（回执失败）"
                else "发送失败 ${task.to}：$error"
            )
        }
    }

    /** 返回 null 表示成功，否则为错误描述 */
    @SuppressLint("MissingPermission")
    private fun sendSms(context: Context, to: String, body: String, simSlot: Int?): String? {
        return try {
            val smsManager = resolveSmsManager(context, simSlot)
            val parts = smsManager.divideMessage(body)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(to, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(to, null, body, null, null)
            }
            null
        } catch (e: Exception) {
            (e.message ?: e.javaClass.simpleName).take(150)
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun resolveSmsManager(context: Context, simSlot: Int?): SmsManager {
        val default: SmsManager =
            if (Build.VERSION.SDK_INT >= 31) context.getSystemService(SmsManager::class.java)
            else SmsManager.getDefault()

        if (simSlot == null) return default
        // 按卡槽找 subscriptionId：simSlotIndex 从 0 开始，卡 1 -> 0，卡 2 -> 1
        val subId = try {
            val smgr = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager
            smgr.activeSubscriptionInfoList
                ?.firstOrNull { it.simSlotIndex == simSlot - 1 }
                ?.subscriptionId
        } catch (e: SecurityException) {
            null // 未授予 READ_PHONE_STATE 时回退默认卡
        } catch (e: Exception) {
            null
        } ?: return default

        return if (Build.VERSION.SDK_INT >= 31) default.createForSubscriptionId(subId)
        else SmsManager.getSmsManagerForSubscriptionId(subId)
    }
}
