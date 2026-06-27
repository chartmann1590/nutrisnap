package com.charles.nutrisnap.data

import com.charles.nutrisnap.BuildConfig

enum class ModelVariant(
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
) {
    E2B(
        url = BuildConfig.MODEL_URL,
        fileName = BuildConfig.MODEL_FILE_NAME,
        sizeBytes = BuildConfig.MODEL_SIZE_BYTES,
    ),
    E4B(
        url = BuildConfig.MODEL_URL_E4B,
        fileName = BuildConfig.MODEL_FILE_NAME_E4B,
        sizeBytes = BuildConfig.MODEL_SIZE_BYTES_E4B,
    );
}