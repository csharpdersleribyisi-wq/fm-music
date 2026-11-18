package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiForYouInsights
import com.example.ai.SonoraAiService
import com.example.audio.LiveAudioEvent
import com.example.audio.LiveAudioRecorder
import com.example.data.catalog.MusicCatalog
import com.example.data.catalog.PersonalizationEngine
import com.example.data.firebase.MusicCloudRepository
import com.example.data.local.OfflineTrackEntity
import com.example.data.local.status
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.ListeningHabitSummary
import com.example.data.model.OfflineDownloadStatus
import com.example.data.model.PersonalizedMix
import com.example.data.model.PlayHistoryEntry
import com.example.data.model.PublicPlaylist
import com.example.data.model.RecommendedTrack
import com.example.data.model.Track
import com.example.data.model.TranscriptSegment
import com.example.data.model.UiState
import com.example.data.model.UserPlaylist
import com.example.data.model.UserTasteProfile
import com.example.data.offline.OfflineDownloadManager
import com.example.player.EqualizerPreset
import com.example.player.EqualizerSettings
import com.example.player.PlaybackState
import com.example.player.SonoraPlayerController
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryFilter {
    ALL,
    PLAYLISTS,
    COLLABORATIVE,
    DOWNLOADED,
    LIKED
}

data class SonoraHomeData(
    val profile: UserTasteProfile?,
    val playlists: List<UserPlaylist>,
    val collaborativePlaylists: List<CollaborativePlaylist>,
    val publicPlaylists: List<PublicPlaylist>,
    val likedTrackIds: Set<String>,
    val playHistory: List<PlayHistoryEntry>,
    val personalizedMixes: List<PersonalizedMix>,
    val forYouRecommendations: List<RecommendedTrack>,
    val habitSummary: ListeningHabitSummary,
    val offlineMap: Map<String, OfflineTrackEntity>
) {
    fun isPlaylistPublished(playlistId: String, currentUserId: String): Boolean =
        publicPlaylists.any { it.ownerId == currentUserId && it.sourcePlaylistId == playlistId }
    val offlineModeEnabled: Boolean
        get() = profile?.offlineModeEnabled ?: false

    val downloadedTracks: List<Track>
        get() = MusicCatalog.tracks.filter {
            offlineMap[it.id]?.status == OfflineDownloadStatus.DOWNLOADED
        }

    val totalOfflineBytes: Long
        get() = offlineMap.values
            .filter { it.status == OfflineDownloadStatus.DOWNLOADED }
            .sumOf { it.fileSizeBytes }

    val recentlyPlayedTracks: List<Track>
        get() = playHistory
            .mapNotNull { MusicCatalog.getTrack(it.trackId) }
            .distinctBy { it.id }
            .take(8)
}

class SonoraViewModel(
    private val cloudRepository: MusicCloudRepository,
    private val offlineManager: OfflineDownloadManager,
    val playerController: SonoraPlayerController,
    private val aiService: SonoraAiService,
    val currentUserId: String,
    initialDisplayName: String?
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenreFilter = MutableStateFlow<String?>(null)
    val selectedGenreFilter: StateFlow<String?> = _selectedGenreFilter.asStateFlow()

    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    val libraryFilter: StateFlow<LibraryFilter> = _libraryFilter.asStateFlow()

    private val _aiInsightsState = MutableStateFlow<UiState<AiForYouInsights>?>(null)
    val aiInsightsState: StateFlow<UiState<AiForYouInsights>?> = _aiInsightsState.asStateFlow()

    private val _transcribeRunning = MutableStateFlow(false)
    val transcribeRunning: StateFlow<Boolean> = _transcribeRunning.asStateFlow()

    private val _transcribeSourceLabel = MutableStateFlow("Microphone Feed")
    val transcribeSourceLabel: StateFlow<String> = _transcribeSourceLabel.asStateFlow()

    private val _liveMicLevel = MutableStateFlow(0f)
    val liveMicLevel: StateFlow<Float> = _liveMicLevel.asStateFlow()

    private val _transcriptSegments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val transcriptSegments: StateFlow<List<TranscriptSegment>> = _transcriptSegments.asStateFlow()

    private val _transcribeError = MutableStateFlow<String?>(null)
    val transcribeError: StateFlow<String?> = _transcribeError.asStateFlow()

    private var transcriptionJob: Job? = null

    val playbackState: StateFlow<PlaybackState> = playerController.state
    val equalizerState: StateFlow<EqualizerSettings> = playerController.equalizerState

    init {
        viewModelScope.launch {
            cloudRepository.ensureUserProfile(currentUserId, initialDisplayName)
                .onFailure { err ->
                    Log.w(TAG, "Profile bootstrap failed", err)
                }
        }
        viewModelScope.launch {
            playerController.state.collect { pState ->
                pState.statusNotice?.let { notice ->
                    _events.tryEmit(notice)
                    playerController.clearNotice()
                }
            }
        }
        viewModelScope.launch {
            playerController.state
                .map { it.currentTrack }
                .filterNotNull()
                .distinctUntilChangedBy { it.id }
                .collect { track ->
                    cloudRepository.recordPlay(currentUserId, track)
                }
        }
    }

    override fun onCleared() {
        transcriptionJob?.cancel()
        playerController.release()
        super.onCleared()
    }

    private val profileFlow = cloudRepository.observeProfile(currentUserId)
    private val playlistsFlow = cloudRepository.observePlaylists(currentUserId)
    private val collabPlaylistsFlow = cloudRepository.observeCollaborativePlaylists(currentUserId)
    private val publicPlaylistsFlow = cloudRepository.observePublicPlaylists(currentUserId)
    private val likedFlow = cloudRepository.observeLikedTracks(currentUserId)
    private val historyFlow = cloudRepository.observePlayHistory(currentUserId)
    private val offlineFlow = offlineManager.observeOfflineMap()

    @Suppress("UNCHECKED_CAST")
    val homeState: StateFlow<UiState<SonoraHomeData>> = combine(
        profileFlow,
        playlistsFlow,
        collabPlaylistsFlow,
        publicPlaylistsFlow,
        likedFlow,
        historyFlow,
        offlineFlow
    ) { args ->
        val profile = args[0] as UserTasteProfile?
        val playlists = args[1] as List<UserPlaylist>
        val collabPlaylists = args[2] as List<CollaborativePlaylist>
        val publicPlaylists = args[3] as List<PublicPlaylist>
        val liked = args[4] as List<com.example.data.model.LikedTrack>
        val history = args[5] as List<PlayHistoryEntry>
        val offlineMap = args[6] as Map<String, OfflineTrackEntity>

        val likedIds = liked.map { it.trackId }.toSet()
        val mixes = PersonalizationEngine.generatePersonalizedMixes(
            profile = profile,
            likedTrackIds = likedIds,
            playHistory = history
        )
        val recommendations = PersonalizationEngine.generateForYouRecommendations(
            profile = profile,
            playlists = playlists,
            collaborativePlaylists = collabPlaylists,
            likedTrackIds = likedIds,
            playHistory = history
        )
        val habitSummary = PersonalizationEngine.analyzeListeningHabits(
            profile = profile,
            playlists = playlists,
            collaborativePlaylists = collabPlaylists,
            likedTrackIds = likedIds,
            playHistory = history
        )
        SonoraHomeData(
            profile = profile,
            playlists = playlists,
            collaborativePlaylists = collabPlaylists,
            publicPlaylists = publicPlaylists,
            likedTrackIds = likedIds,
            playHistory = history,
            personalizedMixes = mixes,
            forYouRecommendations = recommendations,
            habitSummary = habitSummary,
            offlineMap = offlineMap
        )
    }
        .map<SonoraHomeData, UiState<SonoraHomeData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Sonora home stream", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to sync your Sonora library"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenreFilter(genre: String?) {
        _selectedGenreFilter.value = if (_selectedGenreFilter.value == genre) null else genre
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    fun playTrack(track: Track, queue: List<Track> = MusicCatalog.tracks) {
        val offlineOnly = (homeState.value as? UiState.Success)?.data?.offlineModeEnabled ?: false
        playerController.playQueue(queue, track, offlineOnly)
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) = playerController.selectEqualizerPreset(preset)

    fun updateEqualizerBand(bandIndex: Int, gainDb: Float) =
        playerController.updateEqualizerBand(bandIndex, gainDb)

    fun setEqualizerEnabled(enabled: Boolean) = playerController.setEqualizerEnabled(enabled)

    fun setEqualizerPreamp(preampDb: Float) = playerController.setEqualizerPreamp(preampDb)

    fun resetEqualizer() = playerController.resetEqualizer()

    fun refreshAiTasteInsights() {
        val data = (homeState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _aiInsightsState.value = UiState.Loading
            try {
                val allPlaylistTrackIds = (data.playlists.flatMap { it.trackIds } +
                    data.collaborativePlaylists.flatMap { it.trackIds }).distinct()
                val insights = aiService.generateForYouInsights(
                    favoriteGenres = data.profile?.favoriteGenres.orEmpty(),
                    preferredMood = data.profile?.preferredMood ?: "Focus",
                    topPlayedTracks = data.habitSummary.mostPlayedTracks,
                    likedTracks = MusicCatalog.resolveTracks(data.likedTrackIds.toList()),
                    playlistTracks = MusicCatalog.resolveTracks(allPlaylistTrackIds)
                )
                _aiInsightsState.value = UiState.Success(insights)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI taste analysis timed out", e)
                _aiInsightsState.value = UiState.Error(
                    "AI taste analysis timed out after 60 seconds. Please try again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI taste analysis failed", e)
                _aiInsightsState.value = UiState.Error(
                    e.localizedMessage ?: "Failed to analyze your listening habits."
                )
            }
        }
    }

    fun startLiveMicTranscription() {
        transcriptionJob?.cancel()
        _transcribeError.value = null
        _transcribeSourceLabel.value = "Live Microphone Feed"
        _transcribeRunning.value = true

        transcriptionJob = viewModelScope.launch {
            try {
                LiveAudioRecorder.captureStream().collect { event ->
                    when (event) {
                        is LiveAudioEvent.Level -> {
                            _liveMicLevel.value = event.rmsNormalized
                        }
                        is LiveAudioEvent.WavChunk -> {
                            val segId = "mic_${System.currentTimeMillis()}_${event.chunkIndex}"
                            val ts = formatCurrentTimestamp()
                            appendOrUpdateSegment(
                                TranscriptSegment(
                                    id = segId,
                                    sourceLabel = "MIC LIVE • #${event.chunkIndex}",
                                    timestampLabel = ts,
                                    text = "Transcribing live audio feed…",
                                    streaming = true
                                )
                            )
                            streamWavIntoSegment(
                                segmentId = segId,
                                sourceLabel = "MIC LIVE • #${event.chunkIndex}",
                                timestampLabel = ts,
                                wavBytes = event.wavBytes,
                                feedLabel = "Live Microphone Chunk #${event.chunkIndex}",
                                isAmbientMic = true
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live mic transcription error", e)
                _transcribeError.value = e.localizedMessage ?: "Microphone transcription failed."
            } finally {
                _liveMicLevel.value = 0f
                _transcribeRunning.value = false
            }
        }
    }

    fun transcribeTrackAudioFeed(track: Track) {
        transcriptionJob?.cancel()
        _transcribeError.value = null
        _transcribeSourceLabel.value = "${track.title} — ${track.artist}"
        _transcribeRunning.value = true
        _liveMicLevel.value = 0.65f

        transcriptionJob = viewModelScope.launch {
            val segId = "track_${track.id}_${System.currentTimeMillis()}"
            val ts = formatCurrentTimestamp()
            try {
                val wavBytes = aiService.loadTrackWavBytes(track)
                appendOrUpdateSegment(
                    TranscriptSegment(
                        id = segId,
                        sourceLabel = "AUDIO FEED • ${track.title}",
                        timestampLabel = ts,
                        text = "Listening to ${track.title} audio stream…",
                        streaming = true
                    )
                )
                streamWavIntoSegment(
                    segmentId = segId,
                    sourceLabel = "AUDIO FEED • ${track.title}",
                    timestampLabel = ts,
                    wavBytes = wavBytes,
                    feedLabel = "${track.title} by ${track.artist} (${track.genre}, ${track.bpm} BPM)",
                    isAmbientMic = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Track feed transcription error", e)
                _transcribeError.value = e.localizedMessage ?: "Failed to transcribe audio feed."
            } finally {
                _liveMicLevel.value = 0f
                _transcribeRunning.value = false
            }
        }
    }

    private suspend fun streamWavIntoSegment(
        segmentId: String,
        sourceLabel: String,
        timestampLabel: String,
        wavBytes: ByteArray,
        feedLabel: String,
        isAmbientMic: Boolean
    ) {
        val builder = StringBuilder()
        try {
            aiService.transcribeAudioBytesStream(
                wavBytes = wavBytes,
                feedLabel = feedLabel,
                isAmbientMic = isAmbientMic
            ).collect { chunk ->
                builder.append(chunk)
                appendOrUpdateSegment(
                    TranscriptSegment(
                        id = segmentId,
                        sourceLabel = sourceLabel,
                        timestampLabel = timestampLabel,
                        text = builder.toString(),
                        streaming = true
                    )
                )
            }
            val finalText = builder.toString().trim().ifEmpty { "[No speech or lyrics detected in feed]" }
            appendOrUpdateSegment(
                TranscriptSegment(
                    id = segmentId,
                    sourceLabel = sourceLabel,
                    timestampLabel = timestampLabel,
                    text = finalText,
                    streaming = false
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out", e)
            _transcribeError.value = "Transcription timed out after 60 seconds."
            appendOrUpdateSegment(
                TranscriptSegment(
                    id = segmentId,
                    sourceLabel = sourceLabel,
                    timestampLabel = timestampLabel,
                    text = builder.toString().ifBlank { "[Transcription timed out]" },
                    streaming = false
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming transcription chunk failed", e)
            _transcribeError.value = e.localizedMessage ?: "Transcription error occurred."
            appendOrUpdateSegment(
                TranscriptSegment(
                    id = segmentId,
                    sourceLabel = sourceLabel,
                    timestampLabel = timestampLabel,
                    text = "[Error: ${e.localizedMessage ?: "Failed to transcribe feed"}]",
                    streaming = false
                )
            )
        }
    }

    private fun appendOrUpdateSegment(segment: TranscriptSegment) {
        val current = _transcriptSegments.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.id == segment.id }
        if (existingIdx >= 0) {
            current[existingIdx] = segment
        } else {
            current.add(0, segment)
        }
        _transcriptSegments.value = current.take(40)
    }

    fun stopTranscription() {
        transcriptionJob?.cancel()
        transcriptionJob = null
        _liveMicLevel.value = 0f
        _transcribeRunning.value = false
    }

    fun clearTranscript() {
        _transcriptSegments.value = emptyList()
        _transcribeError.value = null
    }

    fun toggleLike(track: Track) {
        val likedIds = (homeState.value as? UiState.Success)?.data?.likedTrackIds ?: emptySet()
        val currentlyLiked = track.id in likedIds
        viewModelScope.launch {
            cloudRepository.toggleLikeTrack(currentUserId, track, currentlyLiked)
                .onSuccess {
                    _events.tryEmit(
                        if (currentlyLiked) "Removed ${track.title} from Liked Songs"
                        else "Added ${track.title} to Liked Songs"
                    )
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Unable to update Liked Songs")
                }
        }
    }

    fun toggleOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            cloudRepository.setOfflineMode(currentUserId, enabled)
                .onSuccess {
                    _events.tryEmit(
                        if (enabled) "Offline Mode enabled — streaming paused"
                        else "Offline Mode off — full streaming restored"
                    )
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not update Offline Mode")
                }
        }
    }

    fun updateTastePreferences(
        displayName: String,
        username: String,
        favoriteGenres: List<String>,
        preferredMood: String
    ) {
        viewModelScope.launch {
            cloudRepository.updateTastePreferences(
                userId = currentUserId,
                displayName = displayName,
                username = username,
                favoriteGenres = favoriteGenres,
                preferredMood = preferredMood
            ).onSuccess {
                _events.tryEmit("Updated taste profile & @${MusicCloudRepository.normalizeHandle(username)}")
            }.onFailure {
                _events.tryEmit(it.message ?: "Failed to save taste preferences")
            }
        }
    }

    fun downloadTrack(track: Track) {
        offlineManager.downloadTrack(track)
        _events.tryEmit("Caching ${track.title} for offline playback")
    }

    fun removeDownload(track: Track) {
        offlineManager.removeDownload(track.id)
        _events.tryEmit("Removed ${track.title} from offline vault")
    }

    fun downloadTracks(tracks: List<Track>, label: String) {
        if (tracks.isEmpty()) return
        offlineManager.downloadTracks(tracks)
        _events.tryEmit("Downloading ${tracks.size} tracks from $label")
    }

    fun clearOfflineVault() {
        offlineManager.clearAllDownloads()
        _events.tryEmit("Cleared offline vault storage")
    }

    fun createPlaylist(
        title: String,
        description: String,
        moodTag: String,
        coverKey: String,
        initialTrackIds: List<String> = emptyList(),
        personalized: Boolean = false,
        publishOnline: Boolean = false
    ) {
        val profile = (homeState.value as? UiState.Success)?.data?.profile
        viewModelScope.launch {
            cloudRepository.createPlaylist(
                userId = currentUserId,
                title = title,
                description = description,
                moodTag = moodTag,
                coverKey = coverKey,
                trackIds = initialTrackIds,
                personalized = personalized
            ).onSuccess { newPlaylistId ->
                if (publishOnline) {
                    val createdPlaylist = UserPlaylist(
                        id = newPlaylistId,
                        userId = currentUserId,
                        title = title,
                        description = description,
                        moodTag = moodTag,
                        coverKey = coverKey,
                        trackIds = initialTrackIds,
                        personalized = personalized
                    )
                    cloudRepository.publishPlaylistOnline(
                        userId = currentUserId,
                        ownerDisplayName = profile?.displayName ?: "Listener",
                        ownerUsername = profile?.username ?: "",
                        playlist = createdPlaylist
                    ).onSuccess {
                        _events.tryEmit("Published \"$title\" online for everyone!")
                    }.onFailure {
                        _events.tryEmit("Saved playlist locally, but online publish failed: ${it.message}")
                    }
                } else {
                    _events.tryEmit("Saved playlist \"$title\"")
                }
            }.onFailure {
                _events.tryEmit(it.message ?: "Could not create playlist")
            }
        }
    }

    fun togglePublishPlaylistOnline(playlist: UserPlaylist) {
        val data = (homeState.value as? UiState.Success)?.data ?: return
        val currentlyPublished = data.isPlaylistPublished(playlist.id, currentUserId)
        viewModelScope.launch {
            if (currentlyPublished) {
                cloudRepository.unpublishPlaylistOnline(currentUserId, playlist.id)
                    .onSuccess {
                        _events.tryEmit("Removed \"${playlist.title}\" from online public feed")
                    }
                    .onFailure {
                        _events.tryEmit(it.message ?: "Could not unpublish playlist")
                    }
            } else {
                cloudRepository.publishPlaylistOnline(
                    userId = currentUserId,
                    ownerDisplayName = data.profile?.displayName ?: "Listener",
                    ownerUsername = data.profile?.username ?: "",
                    playlist = playlist
                ).onSuccess {
                    _events.tryEmit("Published \"${playlist.title}\" online for everyone!")
                }.onFailure {
                    _events.tryEmit(it.message ?: "Could not publish playlist online")
                }
            }
        }
    }

    fun savePublicPlaylistToLibrary(publicPlaylist: PublicPlaylist) {
        createPlaylist(
            title = publicPlaylist.title,
            description = "Curated by ${publicPlaylist.ownerName} • ${publicPlaylist.description}".take(240),
            moodTag = publicPlaylist.moodTag,
            coverKey = publicPlaylist.coverKey,
            initialTrackIds = publicPlaylist.trackIds,
            personalized = false,
            publishOnline = false
        )
    }

    fun unpublishPublicPlaylist(publicPlaylist: PublicPlaylist) {
        if (publicPlaylist.ownerId != currentUserId) return
        viewModelScope.launch {
            cloudRepository.unpublishPlaylistOnline(currentUserId, publicPlaylist.sourcePlaylistId)
                .onSuccess {
                    _events.tryEmit("Removed \"${publicPlaylist.title}\" from online public feed")
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not unpublish playlist")
                }
        }
    }

    fun savePersonalizedMixAsPlaylist(mix: PersonalizedMix) {
        createPlaylist(
            title = mix.title,
            description = mix.subtitle,
            moodTag = mix.moodTag,
            coverKey = mix.coverKey,
            initialTrackIds = mix.tracks.map { it.id },
            personalized = true
        )
    }

    fun addTrackToPlaylist(playlist: UserPlaylist, track: Track) {
        if (track.id in playlist.trackIds) {
            _events.tryEmit("${track.title} is already in ${playlist.title}")
            return
        }
        val updated = (playlist.trackIds + track.id).take(50)
        viewModelScope.launch {
            cloudRepository.updatePlaylistTracks(currentUserId, playlist.id, updated)
                .onSuccess {
                    _events.tryEmit("Added ${track.title} to ${playlist.title}")
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not update playlist")
                }
        }
    }

    fun removeTrackFromPlaylist(playlist: UserPlaylist, trackId: String) {
        val updated = playlist.trackIds.filterNot { it == trackId }
        viewModelScope.launch {
            cloudRepository.updatePlaylistTracks(currentUserId, playlist.id, updated)
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not remove track")
                }
        }
    }

    fun renamePlaylist(
        playlist: UserPlaylist,
        newTitle: String,
        newDescription: String,
        newMoodTag: String,
        newCoverKey: String
    ) {
        viewModelScope.launch {
            cloudRepository.updatePlaylistDetails(
                userId = currentUserId,
                playlistId = playlist.id,
                title = newTitle,
                description = newDescription,
                moodTag = newMoodTag,
                coverKey = newCoverKey
            ).onSuccess {
                _events.tryEmit("Updated playlist \"$newTitle\"")
            }.onFailure {
                _events.tryEmit(it.message ?: "Could not update playlist")
            }
        }
    }

    fun deletePlaylist(playlist: UserPlaylist) {
        viewModelScope.launch {
            cloudRepository.deletePlaylist(currentUserId, playlist.id)
                .onSuccess {
                    _events.tryEmit("Deleted \"${playlist.title}\"")
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not delete playlist")
                }
        }
    }

    // region Collaborative Playlists

    fun createCollaborativePlaylist(
        title: String,
        description: String,
        moodTag: String,
        coverKey: String,
        initialTrackIds: List<String> = emptyList(),
        onCreated: ((CollaborativePlaylist) -> Unit)? = null
    ) {
        val ownerName = (homeState.value as? UiState.Success)?.data?.profile?.displayName ?: "Listener"
        viewModelScope.launch {
            cloudRepository.createCollaborativePlaylist(
                userId = currentUserId,
                ownerDisplayName = ownerName,
                title = title,
                description = description,
                moodTag = moodTag,
                coverKey = coverKey,
                initialTrackIds = initialTrackIds
            ).onSuccess { created ->
                _events.tryEmit("Collaborative playlist created • Code ${created.inviteCode}")
                onCreated?.invoke(created)
            }.onFailure {
                _events.tryEmit(it.message ?: "Could not create collaborative playlist")
            }
        }
    }

    fun joinCollaborativePlaylistByCode(
        codeOrLink: String,
        onJoined: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            cloudRepository.joinCollaborativePlaylistByCode(currentUserId, codeOrLink)
                .onSuccess { playlistId ->
                    _events.tryEmit("Joined collaborative playlist!")
                    onJoined?.invoke(playlistId)
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Unable to join collaborative playlist")
                }
        }
    }

    fun inviteFriendToCollab(playlist: CollaborativePlaylist, username: String) {
        viewModelScope.launch {
            cloudRepository.inviteFriendByUsername(playlist, username)
                .onSuccess { friendName ->
                    _events.tryEmit("Invited $friendName to ${playlist.title}")
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not invite user")
                }
        }
    }

    fun addTrackToCollab(playlist: CollaborativePlaylist, track: Track) {
        if (track.id in playlist.trackIds) {
            _events.tryEmit("${track.title} is already in ${playlist.title}")
            return
        }
        val updated = (playlist.trackIds + track.id).take(50)
        viewModelScope.launch {
            cloudRepository.updateCollaborativeTracks(playlist.id, updated)
                .onSuccess {
                    _events.tryEmit("Added ${track.title} to ${playlist.title}")
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not update collaborative playlist")
                }
        }
    }

    fun removeTrackFromCollab(playlist: CollaborativePlaylist, trackId: String) {
        val updated = playlist.trackIds.filterNot { it == trackId }
        viewModelScope.launch {
            cloudRepository.updateCollaborativeTracks(playlist.id, updated)
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not remove track")
                }
        }
    }

    fun moveTrackInCollab(playlist: CollaborativePlaylist, trackId: String, delta: Int) {
        val list = playlist.trackIds.toMutableList()
        val fromIdx = list.indexOf(trackId)
        val toIdx = fromIdx + delta
        if (fromIdx < 0 || toIdx !in list.indices) return
        val moved = list.removeAt(fromIdx)
        list.add(toIdx, moved)
        viewModelScope.launch {
            cloudRepository.updateCollaborativeTracks(playlist.id, list)
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not reorder track")
                }
        }
    }

    fun leaveCollaborativePlaylist(playlist: CollaborativePlaylist) {
        viewModelScope.launch {
            cloudRepository.leaveCollaborativePlaylist(currentUserId, playlist.id)
                .onSuccess {
                    _events.tryEmit("Left \"${playlist.title}\"")
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not leave playlist")
                }
        }
    }

    fun deleteCollaborativePlaylist(playlist: CollaborativePlaylist) {
        viewModelScope.launch {
            cloudRepository.deleteCollaborativePlaylist(playlist)
                .onSuccess {
                    _events.tryEmit("Deleted collaborative playlist \"${playlist.title}\"")
                }
                .onFailure {
                    _events.tryEmit(it.message ?: "Could not delete collaborative playlist")
                }
        }
    }

    // endregion

    fun emitNotice(message: String) {
        _events.tryEmit(message)
    }

    private fun formatCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private companion object {
        const val TAG = "SonoraVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
