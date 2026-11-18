package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.MusicCatalog
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.data.model.UserTasteProfile
import com.example.ui.theme.SonoraBg
import com.example.ui.theme.SonoraCoral
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.ui.theme.SonoraViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditPlaylistDialog(
    existingPlaylist: UserPlaylist? = null,
    isCollaborativeCreation: Boolean = false,
    showPublishOnlineToggle: Boolean = false,
    initialPublishOnline: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, moodTag: String, coverKey: String, publishOnline: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existingPlaylist?.title ?: "") }
    var description by remember { mutableStateOf(existingPlaylist?.description ?: "") }
    var moodTag by remember {
        mutableStateOf(
            existingPlaylist?.moodTag ?: if (isCollaborativeCreation) "Energetic" else "Focus"
        )
    }
    var coverKey by remember {
        mutableStateOf(
            existingPlaylist?.coverKey ?: if (isCollaborativeCreation) "sonic_horizon" else "midnight_synth"
        )
    }
    var publishOnline by remember { mutableStateOf(initialPublishOnline) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurface,
        title = {
            Text(
                text = when {
                    existingPlaylist != null -> "Edit Playlist"
                    isCollaborativeCreation -> "Create Collaborative Playlist"
                    else -> "Create Playlist"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = SonoraTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = {
                        Text(if (isCollaborativeCreation) "Shared Playlist Name" else "Playlist Name")
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SonoraEmerald,
                        unfocusedBorderColor = SonoraOutline,
                        focusedTextColor = SonoraTextPrimary,
                        unfocusedTextColor = SonoraTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 240) description = it },
                    label = { Text("Vibe / Description") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SonoraEmerald,
                        unfocusedBorderColor = SonoraOutline,
                        focusedTextColor = SonoraTextPrimary,
                        unfocusedTextColor = SonoraTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_desc_input")
                )

                Text(
                    text = "Mood Tag",
                    style = MaterialTheme.typography.labelLarge,
                    color = SonoraTextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MusicCatalog.allMoods.forEach { mood ->
                        val selected = mood == moodTag
                        FilterChip(
                            selected = selected,
                            onClick = { moodTag = mood },
                            label = { Text(mood) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SonoraEmerald.copy(alpha = 0.22f),
                                selectedLabelColor = SonoraEmerald
                            )
                        )
                    }
                }

                Text(
                    text = "Cover Artwork",
                    style = MaterialTheme.typography.labelLarge,
                    color = SonoraTextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MusicCatalog.coverKeys.forEach { key ->
                        val selected = key == coverKey
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) SonoraEmerald else SonoraOutline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { coverKey = key }
                        ) {
                            PlaylistCoverArt(
                                coverKey = key,
                                contentDescription = key,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }

                if (showPublishOnlineToggle) {
                    Surface(
                        color = if (publishOnline) SonoraEmerald.copy(alpha = 0.14f) else SonoraSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (publishOnline) SonoraEmerald.copy(alpha = 0.5f) else SonoraOutline,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { publishOnline = !publishOnline }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = "Publish online",
                                tint = if (publishOnline) SonoraEmerald else SonoraTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Publish Online",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SonoraTextPrimary
                                )
                                Text(
                                    text = "Share with everyone on Sonora",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary
                                )
                            }
                            Switch(
                                checked = publishOnline,
                                onCheckedChange = { publishOnline = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SonoraBg,
                                    checkedTrackColor = SonoraEmerald
                                ),
                                modifier = Modifier.testTag("dialog_publish_online_switch")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), description.trim(), moodTag, coverKey, publishOnline)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SonoraEmerald,
                    contentColor = SonoraBg
                ),
                modifier = Modifier.testTag("confirm_playlist_button")
            ) {
                Text(if (existingPlaylist == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SonoraTextSecondary)
            }
        }
    )
}

@Composable
fun JoinCollaborativePlaylistDialog(
    initialCode: String = "",
    onDismiss: () -> Unit,
    onConfirm: (codeOrLink: String) -> Unit
) {
    var codeOrLink by remember(initialCode) { mutableStateOf(initialCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurface,
        title = {
            Text(
                text = "Join Collaborative Playlist",
                style = MaterialTheme.typography.headlineSmall,
                color = SonoraTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste a Sonora invite link (https://sonora.app/collab/... or sonora://collab/...) or enter the 6-character invite code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SonoraTextSecondary
                )
                OutlinedTextField(
                    value = codeOrLink,
                    onValueChange = { codeOrLink = it },
                    label = { Text("Invite Link or Code (e.g. SONORA-ABC123)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = "Invite link",
                            tint = SonoraViolet
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SonoraViolet,
                        unfocusedBorderColor = SonoraOutline,
                        focusedTextColor = SonoraTextPrimary,
                        unfocusedTextColor = SonoraTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_collab_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (codeOrLink.isNotBlank()) {
                        onConfirm(codeOrLink.trim())
                    }
                },
                enabled = codeOrLink.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SonoraViolet,
                    contentColor = SonoraTextPrimary
                ),
                modifier = Modifier.testTag("confirm_join_collab_btn")
            ) {
                Text("Join Playlist", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SonoraTextSecondary)
            }
        }
    )
}

@Composable
fun InviteFriendDialog(
    playlist: CollaborativePlaylist,
    onDismiss: () -> Unit,
    onInviteByUsername: (username: String) -> Unit,
    onOpenShareLink: () -> Unit
) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurface,
        title = {
            Text(
                text = "Invite Collaborators",
                style = MaterialTheme.typography.headlineSmall,
                color = SonoraTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Share code ${playlist.inviteCode} or invite a friend directly by their Sonora @username.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SonoraTextSecondary
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { if (it.length <= 33) username = it },
                    label = { Text("Friend's @username") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AlternateEmail,
                            contentDescription = "Username",
                            tint = SonoraEmerald
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SonoraEmerald,
                        unfocusedBorderColor = SonoraOutline,
                        focusedTextColor = SonoraTextPrimary,
                        unfocusedTextColor = SonoraTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_username_input")
                )

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenShareLink()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_share_link_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = "Share invite link",
                        tint = SonoraTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Invite Link (${playlist.inviteCode})", color = SonoraTeal)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        onInviteByUsername(username.trim())
                    }
                },
                enabled = username.trim().removePrefix("@").length >= 3,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SonoraEmerald,
                    contentColor = SonoraBg
                ),
                modifier = Modifier.testTag("confirm_invite_username_btn")
            ) {
                Text("Invite Friend", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SonoraTextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrackFromCatalogSheet(
    playlistTitle: String,
    existingTrackIds: Set<String>,
    onDismiss: () -> Unit,
    onSelectTrack: (Track) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Add Songs to \"$playlistTitle\"",
                style = MaterialTheme.typography.titleLarge,
                color = SonoraTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MusicCatalog.tracks, key = { it.id }) { track ->
                    val added = track.id in existingTrackIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SonoraSurfaceElevated)
                            .clickable(enabled = !added) { onSelectTrack(track) }
                            .padding(12.dp)
                            .testTag("collab_pick_track_${track.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistCoverArt(
                            coverKey = track.coverKey,
                            contentDescription = track.title,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = SonoraTextPrimary
                            )
                            Text(
                                text = "${track.artist} • ${track.genre}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }
                        Icon(
                            imageVector = if (added) Icons.Rounded.Check else Icons.Rounded.Add,
                            contentDescription = if (added) "Already added" else "Add track",
                            tint = if (added) SonoraTeal else SonoraEmerald
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track,
    playlists: List<UserPlaylist>,
    collaborativePlaylists: List<CollaborativePlaylist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (UserPlaylist) -> Unit,
    onSelectCollabPlaylist: (CollaborativePlaylist) -> Unit,
    onCreateNewPlaylistWithTrack: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Add \"${track.title}\" to Playlist",
                style = MaterialTheme.typography.titleLarge,
                color = SonoraTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = SonoraEmerald.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateNewPlaylistWithTrack)
                    .testTag("sheet_create_playlist_btn")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New playlist",
                        tint = SonoraEmerald
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Create New Playlist with This Track",
                        style = MaterialTheme.typography.titleSmall,
                        color = SonoraEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (playlists.isEmpty() && collaborativePlaylists.isEmpty()) {
                Text(
                    text = "You have no custom playlists yet. Create one above to sync across devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SonoraTextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(collaborativePlaylists, key = { "collab_${it.id}" }) { collab ->
                        val alreadyIn = track.id in collab.trackIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SonoraSurfaceElevated)
                                .clickable { onSelectCollabPlaylist(collab) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistCoverArt(
                                coverKey = collab.coverKey,
                                contentDescription = collab.title,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.GroupAdd,
                                        contentDescription = "Collaborative",
                                        tint = SonoraViolet,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = collab.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SonoraTextPrimary
                                    )
                                }
                                Text(
                                    text = "${collab.trackIds.size} tracks • Collaborative (${collab.collaboratorUids.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary
                                )
                            }
                            if (alreadyIn) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Already in playlist",
                                    tint = SonoraEmerald
                                )
                            }
                        }
                    }

                    items(playlists, key = { it.id }) { pl ->
                        val alreadyIn = track.id in pl.trackIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SonoraSurfaceElevated)
                                .clickable { onSelectPlaylist(pl) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistCoverArt(
                                coverKey = pl.coverKey,
                                contentDescription = pl.title,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pl.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SonoraTextPrimary
                                )
                                Text(
                                    text = "${pl.trackIds.size} tracks • ${pl.moodTag}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary
                                )
                            }
                            if (alreadyIn) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Already in playlist",
                                    tint = SonoraEmerald
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TastePreferencesSheet(
    profile: UserTasteProfile?,
    userEmail: String?,
    onDismiss: () -> Unit,
    onSavePreferences: (displayName: String, username: String, genres: List<String>, mood: String) -> Unit,
    onSignOutClick: () -> Unit
) {
    var displayName by remember { mutableStateOf(profile?.displayName ?: "Listener") }
    var username by remember { mutableStateOf(profile?.username ?: "") }
    var selectedGenres by remember {
        mutableStateOf(profile?.favoriteGenres?.toSet() ?: setOf("Synthwave", "Lo-Fi Chill"))
    }
    var selectedMood by remember { mutableStateOf(profile?.preferredMood ?: "Focus") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SonoraEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Taste profile",
                            tint = SonoraEmerald
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Profile & Taste Engine",
                            style = MaterialTheme.typography.titleLarge,
                            color = SonoraTextPrimary
                        )
                        if (!userEmail.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = "Cloud synced",
                                    tint = SonoraTeal,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= 80) displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SonoraEmerald,
                    unfocusedBorderColor = SonoraOutline,
                    focusedTextColor = SonoraTextPrimary,
                    unfocusedTextColor = SonoraTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { if (it.length <= 32) username = it.lowercase() },
                label = { Text("Username Handle (for friend collab invites)") },
                prefix = { Text("@", color = SonoraEmerald) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SonoraEmerald,
                    unfocusedBorderColor = SonoraOutline,
                    focusedTextColor = SonoraTextPrimary,
                    unfocusedTextColor = SonoraTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_username_input")
            )

            Text(
                text = "Favorite Genres (shapes For You engine)",
                style = MaterialTheme.typography.labelLarge,
                color = SonoraTextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MusicCatalog.allGenres.forEach { genre ->
                    val active = genre in selectedGenres
                    FilterChip(
                        selected = active,
                        onClick = {
                            selectedGenres = if (active && selectedGenres.size > 1) {
                                selectedGenres - genre
                            } else {
                                selectedGenres + genre
                            }
                        },
                        label = { Text(genre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonoraEmerald.copy(alpha = 0.22f),
                            selectedLabelColor = SonoraEmerald
                        )
                    )
                }
            }

            Text(
                text = "Default Listening Mood",
                style = MaterialTheme.typography.labelLarge,
                color = SonoraTextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MusicCatalog.allMoods.forEach { mood ->
                    val active = mood == selectedMood
                    FilterChip(
                        selected = active,
                        onClick = { selectedMood = mood },
                        label = { Text(mood) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonoraTeal.copy(alpha = 0.22f),
                            selectedLabelColor = SonoraTeal
                        )
                    )
                }
            }

            Button(
                onClick = {
                    onSavePreferences(
                        displayName.trim().ifBlank { "Listener" },
                        username.trim(),
                        selectedGenres.toList(),
                        selectedMood
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SonoraEmerald,
                    contentColor = SonoraBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_taste_button")
            ) {
                Text("Save Taste Profile", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onSignOutClick()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraCoral),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Sign out",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
