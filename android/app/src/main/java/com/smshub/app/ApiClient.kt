package com.smshub.app

import com.smshub.app.db.PendingMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ApiClient {

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val message: String) : Result()
        /** 网络错误等可重试失败 */
        data class RetryableError(val message: String) : Result()
        /** 请求被拒绝等不可重试失败 */
        data class FatalError(val message: String) : Result()
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** 心跳 / 连接测试（同时让服务器按设备名自动创建设备，并上报本机号码） */
    fun ping(serverUrl: String, name: String, phone: String): Result {
        val request = Request.Builder()
            .url("$serverUrl/api/v1/ping?name=${enc(name)}&phone=${enc(phone)}")
            .get()
            .build()
        return execute(request) { body ->
            val device = JSONObject(body).optJSONObject("device")
            "连接成功，设备：${device?.optString("name") ?: name}"
        }
    }

    /** 批量上传短信（附带本机号码，取不到为空串） */
    fun upload(serverUrl: String, deviceName: String, phone: String, messages: List<PendingMessage>): Result {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(
                JSONObject()
                    .put("address", m.address)
                    .put("body", m.body)
                    .put("date", m.date)
                    .put("type", m.type)
                    .put("receiver", m.receiver)
            )
        }
        val payload = JSONObject()
            .put("device", deviceName)
            .put("phone", phone)
            .put("messages", arr)
            .toString()

        val request = Request.Builder()
            .url("$serverUrl/api/v1/messages")
            .post(payload.toRequestBody(JSON))
            .build()
        return execute(request) { body ->
            "已上传 ${JSONObject(body).optInt("received", messages.size)} 条"
        }
    }

    private fun execute(request: Request, onSuccess: (String) -> String): Result {
        return try {
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                when {
                    resp.isSuccessful -> Result.Success(onSuccess(body))
                    resp.code in 400..499 ->
                        Result.FatalError("请求被拒绝（${resp.code}）：${body.take(100)}")
                    else -> Result.RetryableError("服务器错误（${resp.code}）")
                }
            }
        } catch (e: Exception) {
            Result.RetryableError("网络错误：${e.message ?: e.javaClass.simpleName}")
        }
    }
}
