package com.howdy.echowave.data.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LocalAudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copies user-selected audio into app-specific external Music storage. Android removes
 * this directory when EchoWave is uninstalled. Audio bytes are never stored in Room.
 */
class LocalAudioRepositoryImpl(context: Context) : LocalAudioRepository {
    private val appContext = context.applicationContext

    override suspend fun tracks(): List<Track> = withContext(Dispatchers.IO) {
        audioDirectory().listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension.lowercase() in SUPPORTED_EXTENSIONS }
            .sortedBy { it.name.lowercase() }
            .mapNotNull(::toTrack)
            .toList()
    }

    override suspend fun importFrom(uri: String): Track = withContext(Dispatchers.IO) {
        val sourceUri = uri.toUri()
        val displayName = displayName(sourceUri)
        val originalExtension = displayName.substringAfterLast('.', "").lowercase()
        val mimeType = appContext.contentResolver.getType(sourceUri).orEmpty()
        val extension = chooseExtension(originalExtension, mimeType)
            ?: throw IllegalArgumentException("Choose a supported audio file.")

        val directory = audioDirectory()
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("EchoWave could not create its music folder.")
        }

        val baseName = displayName.substringBeforeLast('.', displayName)
            .sanitizeFileName()
            .ifBlank { "Audio" }
            .take(90)
        val destination = File(directory, "$baseName-${UUID.randomUUID()}.$extension")
        try {
            val input = appContext.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("EchoWave could not open the selected file.")
            input.use { stream ->
                FileOutputStream(destination).use { output ->
                    stream.copyTo(output, DEFAULT_BUFFER_SIZE)
                    output.fd.sync()
                }
            }
            if (destination.length() == 0L) {
                throw IllegalArgumentException("The selected audio file is empty.")
            }
            toTrack(destination)
                ?: throw IllegalArgumentException("EchoWave could not read this audio file.")
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    override suspend fun delete(trackId: String) = withContext(Dispatchers.IO) {
        val file = fileForId(trackId) ?: return@withContext
        if (file.exists() && !file.delete()) {
            throw IllegalStateException("EchoWave could not remove this audio file.")
        }
    }

    override suspend fun fileUri(trackId: String): String? = withContext(Dispatchers.IO) {
        fileForId(trackId)?.takeIf { it.isFile && it.length() > 0L }?.toURI()?.toString()
    }

    private fun audioDirectory(): File {
        val musicRoot = appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(appContext.filesDir, Environment.DIRECTORY_MUSIC)
        return File(musicRoot, "$APP_FOLDER/$DOWNLOADS_FOLDER")
    }

    private fun fileForId(trackId: String): File? {
        if (!trackId.startsWith(LocalAudioRepository.TRACK_ID_PREFIX)) return null
        val name = trackId.removePrefix(LocalAudioRepository.TRACK_ID_PREFIX)
        if (name.isBlank() || File(name).name != name) return null
        val root = audioDirectory().canonicalFile
        val candidate = File(root, name).canonicalFile
        return candidate.takeIf { it.parentFile == root && it.extension.lowercase() in SUPPORTED_EXTENSIONS }
    }

    private fun displayName(uri: Uri): String {
        val fromProvider = runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()
        return fromProvider?.takeIf(String::isNotBlank)
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: "Audio"
    }

    private fun toTrack(file: File): Track? {
        if (!file.isFile) return null
        val retriever = MediaMetadataRetriever()
        val metadata = try {
            retriever.setDataSource(file.absolutePath)
            Metadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
        return Track(
            id = LocalAudioRepository.TRACK_ID_PREFIX + file.name,
            title = metadata?.title?.takeIf(String::isNotBlank) ?: file.nameWithoutExtension,
            artist = metadata?.artist?.takeIf(String::isNotBlank) ?: "Unknown artist",
            album = metadata?.album?.takeIf(String::isNotBlank),
            durationMs = metadata?.durationMs?.takeIf { it > 0L },
            source = LOCAL_SOURCE,
        )
    }

    private data class Metadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long?,
    )

    private fun chooseExtension(extension: String, mimeType: String): String? {
        if (extension in SUPPORTED_EXTENSIONS) return extension
        val byMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.lowercase()
        return byMime?.takeIf { it in SUPPORTED_EXTENSIONS }
    }

    private fun String.sanitizeFileName(): String =
        replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim()
            .trim('.')

    companion object {
        const val LOCAL_SOURCE = "local"
        private const val APP_FOLDER = "EchoWave"
        private const val DOWNLOADS_FOLDER = "Downloads"
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024
        private val SUPPORTED_EXTENSIONS = setOf("aac", "flac", "m4a", "mp3", "ogg", "opus", "wav", "webm")
    }
}
