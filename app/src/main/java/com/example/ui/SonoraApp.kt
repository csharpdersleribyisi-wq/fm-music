package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.ai.SonoraAiService
import com.example.data.catalog.MusicCatalog
import com.example.data.firebase.MusicCloudRepository
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.Track
import com.example.data.model.UiState
import com.example.data.model.UserPlaylist
import com.example.data.offline.OfflineDownloadManager
import com.example.player.SonoraPlayerController
import com.example.ui.auth.signOutUser
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.AddTrackFromCatalogSheet
import com.example.ui.components.CreateOrEditPlaylistDialog
import com.example.ui.components.EqualizerSheet
import com.example.ui.components.InviteFriendDialog
import com.example.ui.components.JoinCollaborativePlaylistDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.SleepTimerSheet
import com.example.ui.components.SocialShareSheet
import com.example.ui.components.SocialShareTarget
import com.example.ui.components.TastePreferencesSheet
import com.example.ui.screens.CollectionDetailTarget
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LiveTranscribeScreen
import com.example.ui.screens.OfflineVaultScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.theme.SonoraBg
import com.example.ui.theme.SonoraCoral
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceHigh
import com.example.ui.theme.SonoraTextSecondary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object DiscoverRoute

@Serializable
data object LibraryRoute

@Serializable
data object TranscribeRoute

@Serializable
data object OfflineRoute

@Serializable
data class CollectionDetailRoute(
    val kind: String,
    val targetId: String
)

private data class NavTabItem(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun SonoraApp(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?,
    pendingInviteCode: String? = null,
    onConsumePendingInvite: () -> Unit = {},
    pendingPublicPlaylistId: String? = null,
    onConsumePendingPublicPlaylist: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SonoraViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val cloudRepo = MusicCloudRepository(db)
                val offlineManager = OfflineDownloadManager(app)
                val playerController = SonoraPlayerController(app, offlineManager)
                val aiService = SonoraAiService(app)
                SonoraViewModel(
                    cloudRepository = cloudRepo,
                    offlineManager = offlineManager,
                    playerController = playerController,
                    aiService = aiService,
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName
                )
            }
        }
    )

    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenreFilter.collectAsStateWithLifecycle()
    val libraryFilter by viewModel.libraryFilter.collectAsStateWithLifecycle()
    val aiInsightsState by viewModel.aiInsightsState.collectAsStateWithLifecycle()

    val transcribeRunning by viewModel.transcribeRunning.collectAsStateWithLifecycle()
    val transcribeSourceLabel by viewModel.transcribeSourceLabel.collectAsStateWithLifecycle()
    val liveMicLevel by viewModel.liveMicLevel.collectAsStateWithLifecycle()
    val transcriptSegments by viewModel.transcriptSegments.collectAsStateWithLifecycle()
    val transcribeError by viewModel.transcribeError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showNowPlaying by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showCreateCollabDialog by remember { mutableStateOf(false) }
    var showJoinCollabDialog by remember { mutableStateOf<String?>(null) }
    var editingPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var pendingTrackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var prefillTrackForNewPlaylist by remember { mutableStateOf<Track?>(null) }
    var showTasteSheet by remember { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<SocialShareTarget?>(null) }
    var invitingCollabPlaylist by remember { mutableStateOf<CollaborativePlaylist?>(null) }
    var addingTrackToCollab by remember { mutableStateOf<CollaborativePlaylist?>(null) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    LaunchedEffect(pendingInviteCode) {
        if (!pendingInviteCode.isNullOrBlank()) {
            showJoinCollabDialog = pendingInviteCode
            onConsumePendingInvite()
        }
    }

    LaunchedEffect(pendingPublicPlaylistId) {
        if (!pendingPublicPlaylistId.isNullOrBlank()) {
            navController.navigate(
                CollectionDetailRoute(kind = "public", targetId = pendingPublicPlaylistId)
            )
            onConsumePendingPublicPlaylist()
        }
    }

    val tabs = remember {
        listOf(
            NavTabItem(HomeRoute, R.string.nav_for_you, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_for_you"),
            NavTabItem(DiscoverRoute, R.string.nav_discover, Icons.Rounded.Search, Icons.Outlined.Search, "nav_discover"),
            NavTabItem(LibraryRoute, R.string.nav_library, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
            NavTabItem(TranscribeRoute, R.string.nav_transcribe, Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq, "nav_transcribe"),
            NavTabItem(OfflineRoute, R.string.nav_offline, Icons.Rounded.OfflineBolt, Icons.Outlined.OfflineBolt, "nav_offline")
        )
    }

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SonoraBg)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = SonoraBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SonoraSurface)
                            .navigationBarsPadding()
                    ) {
                        if (playbackState.currentTrack != null) {
                            val likedIds = (homeState as? UiState.Success)?.data?.likedTrackIds ?: emptySet()
                            MiniPlayerBar(
                                playbackState = playbackState,
                                isLiked = playbackState.currentTrack!!.id in likedIds,
                                onExpandClick = { showNowPlaying = true },
                                onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                                onToggleLike = { viewModel.toggleLike(playbackState.currentTrack!!) }
                            )
                        }
                        NavigationBar(
                            containerColor = SonoraSurface,
                            tonalElevation = 0.dp
                        ) {
                            tabs.forEach { tab ->
                                val selected = when (tab.route) {
                                    is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                    is DiscoverRoute -> currentDestination?.hasRoute<DiscoverRoute>() == true
                                    is LibraryRoute -> currentDestination?.hasRoute<LibraryRoute>() == true
                                    is TranscribeRoute -> currentDestination?.hasRoute<TranscribeRoute>() == true
                                    is OfflineRoute -> currentDestination?.hasRoute<OfflineRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SonoraEmerald,
                                        selectedTextColor = SonoraEmerald,
                                        indicatorColor = SonoraSurfaceHigh,
                                        unselectedIconColor = SonoraTextSecondary,
                                        unselectedTextColor = SonoraTextSecondary
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = SonoraSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        tabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                is DiscoverRoute -> currentDestination?.hasRoute<DiscoverRoute>() == true
                                is LibraryRoute -> currentDestination?.hasRoute<LibraryRoute>() == true
                                is TranscribeRoute -> currentDestination?.hasRoute<TranscribeRoute>() == true
                                is OfflineRoute -> currentDestination?.hasRoute<OfflineRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SonoraEmerald,
                                    selectedTextColor = SonoraEmerald,
                                    indicatorColor = SonoraSurfaceHigh,
                                    unselectedIconColor = SonoraTextSecondary,
                                    unselectedTextColor = SonoraTextSecondary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (val state = homeState) {
                        UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SonoraEmerald)
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.message,
                                    color = SonoraCoral,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        is UiState.Success -> {
                            val data = state.data
                            val bottomPad = PaddingValues(
                                bottom = if (useNavRail && playbackState.currentTrack != null) {
                                    88.dp + innerPadding.calculateBottomPadding()
                                } else {
                                    innerPadding.calculateBottomPadding()
                                }
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 840.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = HomeRoute
                                    ) {
                                        composable<HomeRoute> {
                                            HomeScreen(
                                                data = data,
                                                playbackState = playbackState,
                                                equalizerSettings = equalizerState,
                                                aiInsightsState = aiInsightsState,
                                                onRefreshAiInsights = { viewModel.refreshAiTasteInsights() },
                                                onOpenEqualizer = { showEqualizerSheet = true },
                                                onOpenSleepTimer = { showSleepTimerSheet = true },
                                                onOpenTasteProfile = { showTasteSheet = true },
                                                onToggleOfflineMode = { viewModel.toggleOfflineMode(it) },
                                                onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                                                onToggleLike = { viewModel.toggleLike(it) },
                                                onDownloadTrack = { viewModel.downloadTrack(it) },
                                                onRemoveDownload = { viewModel.removeDownload(it) },
                                                onDownloadTracks = { tracks, label ->
                                                    viewModel.downloadTracks(tracks, label)
                                                },
                                                onAddToPlaylist = { pendingTrackForPlaylist = it },
                                                onShareTrack = { shareTarget = SocialShareTarget.TrackTarget(it) },
                                                onOpenMix = { mix ->
                                                    navController.navigate(
                                                        CollectionDetailRoute(kind = "mix", targetId = mix.id)
                                                    )
                                                },
                                                onSaveMix = { viewModel.savePersonalizedMixAsPlaylist(it) },
                                                onOpenPlaylist = { pl ->
                                                    navController.navigate(
                                                        CollectionDetailRoute(kind = "playlist", targetId = pl.id)
                                                    )
                                                },
                                                onOpenLikedSongs = {
                                                    navController.navigate(
                                                        CollectionDetailRoute(kind = "liked", targetId = "liked")
                                                    )
                                                },
                                                bottomContentPadding = bottomPad
                                            )
                                        }

                                        composable<DiscoverRoute> {
                                            DiscoverScreen(
                                                data = data,
                                                playbackState = playbackState,
                                                searchQuery = searchQuery,
                                                selectedGenre = selectedGenre,
                                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                                onSelectGenre = { viewModel.setGenreFilter(it) },
                                                onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                                                onToggleLike = { viewModel.toggleLike(it) },
                                                onDownloadTrack = { viewModel.downloadTrack(it) },
                                                onRemoveDownload = { viewModel.removeDownload(it) },
                                                onAddToPlaylist = { pendingTrackForPlaylist = it },
                                                onShareTrack = { shareTarget = SocialShareTarget.TrackTarget(it) },
                                                onOpenAlbum = { album ->
                                                    navController.navigate(
                                                        CollectionDetailRoute(kind = "album", targetId = album.title)
                                                    )
                                                },
                                                onShareAlbum = { album ->
                                                    shareTarget = SocialShareTarget.AlbumTarget(album)
                                                },
                                                onOpenPublicPlaylist = { pub ->
                                                    navController.navigate(
                                                        CollectionDetailRoute(kind = "public", targetId = pub.id)
                                                    )
                                                },
                                                onSharePublicPlaylist = { pub ->
                                                    shareTarget = SocialShareTarget.PlaylistTarget(
                                                        playlistId = pub.sourcePlaylistId,
                                                        title = pub.title,
                                                        trackCount = pub.trackIds.size,
                                                        publicPlaylistId = pub.id
                                                    )
                                                },
                                                bottomContentPadding = bottomPad
                                            )
                                        }

                                        composable<LibraryRoute> {
                                            LibraryScreen(
                                                currentUserId = currentUserId,
                                                data = data,
                                                playbackState = playbackState,
                                                activeFilter = libraryFilter,
                                                onSelectFilter = { viewModel.setLibraryFilter(it) },
                                                onCreatePlaylistClick = {
                                                    prefillTrackForNewPlaylist = null
                                                    showCreatePlaylistDialog = true
                                                },
                                                onCreateCollabPlaylistClick = {
                                                    showCreateCollabDialog = true
                                                },
                                                onJoinCollabClick = {
                                                    showJoinCollabDialog = ""
                                                },
                                                onOpenPlaylist = { pl ->
                                                    navController.navigate(
                                                        CollectionDetailRoute(kind = "playlist", targetId = pl.id)
                                                    )
                                                },
                                                onOpenCollabPlaylist = { collab ->
                                                    navController.navigate(
                                                        CollectionDetailRoute(kind = "collab", targetId = collab.id)
                                                    )
                                                },
                                                onOpenLikedSongs = {
                                                    navController.navigate(
                                                        CollectionDetailRoute(kind = "liked", targetId = "liked")
                                                    )
                                                },
                                                onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                                                onToggleLike = { viewModel.toggleLike(it) },
                                                onDownloadTrack = { viewModel.downloadTrack(it) },
                                                onRemoveDownload = { viewModel.removeDownload(it) },
                                                onDownloadPlaylistTracks = { tracks, label ->
                                                    viewModel.downloadTracks(tracks, label)
                                                },
                                                onAddToPlaylist = { pendingTrackForPlaylist = it },
                                                onShareTrack = { shareTarget = SocialShareTarget.TrackTarget(it) },
                                                onSharePlaylist = { pl ->
                                                    val pubId = if (data.isPlaylistPublished(pl.id, currentUserId)) {
                                                        MusicCloudRepository.publicDocId(currentUserId, pl.id)
                                                    } else null
                                                    shareTarget = SocialShareTarget.PlaylistTarget(
                                                        playlistId = pl.id,
                                                        title = pl.title,
                                                        trackCount = pl.trackIds.size,
                                                        publicPlaylistId = pubId
                                                    )
                                                },
                                                onShareCollabPlaylist = { collab ->
                                                    shareTarget = SocialShareTarget.PlaylistTarget(
                                                        playlistId = collab.id,
                                                        title = collab.title,
                                                        trackCount = collab.trackIds.size,
                                                        collabInviteCode = collab.inviteCode
                                                    )
                                                },
                                                onTogglePublishPlaylist = { pl ->
                                                    viewModel.togglePublishPlaylistOnline(pl)
                                                },
                                                bottomContentPadding = bottomPad
                                            )
                                        }

                                        composable<TranscribeRoute> {
                                            LiveTranscribeScreen(
                                                playbackState = playbackState,
                                                isRunning = transcribeRunning,
                                                sourceLabel = transcribeSourceLabel,
                                                liveMicLevel = liveMicLevel,
                                                segments = transcriptSegments,
                                                errorMessage = transcribeError,
                                                onStartLiveMic = { viewModel.startLiveMicTranscription() },
                                                onTranscribeTrack = { viewModel.transcribeTrackAudioFeed(it) },
                                                onStop = { viewModel.stopTranscription() },
                                                onClear = { viewModel.clearTranscript() },
                                                onNotice = { viewModel.emitNotice(it) },
                                                bottomContentPadding = bottomPad
                                            )
                                        }

                                        composable<OfflineRoute> {
                                            OfflineVaultScreen(
                                                data = data,
                                                playbackState = playbackState,
                                                onToggleOfflineMode = { viewModel.toggleOfflineMode(it) },
                                                onDownloadAllCatalog = {
                                                    viewModel.downloadTracks(MusicCatalog.tracks, "Full Catalog")
                                                },
                                                onClearVault = { viewModel.clearOfflineVault() },
                                                onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                                                onToggleLike = { viewModel.toggleLike(it) },
                                                onDownloadTrack = { viewModel.downloadTrack(it) },
                                                onRemoveDownload = { viewModel.removeDownload(it) },
                                                onAddToPlaylist = { pendingTrackForPlaylist = it },
                                                onShareTrack = { shareTarget = SocialShareTarget.TrackTarget(it) },
                                                bottomContentPadding = bottomPad
                                            )
                                        }

                                        composable<CollectionDetailRoute> { entry ->
                                            val route = entry.toRoute<CollectionDetailRoute>()
                                            val detailTarget = when (route.kind) {
                                                "playlist" -> CollectionDetailTarget.PlaylistTarget(route.targetId)
                                                "collab" -> CollectionDetailTarget.CollabPlaylistTarget(route.targetId)
                                                "public" -> CollectionDetailTarget.PublicPlaylistTarget(route.targetId)
                                                "mix" -> CollectionDetailTarget.MixTarget(route.targetId)
                                                "album" -> CollectionDetailTarget.AlbumTarget(route.targetId)
                                                else -> CollectionDetailTarget.LikedSongsTarget
                                            }
                                            PlaylistDetailScreen(
                                                target = detailTarget,
                                                currentUserId = currentUserId,
                                                data = data,
                                                playbackState = playbackState,
                                                onBack = { navController.popBackStack() },
                                                onPlayTrack = { t, q -> viewModel.playTrack(t, q) },
                                                onToggleLike = { viewModel.toggleLike(it) },
                                                onDownloadTrack = { viewModel.downloadTrack(it) },
                                                onRemoveDownload = { viewModel.removeDownload(it) },
                                                onDownloadAll = { tracks, label ->
                                                    viewModel.downloadTracks(tracks, label)
                                                },
                                                onAddToPlaylist = { pendingTrackForPlaylist = it },
                                                onShareTrack = { shareTarget = SocialShareTarget.TrackTarget(it) },
                                                onSharePlaylist = { pl ->
                                                    val pubId = if (data.isPlaylistPublished(pl.id, currentUserId)) {
                                                        MusicCloudRepository.publicDocId(currentUserId, pl.id)
                                                    } else null
                                                    shareTarget = SocialShareTarget.PlaylistTarget(
                                                        playlistId = pl.id,
                                                        title = pl.title,
                                                        trackCount = pl.trackIds.size,
                                                        publicPlaylistId = pubId
                                                    )
                                                },
                                                onShareCollabPlaylist = { collab ->
                                                    shareTarget = SocialShareTarget.PlaylistTarget(
                                                        playlistId = collab.id,
                                                        title = collab.title,
                                                        trackCount = collab.trackIds.size,
                                                        collabInviteCode = collab.inviteCode
                                                    )
                                                },
                                                onSharePublicPlaylist = { pub ->
                                                    shareTarget = SocialShareTarget.PlaylistTarget(
                                                        playlistId = pub.sourcePlaylistId,
                                                        title = pub.title,
                                                        trackCount = pub.trackIds.size,
                                                        publicPlaylistId = pub.id
                                                    )
                                                },
                                                onShareAlbum = { album ->
                                                    shareTarget = SocialShareTarget.AlbumTarget(album)
                                                },
                                                onSaveMixAsPlaylist = { viewModel.savePersonalizedMixAsPlaylist(it) },
                                                onSavePublicPlaylist = { viewModel.savePublicPlaylistToLibrary(it) },
                                                onTogglePublishPlaylist = { viewModel.togglePublishPlaylistOnline(it) },
                                                onUnpublishPublicPlaylist = { viewModel.unpublishPublicPlaylist(it) },
                                                onEditPlaylist = { editingPlaylist = it },
                                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                                onRemoveTrackFromPlaylist = { pl, tid ->
                                                    viewModel.removeTrackFromPlaylist(pl, tid)
                                                },
                                                onOpenAddTrackToCollab = { addingTrackToCollab = it },
                                                onOpenInviteFriendToCollab = { invitingCollabPlaylist = it },
                                                onMoveCollabTrack = { collab, tid, delta ->
                                                    viewModel.moveTrackInCollab(collab, tid, delta)
                                                },
                                                onRemoveCollabTrack = { collab, tid ->
                                                    viewModel.removeTrackFromCollab(collab, tid)
                                                },
                                                onDeleteCollabPlaylist = { viewModel.deleteCollaborativePlaylist(it) },
                                                onLeaveCollabPlaylist = { viewModel.leaveCollaborativePlaylist(it) },
                                                bottomContentPadding = bottomPad
                                            )
                                        }
                                    }
                                }

                                if (useNavRail && playbackState.currentTrack != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        MiniPlayerBar(
                                            playbackState = playbackState,
                                            isLiked = playbackState.currentTrack!!.id in data.likedTrackIds,
                                            onExpandClick = { showNowPlaying = true },
                                            onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                                            onToggleLike = { viewModel.toggleLike(playbackState.currentTrack!!) }
                                        )
                                    }
                                }
                            }

                            if (showNowPlaying && playbackState.currentTrack != null) {
                                val currentTrack = playbackState.currentTrack!!
                                NowPlayingSheet(
                                    playbackState = playbackState,
                                    equalizerSettings = equalizerState,
                                    isLiked = currentTrack.id in data.likedTrackIds,
                                    offlineEntity = data.offlineMap[currentTrack.id],
                                    onDismiss = { showNowPlaying = false },
                                    onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                                    onSeekTo = { viewModel.playerController.seekTo(it) },
                                    onSkipNext = { viewModel.playerController.skipNext() },
                                    onSkipPrevious = { viewModel.playerController.skipPrevious() },
                                    onToggleShuffle = { viewModel.playerController.toggleShuffle() },
                                    onCycleRepeat = { viewModel.playerController.cycleRepeatMode() },
                                    onToggleLike = { viewModel.toggleLike(currentTrack) },
                                    onDownloadClick = { viewModel.downloadTrack(currentTrack) },
                                    onRemoveDownloadClick = { viewModel.removeDownload(currentTrack) },
                                    onAddToPlaylistClick = { pendingTrackForPlaylist = currentTrack },
                                    onShareTrackClick = {
                                        shareTarget = SocialShareTarget.TrackTarget(currentTrack)
                                    },
                                    onTranscribeTrackClick = {
                                        viewModel.transcribeTrackAudioFeed(currentTrack)
                                        navigateToTab(TranscribeRoute)
                                    },
                                    onOpenEqualizerClick = { showEqualizerSheet = true },
                                    onOpenSleepTimerClick = { showSleepTimerSheet = true },
                                    onSelectQueueTrack = { viewModel.playTrack(it, playbackState.queue) }
                                )
                            }

                            if (showEqualizerSheet) {
                                EqualizerSheet(
                                    settings = equalizerState,
                                    onDismiss = { showEqualizerSheet = false },
                                    onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
                                    onSelectPreset = { viewModel.selectEqualizerPreset(it) },
                                    onUpdateBand = { bandIdx, gainDb ->
                                        viewModel.updateEqualizerBand(bandIdx, gainDb)
                                    },
                                    onUpdatePreamp = { viewModel.setEqualizerPreamp(it) },
                                    onReset = { viewModel.resetEqualizer() }
                                )
                            }

                            if (showSleepTimerSheet) {
                                SleepTimerSheet(
                                    playbackState = playbackState,
                                    onDismiss = { showSleepTimerSheet = false },
                                    onStartTimer = { mins, fade ->
                                        viewModel.startSleepTimer(mins, fade)
                                    },
                                    onExtendTimer = { extraMins ->
                                        viewModel.extendSleepTimer(extraMins)
                                    },
                                    onCancelTimer = { viewModel.cancelSleepTimer() }
                                )
                            }

                            if (showCreatePlaylistDialog) {
                                CreateOrEditPlaylistDialog(
                                    existingPlaylist = null,
                                    isCollaborativeCreation = false,
                                    showPublishOnlineToggle = true,
                                    onDismiss = {
                                        showCreatePlaylistDialog = false
                                        prefillTrackForNewPlaylist = null
                                    },
                                    onConfirm = { title, desc, mood, cover, publishOnline ->
                                        val initialIds = prefillTrackForNewPlaylist?.let { listOf(it.id) }
                                            ?: emptyList()
                                        viewModel.createPlaylist(
                                            title = title,
                                            description = desc,
                                            moodTag = mood,
                                            coverKey = cover,
                                            initialTrackIds = initialIds,
                                            publishOnline = publishOnline
                                        )
                                        showCreatePlaylistDialog = false
                                        prefillTrackForNewPlaylist = null
                                    }
                                )
                            }

                            if (showCreateCollabDialog) {
                                CreateOrEditPlaylistDialog(
                                    existingPlaylist = null,
                                    isCollaborativeCreation = true,
                                    onDismiss = { showCreateCollabDialog = false },
                                    onConfirm = { title, desc, mood, cover, _ ->
                                        viewModel.createCollaborativePlaylist(
                                            title = title,
                                            description = desc,
                                            moodTag = mood,
                                            coverKey = cover
                                        ) { created ->
                                            navController.navigate(
                                                CollectionDetailRoute(kind = "collab", targetId = created.id)
                                            )
                                        }
                                        showCreateCollabDialog = false
                                    }
                                )
                            }

                            showJoinCollabDialog?.let { initialCode ->
                                JoinCollaborativePlaylistDialog(
                                    initialCode = initialCode,
                                    onDismiss = { showJoinCollabDialog = null },
                                    onConfirm = { codeOrLink ->
                                        showJoinCollabDialog = null
                                        viewModel.joinCollaborativePlaylistByCode(codeOrLink) { joinedId ->
                                            navController.navigate(
                                                CollectionDetailRoute(kind = "collab", targetId = joinedId)
                                            )
                                        }
                                    }
                                )
                            }

                            invitingCollabPlaylist?.let { collab ->
                                val liveCollab = data.collaborativePlaylists.firstOrNull { it.id == collab.id } ?: collab
                                InviteFriendDialog(
                                    playlist = liveCollab,
                                    onDismiss = { invitingCollabPlaylist = null },
                                    onInviteByUsername = { username ->
                                        viewModel.inviteFriendToCollab(liveCollab, username)
                                        invitingCollabPlaylist = null
                                    },
                                    onOpenShareLink = {
                                        shareTarget = SocialShareTarget.PlaylistTarget(
                                            playlistId = liveCollab.id,
                                            title = liveCollab.title,
                                            trackCount = liveCollab.trackIds.size,
                                            collabInviteCode = liveCollab.inviteCode
                                        )
                                    }
                                )
                            }

                            addingTrackToCollab?.let { collab ->
                                val liveCollab = data.collaborativePlaylists.firstOrNull { it.id == collab.id } ?: collab
                                AddTrackFromCatalogSheet(
                                    playlistTitle = liveCollab.title,
                                    existingTrackIds = liveCollab.trackIds.toSet(),
                                    onDismiss = { addingTrackToCollab = null },
                                    onSelectTrack = { track ->
                                        viewModel.addTrackToCollab(liveCollab, track)
                                    }
                                )
                            }

                            editingPlaylist?.let { pl ->
                                CreateOrEditPlaylistDialog(
                                    existingPlaylist = pl,
                                    onDismiss = { editingPlaylist = null },
                                    onConfirm = { title, desc, mood, cover, _ ->
                                        viewModel.renamePlaylist(pl, title, desc, mood, cover)
                                        editingPlaylist = null
                                    }
                                )
                            }

                            pendingTrackForPlaylist?.let { track ->
                                AddToPlaylistSheet(
                                    track = track,
                                    playlists = data.playlists,
                                    collaborativePlaylists = data.collaborativePlaylists,
                                    onDismiss = { pendingTrackForPlaylist = null },
                                    onSelectPlaylist = { pl ->
                                        viewModel.addTrackToPlaylist(pl, track)
                                        pendingTrackForPlaylist = null
                                    },
                                    onSelectCollabPlaylist = { collab ->
                                        viewModel.addTrackToCollab(collab, track)
                                        pendingTrackForPlaylist = null
                                    },
                                    onCreateNewPlaylistWithTrack = {
                                        prefillTrackForNewPlaylist = track
                                        pendingTrackForPlaylist = null
                                        showCreatePlaylistDialog = true
                                    }
                                )
                            }

                            shareTarget?.let { target ->
                                SocialShareSheet(
                                    target = target,
                                    onDismiss = { shareTarget = null },
                                    onCopiedNotice = { msg -> viewModel.emitNotice(msg) }
                                )
                            }

                            if (showTasteSheet) {
                                TastePreferencesSheet(
                                    profile = data.profile,
                                    userEmail = userEmail,
                                    onDismiss = { showTasteSheet = false },
                                    onSavePreferences = { name, username, genres, mood ->
                                        viewModel.updateTastePreferences(name, username, genres, mood)
                                    },
                                    onSignOutClick = {
                                        signOutUser(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
