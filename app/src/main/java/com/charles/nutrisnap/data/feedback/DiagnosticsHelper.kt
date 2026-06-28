package com.charles.nutrisnap.data.feedback

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.icu.util.TimeZone
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DiagnosticsHelper {

    fun collect(context: Context): String {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val packageName = context.packageName
        val versionName = try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) { "?" }
        val versionCode = try {
            if (Build.VERSION.SDK_INT >= 28) {
                context.packageManager.getPackageInfo(packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
            }
        } catch (_: Exception) { 0L }

        val deviceBrand = Build.BRAND
        val deviceModel = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val androidVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT
        val locale = Locale.getDefault().toString()
        val timeZone = TimeZone.getDefault().getDisplayName(
            false, TimeZone.SHORT, Locale.getDefault()
        )
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val disk = getDiskInfo()
        val memory = getMemoryInfo()

        return buildString {
            appendLine("## Diagnostics")
            appendLine()
            appendLine("- App: $appName")
            appendLine("- Package: $packageName")
            appendLine("- Version: $versionName ($versionCode)")
            appendLine("- Device: $deviceBrand $deviceModel")
            appendLine("- Manufacturer: $manufacturer")
            appendLine("- Android: $androidVersion / API $apiLevel")
            appendLine("- Locale: $locale")
            appendLine("- Time Zone: $timeZone")
            appendLine("- Storage Free/Total: ${disk.free} / ${disk.total}")
            appendLine("- Memory Free/Total: ${memory.free} / ${memory.total}")
            appendLine("- Timestamp: $timestamp")
        }
    }

    private data class StorageInfo(val free: String, val total: String)

    private fun getDiskInfo(): StorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val freeBlocks = stat.availableBlocksLong
            StorageInfo(
                free = formatBytes(freeBlocks * blockSize),
                total = formatBytes(totalBlocks * blockSize),
            )
        } catch (_: Exception) {
            StorageInfo("?", "?")
        }
    }

    private fun getMemoryInfo(): StorageInfo {
        return try {
            val runtime = Runtime.getRuntime()
            val freeMem = runtime.freeMemory()
            val totalMem = runtime.totalMemory()
            StorageInfo(
                free = formatBytes(freeMem),
                total = formatBytes(totalMem),
            )
        } catch (_: Exception) {
            StorageInfo("?", "?")
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return "%.1f GB".format(gb)
    }
}
