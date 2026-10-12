package com.smshub.app

import com.smshub.app.db.PendingMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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
        /** 鉴权失败等不可重试失败 */
        data class FatalError(val message: String) : Result()
    }

    /** 自动注册设备：成功时 Success 的 message 为设备密钥 */
    fun register(serverUrl: String, name: String, installId: String): Result {
        val payload = JSONObject()
            .put("name", name)
            .put("installId", installId)
            .toString()
        val request = Request.Builder()
            .url("$serverUrl/api/v1/devices/register")
            .post(payload.toRequestBody(JSON))
            .build()
        return execute(request) { body ->
            JSONObject(body).getString("deviceKey")
        }
    }

    /** 心跳 / 连接测试 */
    fun ping(serverUrl: String, deviceKey: String): Result {
        val request = Request.Builder()
            .url("$serverUrl/api/v1/ping")
            .header("Authorization", "Bearer $deviceKey")
            .get()
            .build()
        return execute(request) { body ->
            val device = JSONObject(body).optJSONObject("device")
            "连接成功，设备：${device?.optString("name") ?: "未知"}"
        }
    }

    /** 批量上传短信 */
    fun upload(serverUrl: String, deviceKey: String, messages: List<PendingMessage>): Result {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(
                JSONObject()
                    .put("address", m.address)
                    .put("body", m.body)
                    .put("date", m.date)
                    .put("type", m.type)
            )
        }
        val payload = JSONObject().put("messages", arr).toString()

        val request = Request.Builder()
            .url("$serverUrl/api/v1/messages")
            .header("Authorization", "Bearer $deviceKey")
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
                    resp.code == 401 -> Result.FatalError("设备密钥无效（401）")
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
