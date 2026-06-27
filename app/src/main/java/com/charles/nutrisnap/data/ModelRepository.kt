package com.charles.nutrisnap.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.charles.nutrisnap.download.ModelDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val prefs: UserPreferencesRepository,
) {
    private fun modelFile(variant: ModelVariant): File =
        File(File(context.filesDir, "models"), variant.fileName)

    private fun uniqueName(variant: ModelVariant): String =
        "gemma4_model_download_${variant.name}"

    fun isReady(variant: ModelVariant = ModelVariant.E2B): Boolean =
        modelFile(variant).exists() && modelFile(variant).length() > 0

    val state: Flow<ModelState> = prefs.prefs.map { it.modelVariant }.let { variantFlow ->
        variantFlow.flatMapLatest { variant ->
            flow {
                val file = modelFile(variant)
                if (file.exists() && file.length() > 0) {
                    emit(ModelState.Ready(file.absolutePath))
                } else {
                    emitAll(workInfoState(variant))
                }
            }
        }
    }

    private fun workInfoState(variant: ModelVariant): Flow<ModelState> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueName(variant))
            .flatMapLatest { infos ->
                val info = infos.firstOrNull()
                when (info?.state) {
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> flowOf(
                        ModelState.Downloading(
                            percent = info.progress.getInt(ModelDownloadWorker.KEY_PERCENT, 0),
                            bytesSoFar = info.progress.getLong(ModelDownloadWorker.KEY_SO_FAR, 0L),
                            totalBytes = info.progress.getLong(
                                ModelDownloadWorker.KEY_TOTAL, variant.sizeBytes,
                            ),
                        )
                    )
                    WorkInfo.State.SUCCEEDED ->
                        flowOf(ModelState.Ready(modelFile(variant).absolutePath))
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED ->
                        flowOf(ModelState.Failed("Download failed - tap to retry"))
                    else -> flowOf(ModelState.NotDownloaded)
                }
            }

    private fun buildRequest(variant: ModelVariant) =
        OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    ModelDownloadWorker.KEY_VARIANT to variant.name,
                    ModelDownloadWorker.KEY_URL to variant.url,
                    ModelDownloadWorker.KEY_FILE_NAME to variant.fileName,
                    ModelDownloadWorker.KEY_TOTAL to variant.sizeBytes,
                )
            )
            .build()

    fun startDownload(variant: ModelVariant = ModelVariant.E2B) {
        workManager.enqueueUniqueWork(
            uniqueName(variant),
            ExistingWorkPolicy.KEEP,
            buildRequest(variant),
        )
    }

    fun retryDownload(variant: ModelVariant = ModelVariant.E2B) {
        workManager.enqueueUniqueWork(
            uniqueName(variant),
            ExistingWorkPolicy.REPLACE,
            buildRequest(variant),
        )
    }

    fun cancel(variant: ModelVariant = ModelVariant.E2B) =
        workManager.cancelUniqueWork(uniqueName(variant))

    fun lastPath(variant: ModelVariant = ModelVariant.E2B): String? {
        val file = modelFile(variant)
        return if (file.exists()) file.absolutePath else null
    }

    fun markCorrupt(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }
}