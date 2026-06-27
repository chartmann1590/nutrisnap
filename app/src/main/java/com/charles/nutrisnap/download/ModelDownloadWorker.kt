package com.charles.nutrisnap.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.charles.nutrisnap.BuildConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val client: OkHttpClient,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: BuildConfig.MODEL_URL
        val fileName = inputData.getString(KEY_FILE_NAME) ?: BuildConfig.MODEL_FILE_NAME
        val totalBytes = inputData.getLong(KEY_TOTAL, BuildConfig.MODEL_SIZE_BYTES)

        val modelsDir = File(applicationContext.filesDir, "models").apply { mkdirs() }
        val finalFile = File(modelsDir, fileName)
        val partFile = File(modelsDir, "$fileName.part")

        if (finalFile.exists() && finalFile.length() > 0) {
            return@withContext Result.success(doneData(finalFile))
        }

        val have = if (partFile.exists()) partFile.length() else 0L

        try {
            val request = Request.Builder()
                .url(url)
                .apply { if (have > 0) header("Range", "bytes=$have-") }
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.retry()
                }
                val body = response.body ?: return@withContext Result.retry()

                val total = response.header("Content-Range")
                    ?.substringAfter('/')?.toLongOrNull()
                    ?: (body.contentLength().takeIf { it > 0 }?.let { have + it })
                    ?: totalBytes

                val appendMode = response.code == 206 && have > 0
                RandomAccessFile(partFile, "rw").use { out ->
                    if (appendMode) out.seek(have) else out.setLength(0)
                    var written = if (appendMode) have else 0L
                    val buffer = ByteArray(1 shl 16)
                    body.byteStream().use { input ->
                        while (true) {
                            if (isStopped) return@withContext Result.retry()
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            written += read
                            val pct = if (total > 0) ((written * 100) / total).toInt().coerceIn(0, 100) else 0
                            setProgress(progressData(pct, written, total))
                        }
                    }
                }
            }

            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
            Result.success(doneData(finalFile))
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun progressData(percent: Int, soFar: Long, total: Long): Data =
        workDataOf(KEY_PERCENT to percent, KEY_SO_FAR to soFar, KEY_TOTAL to total)

    private fun doneData(file: File): Data = workDataOf(KEY_PATH to file.absolutePath)

    companion object {
        const val UNIQUE_NAME = "gemma4_model_download"
        const val KEY_PERCENT = "percent"
        const val KEY_SO_FAR = "so_far"
        const val KEY_TOTAL = "total"
        const val KEY_PATH = "path"
        const val KEY_VARIANT = "variant"
        const val KEY_URL = "url"
        const val KEY_FILE_NAME = "file_name"
    }
}