package com.example.data.offline

import android.content.Context
import android.net.Uri
import com.example.data.local.OfflineDatabase
import com.example.data.local.OfflineTrackEntity
import com.example.data.local.status
import com.example.data.model.OfflineDownloadStatus
import com.example.data.model.Track
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OfflineDownloadManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val dao = OfflineDatabase.getInstance(appContext).offlineTrackDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val vaultDir: File
        get() = File(appContext.filesDir, "offline_audio").apply {
            if (!exists()) mkdirs()
        }

    fun observeOfflineMap(): Flow<Map<String, OfflineTrackEntity>> =
        dao.observeAll().map { list ->
            list.associateBy { it.trackId }
        }

    fun downloadTrack(track: Track) {
        if (activeJobs.containsKey(track.id)) return
        val job = scope.launch {
            val targetFile = File(vaultDir, "${track.id}.wav")
            val tempFile = File(vaultDir, "${track.id}.wav.part")
            try {
                dao.upsert(
                    OfflineTrackEntity(
                        trackId = track.id,
                        localFilePath = targetFile.absolutePath,
                        fileSizeBytes = 0L,
                        statusName = OfflineDownloadStatus.DOWNLOADING.name,
                        progressPercent = 0,
                        downloadedAt = System.currentTimeMillis()
                    )
                )

                appContext.resources.openRawResource(track.rawResId).use { input ->
                    val totalBytes = input.available().coerceAtLeast(1)
                    var copiedBytes = 0L
                    var lastReportedPct = -1
                    val buffer = ByteArray(16 * 1024)

                    FileOutputStream(tempFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copiedBytes += read
                            val pct = ((copiedBytes * 100L) / totalBytes).toInt().coerceIn(1, 99)
                            if (pct - lastReportedPct >= 12) {
                                lastReportedPct = pct
                                dao.upsert(
                                    OfflineTrackEntity(
                                        trackId = track.id,
                                        localFilePath = targetFile.absolutePath,
                                        fileSizeBytes = copiedBytes,
                                        statusName = OfflineDownloadStatus.DOWNLOADING.name,
                                        progressPercent = pct,
                                        downloadedAt = System.currentTimeMillis()
                                    )
                                )
                                delay(45)
                            }
                        }
                        output.flush()
                    }
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                dao.upsert(
                    OfflineTrackEntity(
                        trackId = track.id,
                        localFilePath = targetFile.absolutePath,
                        fileSizeBytes = targetFile.length(),
                        statusName = OfflineDownloadStatus.DOWNLOADED.name,
                        progressPercent = 100,
                        downloadedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: CancellationException) {
                tempFile.delete()
                dao.deleteById(track.id)
                throw e
            } catch (_: Exception) {
                tempFile.delete()
                dao.upsert(
                    OfflineTrackEntity(
                        trackId = track.id,
                        localFilePath = "",
                        fileSizeBytes = 0L,
                        statusName = OfflineDownloadStatus.FAILED.name,
                        progressPercent = 0,
                        downloadedAt = System.currentTimeMillis()
                    )
                )
            } finally {
                activeJobs.remove(track.id)
            }
        }
        activeJobs[track.id] = job
    }

    fun downloadTracks(tracks: List<Track>) {
        tracks.forEach { downloadTrack(it) }
    }

    fun removeDownload(trackId: String) {
        activeJobs.remove(trackId)?.cancel()
        scope.launch {
            File(vaultDir, "$trackId.wav").delete()
            File(vaultDir, "$trackId.wav.part").delete()
            dao.deleteById(trackId)
        }
    }

    fun clearAllDownloads() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        scope.launch {
            vaultDir.listFiles()?.forEach { it.delete() }
            dao.deleteAll()
        }
    }

    suspend fun resolvePlaybackUri(track: Track, offlineModeEnabled: Boolean): Uri? =
        withContext(Dispatchers.IO) {
            val cached = dao.getById(track.id)
            if (cached != null && cached.status == OfflineDownloadStatus.DOWNLOADED) {
                val file = File(cached.localFilePath)
                if (file.exists() && file.length() > 0L) {
                    return@withContext Uri.fromFile(file)
                }
            }
            if (offlineModeEnabled) {
                null
            } else {
                Uri.parse("android.resource://${appContext.packageName}/${track.rawResId}")
            }
        }

    fun isTrackAvailableOffline(entity: OfflineTrackEntity?): Boolean {
        if (entity == null || entity.status != OfflineDownloadStatus.DOWNLOADED) return false
        val file = File(entity.localFilePath)
        return file.exists() && file.length() > 0L
    }
}
