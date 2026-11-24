package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object AndroidMediaHelper {

    suspend fun saveImageToGallery(
        context: Context,
        bytes: ByteArray,
        mimeType: String,
        fileNamePrefix: String = "momo_ai"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val ext = when {
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                mimeType.contains("webp") -> "webp"
                else -> "png"
            }
            val displayName = "${fileNamePrefix}_${System.currentTimeMillis()}.$ext"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Momo")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("Gagal membuat file di galeri Android")
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                } ?: throw Exception("Gagal menulis gambar ke galeri")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Tersimpan di Galeri (Pictures/Momo/$displayName)"
            } else {
                // Zero-permission fallback for Android 7-9: save to app pictures and open Share Sheet
                val file = writeTempShareFile(context, bytes, displayName)
                shareFile(context, file, mimeType, "Simpan gambar Momo AI")
                "Pilih aplikasi untuk menyimpan $displayName"
            }
        }
    }

    suspend fun saveDataUrlOrRemoteImage(
        context: Context,
        apiClient: MomoApiClient,
        imageUrl: String?,
        dataUrl: String?,
        fileNamePrefix: String = "momo_image"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val (bytes, mime) = when {
                !dataUrl.isNullOrBlank() && dataUrl.startsWith("data:") -> decodeDataUrl(dataUrl)
                !imageUrl.isNullOrBlank() -> apiClient.downloadImageBytes(imageUrl)
                else -> throw Exception("Sumber gambar tidak tersedia")
            }
            saveImageToGallery(context, bytes, mime, fileNamePrefix).getOrThrow()
        }
    }

    suspend fun shareRemoteImage(
        context: Context,
        apiClient: MomoApiClient,
        imageUrl: String,
        caption: String = "Karya dari Momo AI"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val (bytes, mime) = apiClient.downloadImageBytes(imageUrl)
                val ext = if (mime.contains("jpeg")) "jpg" else "png"
                val file = writeTempShareFile(context, bytes, "momo_share_${System.currentTimeMillis()}.$ext")
                withContext(Dispatchers.Main) {
                    shareFile(context, file, mime, caption)
                }
            } catch (_: Exception) {
                // Fallback to ?download=1 link share
                val downloadLink = QuotaFormatters.appendDownloadParam(imageUrl)
                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "$caption\n$downloadLink")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan gambar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }

    fun openDownloadFallbackLink(context: Context, imageUrl: String) {
        val url = QuotaFormatters.appendDownloadParam(imageUrl)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    suspend fun readUriAsAttachment(
        context: Context,
        uri: Uri,
        kind: AttachmentKind
    ): Result<ChatAttachment> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: if (kind == AttachmentKind.PHOTO) "image/jpeg" else "application/octet-stream"
            val name = queryFileName(context, uri) ?: if (kind == AttachmentKind.PHOTO) "foto.jpg" else "berkas"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Tidak dapat membaca berkas yang dipilih")

            if (bytes.size > 8 * 1024 * 1024) {
                throw Exception("Ukuran berkas melebihi 8 MB")
            }

            val isTextLike = kind == AttachmentKind.FILE && (
                mimeType.startsWith("text/") ||
                    mimeType.contains("json") ||
                    mimeType.contains("xml") ||
                    mimeType.contains("javascript") ||
                    name.endsWith(".md", true) ||
                    name.endsWith(".txt", true) ||
                    name.endsWith(".csv", true) ||
                    name.endsWith(".kt", true) ||
                    name.endsWith(".py", true) ||
                    name.endsWith(".ts", true)
                )

            val textContent = if (isTextLike) bytes.toString(Charsets.UTF_8) else null
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUrl = "data:$mimeType;base64,$b64"

            ChatAttachment(
                id = UUID.randomUUID().toString(),
                kind = kind,
                name = name,
                mimeType = mimeType,
                dataUrl = dataUrl,
                textContent = textContent,
                previewUri = if (kind == AttachmentKind.PHOTO) uri.toString() else null
            )
        }
    }

    fun decodeDataUrl(dataUrl: String): Pair<ByteArray, String> {
        val headerEnd = dataUrl.indexOf(',')
        if (headerEnd <= 0) throw Exception("Format data gambar tidak valid")
        val header = dataUrl.substring(5, headerEnd) // after "data:"
        val mime = header.substringBefore(";").ifBlank { "image/png" }
        val base64Part = dataUrl.substring(headerEnd + 1)
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        return bytes to mime
    }

    private fun writeTempShareFile(context: Context, bytes: ByteArray, fileName: String): File {
        val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return file
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
