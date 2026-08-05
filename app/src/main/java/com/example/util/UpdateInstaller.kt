package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateInstaller {

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String = "https://github.com/satriaevo77/AStock/releases/download/v1.2/app-release.apk",
        onProgress: (String) -> Unit
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            onProgress("Menyiapkan unduhan APK di latar belakang...")

            // 1. Enqueue download to Android System DownloadManager
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (downloadManager != null) {
                    val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                        setTitle("AStock Update-Fix v1.2")
                        setDescription("Mengunduh paket APK pembaruan di latar belakang...")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "AStock_v1.2_Fix.apk")
                        setMimeType("application/vnd.android.package-archive")
                    }
                    downloadManager.enqueue(request)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Perform direct streaming download with progress callback
            val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir, "AStock_v1.2_Fix.apk")
            if (destFile.exists()) {
                destFile.delete()
            }

            onProgress("Menghubungkan ke server repositori...")
            var connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            // Handle HTTP Redirects (e.g. GitHub releases to AWS S3)
            var responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == 307 || responseCode == 308) {
                val newUrl = connection.getHeaderField("Location")
                if (!newUrl.isNull_or_empty()) {
                    connection.disconnect()
                    connection = URL(newUrl).openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.connect()
                    responseCode = connection.responseCode
                }
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (fileLength > 0) {
                    val percent = ((totalBytesRead * 100) / fileLength).toInt()
                    onProgress("Mengunduh APK v1.2: $percent% (${totalBytesRead / 1024} KB / ${fileLength / 1024} KB)")
                } else {
                    onProgress("Mengunduh APK v1.2: ${totalBytesRead / 1024} KB...")
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            onProgress("Mengunduh selesai (100%). Menyiapkan installer Android...")

            // 3. Trigger Android APK Package Installer
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                destFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                try {
                    context.startActivity(installIntent)
                } catch (e: Exception) {
                    // Fallback to browser or download manager if direct install intent fails
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                }
            }

            Pair(true, "✓ Paket APK Update-Fix v1.2 berhasil diunduh ke folder Downloads! Membuka installer Android...")

        } catch (e: Exception) {
            e.printStackTrace()

            // Fallback: Launch DownloadManager / Browser download
            withContext(Dispatchers.Main) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            Pair(true, "✓ Unduhan APK v1.2 telah diteruskan ke DownloadManager & Browser HP Anda untuk diinstal.")
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
}
