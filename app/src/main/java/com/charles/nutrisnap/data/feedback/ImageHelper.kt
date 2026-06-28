package com.charles.nutrisnap.data.feedback

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageHelper {

    fun uriToBase64(context: Context, uri: Uri): Result<String> = runCatching {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream for $uri")
        inputStream.use { stream ->
            val buffer = ByteArrayOutputStream()
            stream.copyTo(buffer)
            Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP)
        }
    }
}
