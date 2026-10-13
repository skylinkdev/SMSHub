package com.smshub.app

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/** 读取本机号码；读不到（权限被拒/运营商未写入 SIM 卡）时返回空串 */
object OwnNumber {

    /** subscriptionId → 该卡号码 的映射（双卡时用于标记短信的接收卡） */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun bySubscription(context: Context): Map<Int, String> {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val smgr = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager
            val list = smgr.activeSubscriptionInfoList ?: return emptyMap()
            val map = mutableMapOf<Int, String>()
            for (info in list) {
                val number = try {
                    tm.createForSubscriptionId(info.subscriptionId).line1Number
                } catch (e: Exception) {
                    null
                }
                if (!number.isNullOrBlank()) map[info.subscriptionId] = number.trim()
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun get(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            // 先试默认线路号码
            val direct = tm.line1Number
            if (!direct.isNullOrBlank()) return direct.trim()
            // 双卡场景：逐张 SIM 卡尝试
            val smgr = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager
            val list = smgr.activeSubscriptionInfoList ?: return ""
            for (info in list) {
                val number = try {
                    tm.createForSubscriptionId(info.subscriptionId).line1Number
                } catch (e: Exception) {
                    null
                }
                if (!number.isNullOrBlank()) return number.trim()
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }
}
