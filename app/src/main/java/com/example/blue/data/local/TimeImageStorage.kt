package com.example.blue.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TimeImageStorage(context: Context) {
    private val appContext = context.applicationContext
    private val filesDirectory = File(appContext.filesDir, DIRECTORY_NAME)

    fun fileFor(relativePath: String): File = File(appContext.filesDir, relativePath)

    suspend fun copyFromUri(source: Uri): String = withContext(Dispatchers.IO) {
        check(filesDirectory.isDirectory || filesDirectory.mkdirs()) { "无法创建时光图片目录" }
        val extension = appContext.contentResolver.getType(source)
            ?.substringAfter('/', "jpg")
            ?.takeIf { it.matches(Regex("[A-Za-z0-9]+")) }
            ?: "jpg"
        val destination = File(filesDirectory, "${UUID.randomUUID()}.$extension")
        appContext.contentResolver.openInputStream(source)?.use { input ->
            destination.outputStream().use(input::copyTo)
        } ?: error("无法读取所选图片")
        "$DIRECTORY_NAME/${destination.name}"
    }

    suspend fun delete(relativePath: String) = withContext(Dispatchers.IO) {
        fileFor(relativePath).delete()
    }

    companion object {
        const val DIRECTORY_NAME = "time_images"
    }
}
