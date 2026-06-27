package com.charles.nutrisnap.data

/** The lifecycle of the on-device Gemma 4 model file. */
sealed interface ModelState {
    data object NotDownloaded : ModelState
    data class Downloading(
        val percent: Int,
        val bytesSoFar: Long,
        val totalBytes: Long,
    ) : ModelState
    data class Verifying(val path: String) : ModelState
    data class Ready(val path: String) : ModelState
    data class Failed(val reason: String) : ModelState
}
