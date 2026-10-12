package com.smshub.app

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import com.smshub.app.db.AppDatabase
import java.util.concurrent.TimeUnit

/**
 * 把本地队列里的短信批量上传到服务器。
 * 使用唯一工作名，避免重复排队；失败自动指数退避重试。
 */
class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.isConfigured) return Result.success()

        val dao = AppDatabase.get(applicationContext).dao()
        val batch = dao.batch(BATCH_SIZE)
        if (batch.isEmpty()) return Result.success()

        return when (val r = ApiClient.upload(prefs.serverUrl, Prefs.deviceName, batch)) {
            is ApiClient.Result.Success -> {
                dao.deleteByIds(batch.map { it.id })
                prefs.lastUploadAt = System.currentTimeMillis()
                prefs.lastStatus = r.message
                prefs.log("上传成功：${batch.size} 条")
                // 队列里还有剩余则继续上传下一批
                val remaining = dao.batch(1)
                if (remaining.isNotEmpty()) enqueue(applicationContext)
                Result.success()
            }
            is ApiClient.Result.RetryableError -> {
                prefs.lastStatus = r.message
                prefs.log("上传失败（将重试）：${r.message}")
                if (runAttemptCount < MAX_RETRY) Result.retry() else Result.success()
            }
            is ApiClient.Result.FatalError -> {
                // 请求被拒绝等：丢弃重试，等待用户修正配置
                prefs.lastStatus = r.message
                prefs.log("上传失败（不重试）：${r.message}")
                Result.success()
            }
        }
    }

    companion object {
        private const val WORK_NAME = "smshub_upload"
        private const val BATCH_SIZE = 100
        private const val MAX_RETRY = 5

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
