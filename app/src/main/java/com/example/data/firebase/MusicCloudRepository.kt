package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.catalog.MusicCatalog
import com.example.data.model.CollabInvite
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.LikedTrack
import com.example.data.model.PlayHistoryEntry
import com.example.data.model.PublicPlaylist
import com.example.data.model.Track
import com.example.data.model.UserDirectoryEntry
import com.example.data.model.UserPlaylist
import com.example.data.model.UserTasteProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class MusicCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<UserTasteProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        UserTasteProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observePlaylists(userId: String): Flow<List<UserPlaylist>> = flow {
        val path = "users/$userId/playlists"
        emitAll(
            db.collection("users").document(userId).collection("playlists")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserPlaylist::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeCollaborativePlaylists(userId: String): Flow<List<CollaborativePlaylist>> = flow {
        val path = "collaborativePlaylists"
        emitAll(
            db.collection("collaborativePlaylists")
                .whereArrayContains("collaboratorUids", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CollaborativePlaylist::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePublicPlaylists(userId: String): Flow<List<PublicPlaylist>> = flow {
        val path = "publicPlaylists"
        emitAll(
            db.collection("publicPlaylists")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PublicPlaylist::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeLikedTracks(userId: String): Flow<List<LikedTrack>> = flow {
        val path = "users/$userId/likedTracks"
        emitAll(
            db.collection("users").document(userId).collection("likedTracks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LikedTrack::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePlayHistory(userId: String): Flow<List<PlayHistoryEntry>> = flow {
        val path = "users/$userId/playHistory"
        emitAll(
            db.collection("users").document(userId).collection("playHistory")
                .orderBy("playedAt", Query.Direction.DESCENDING)
                .limit(40)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PlayHistoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(userId: String, rawDisplayName: String?): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val safeName = rawDisplayName?.trim()?.takeIf { it.isNotEmpty() }?.take(80) ?: "Listener"
            val defaultHandle = normalizeHandle("${safeName}_${uid.take(4)}")

            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "username" to defaultHandle,
                    "favoriteGenres" to listOf("Synthwave", "Lo-Fi Chill"),
                    "preferredMood" to "Focus",
                    "offlineModeEnabled" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                upsertDirectoryHandle(uid, defaultHandle, safeName)
            } else {
                val existingHandle = snap.getString("username").orEmpty()
                val existingName = snap.getString("displayName").orEmpty().ifBlank { safeName }
                val resolvedHandle = if (existingHandle.length >= 3) existingHandle else defaultHandle
                if (existingHandle.isBlank()) {
                    docRef.update(
                        mapOf(
                            "displayName" to existingName,
                            "username" to resolvedHandle,
                            "favoriteGenres" to (snap.get("favoriteGenres") as? List<*> ?: listOf("Synthwave")),
                            "preferredMood" to (snap.getString("preferredMood") ?: "Focus"),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                upsertDirectoryHandle(uid, resolvedHandle, existingName)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun upsertDirectoryHandle(userId: String, username: String, displayName: String) {
        val normalized = normalizeHandle(username)
        val payload = mapOf(
            "userId" to userId,
            "username" to normalized,
            "displayName" to displayName.trim().ifBlank { "Listener" }.take(80),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("userDirectory").document(userId).set(payload).await()
    }

    suspend fun updateTastePreferences(
        userId: String,
        displayName: String,
        username: String,
        favoriteGenres: List<String>,
        preferredMood: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val safeName = displayName.trim().ifBlank { "Listener" }.take(80)
            val safeHandle = normalizeHandle(username.ifBlank { "${safeName}_${uid.take(4)}" })
            val safeGenres = favoriteGenres.distinct().take(10)
            val safeMood = preferredMood.trim().ifBlank { "Focus" }.take(40)
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "displayName" to safeName,
                        "username" to safeHandle,
                        "favoriteGenres" to safeGenres,
                        "preferredMood" to safeMood,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            upsertDirectoryHandle(uid, safeHandle, safeName)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setOfflineMode(userId: String, enabled: Boolean): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "offlineModeEnabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(
        userId: String,
        title: String,
        description: String,
        moodTag: String,
        coverKey: String,
        trackIds: List<String>,
        personalized: Boolean
    ): Result<String> {
        var docPath = "users/$userId/playlists"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanTitle = title.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 80) {
                "Playlist title must be 1 to 80 characters."
            }
            val cleanDesc = description.trim().take(240)
            val cleanMood = moodTag.trim().ifBlank { "Personal Mix" }.take(40)
            val safeCover = if (coverKey in MusicCatalog.coverKeys) coverKey else "midnight_synth"
            val safeTracks = trackIds.distinct().take(50)

            val docRef = db.collection("users").document(uid).collection("playlists").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "moodTag" to cleanMood,
                "coverKey" to safeCover,
                "trackIds" to safeTracks,
                "personalized" to personalized,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistDetails(
        userId: String,
        playlistId: String,
        title: String,
        description: String,
        moodTag: String,
        coverKey: String
    ): Result<Unit> {
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(playlistId.isNotBlank()) { "Playlist ID cannot be blank." }
            val cleanTitle = title.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 80) {
                "Playlist title must be 1 to 80 characters."
            }
            val safeCover = if (coverKey in MusicCatalog.coverKeys) coverKey else "midnight_synth"
            db.document(docPath)
                .update(
                    mapOf(
                        "title" to cleanTitle,
                        "description" to description.trim().take(240),
                        "moodTag" to moodTag.trim().ifBlank { "Personal Mix" }.take(40),
                        "coverKey" to safeCover,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            val pubRef = db.collection("publicPlaylists").document(publicDocId(uid, playlistId))
            runCatching {
                if (pubRef.get().await().exists()) {
                    pubRef.update(
                        mapOf(
                            "title" to cleanTitle,
                            "description" to description.trim().take(240),
                            "moodTag" to moodTag.trim().ifBlank { "Personal Mix" }.take(40),
                            "coverKey" to safeCover,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistTracks(
        userId: String,
        playlistId: String,
        trackIds: List<String>
    ): Result<Unit> {
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(playlistId.isNotBlank()) { "Playlist ID cannot be blank." }
            val safeTracks = trackIds.distinct().take(50)
            db.document(docPath)
                .update(
                    mapOf(
                        "trackIds" to safeTracks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            val pubRef = db.collection("publicPlaylists").document(publicDocId(uid, playlistId))
            runCatching {
                if (pubRef.get().await().exists()) {
                    pubRef.update(
                        mapOf(
                            "trackIds" to safeTracks,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(userId: String, playlistId: String): Result<Unit> {
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(playlistId.isNotBlank()) { "Playlist ID cannot be blank." }
            db.document(docPath).delete().await()
            runCatching {
                val pubRef = db.collection("publicPlaylists").document(publicDocId(uid, playlistId))
                if (pubRef.get().await().exists()) {
                    pubRef.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // region Public Online Playlists

    suspend fun publishPlaylistOnline(
        userId: String,
        ownerDisplayName: String,
        ownerUsername: String,
        playlist: UserPlaylist
    ): Result<String> {
        val docId = publicDocId(userId, playlist.id)
        val docPath = "publicPlaylists/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(playlist.id.isNotBlank()) { "Playlist ID cannot be blank." }
            val cleanTitle = playlist.title.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 80) {
                "Playlist title must be 1 to 80 characters."
            }
            val safeOwnerName = ownerDisplayName.trim().ifBlank { "Listener" }.take(80)
            val safeUsername = normalizeHandle(ownerUsername.ifBlank { safeOwnerName }).take(40)
            val safeDesc = playlist.description.trim().take(240)
            val safeMood = playlist.moodTag.trim().ifBlank { "Public Mix" }.take(40)
            val safeCover = if (playlist.coverKey in MusicCatalog.coverKeys) playlist.coverKey else "midnight_synth"
            val safeTracks = playlist.trackIds.distinct().take(50)

            val docRef = db.collection("publicPlaylists").document(docId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "ownerName" to safeOwnerName,
                        "ownerUsername" to safeUsername,
                        "title" to cleanTitle,
                        "description" to safeDesc,
                        "moodTag" to safeMood,
                        "coverKey" to safeCover,
                        "trackIds" to safeTracks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "ownerId" to uid,
                    "ownerName" to safeOwnerName,
                    "ownerUsername" to safeUsername,
                    "sourcePlaylistId" to playlist.id,
                    "title" to cleanTitle,
                    "description" to safeDesc,
                    "moodTag" to safeMood,
                    "coverKey" to safeCover,
                    "trackIds" to safeTracks,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unpublishPlaylistOnline(userId: String, sourcePlaylistId: String): Result<Unit> {
        val docId = publicDocId(userId, sourcePlaylistId)
        val docPath = "publicPlaylists/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            db.collection("publicPlaylists").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // endregion

    // region Collaborative Playlists

    suspend fun createCollaborativePlaylist(
        userId: String,
        ownerDisplayName: String,
        title: String,
        description: String,
        moodTag: String,
        coverKey: String,
        initialTrackIds: List<String>
    ): Result<CollaborativePlaylist> {
        var docPath = "collaborativePlaylists"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanTitle = title.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 80) {
                "Playlist title must be 1 to 80 characters."
            }
            val cleanOwnerName = ownerDisplayName.trim().ifBlank { "Listener" }.take(80)
            val cleanDesc = description.trim().take(240)
            val cleanMood = moodTag.trim().ifBlank { "Shared Mix" }.take(40)
            val safeCover = if (coverKey in MusicCatalog.coverKeys) coverKey else "sonic_horizon"
            val safeTracks = initialTrackIds.distinct().take(50)
            val inviteCode = generateInviteCode()

            val playlistRef = db.collection("collaborativePlaylists").document()
            docPath = playlistRef.path
            val inviteRef = db.collection("collabInvites").document(inviteCode)

            val playlistPayload = mapOf(
                "ownerId" to uid,
                "ownerName" to cleanOwnerName,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "moodTag" to cleanMood,
                "coverKey" to safeCover,
                "trackIds" to safeTracks,
                "collaboratorUids" to listOf(uid),
                "invitedHandles" to emptyList<String>(),
                "inviteCode" to inviteCode,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val invitePayload = mapOf(
                "inviteCode" to inviteCode,
                "playlistId" to playlistRef.id,
                "title" to cleanTitle,
                "ownerId" to uid,
                "ownerName" to cleanOwnerName,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.runBatch { batch ->
                batch.set(playlistRef, playlistPayload)
                batch.set(inviteRef, invitePayload)
            }.await()

            Result.success(
                CollaborativePlaylist(
                    id = playlistRef.id,
                    ownerId = uid,
                    ownerName = cleanOwnerName,
                    title = cleanTitle,
                    description = cleanDesc,
                    moodTag = cleanMood,
                    coverKey = safeCover,
                    trackIds = safeTracks,
                    collaboratorUids = listOf(uid),
                    invitedHandles = emptyList(),
                    inviteCode = inviteCode
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun inviteFriendByUsername(
        playlist: CollaborativePlaylist,
        rawHandle: String
    ): Result<String> {
        val docPath = "collaborativePlaylists/${playlist.id}"
        return try {
            val uid = requireUserId()
            check(playlist.ownerId == uid) { "Only the playlist owner can invite friends directly." }
            val normalized = rawHandle.trim().removePrefix("@").lowercase()
            require(normalized.matches(Regex("^[a-z0-9_]{3,32}$"))) {
                "Enter a valid handle (3-32 lowercase letters, numbers, or underscores)."
            }

            val querySnap = db.collection("userDirectory")
                .whereEqualTo("username", normalized)
                .limit(1)
                .get()
                .await()

            val entry = querySnap.documents.firstOrNull()?.toObject(
                UserDirectoryEntry::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(
                IllegalArgumentException("No Sonora listener found with handle @$normalized. Share your invite link instead!")
            )

            if (entry.userId in playlist.collaboratorUids) {
                return Result.failure(IllegalArgumentException("@$normalized is already a collaborator."))
            }

            val updatedUids = (playlist.collaboratorUids + entry.userId).distinct().take(20)
            val updatedHandles = (playlist.invitedHandles + entry.username).distinct().take(20)

            db.collection("collaborativePlaylists").document(playlist.id)
                .update(
                    mapOf(
                        "collaboratorUids" to updatedUids,
                        "invitedHandles" to updatedHandles,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(entry.displayName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinCollaborativePlaylistByCode(
        userId: String,
        rawCodeOrLink: String
    ): Result<String> {
        val inviteCode = extractInviteCode(rawCodeOrLink)
        if (inviteCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a valid invite code or link."))
        }
        var docPath = "collabInvites/$inviteCode"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val inviteSnap = db.collection("collabInvites").document(inviteCode).get().await()
            val invite = inviteSnap.toObject(
                CollabInvite::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalArgumentException("Invite code \"$inviteCode\" was not found."))

            docPath = "collaborativePlaylists/${invite.playlistId}"
            val playlistRef = db.collection("collaborativePlaylists").document(invite.playlistId)

            val alreadyMember = runCatching { playlistRef.get().await().exists() }.getOrDefault(false)
            if (!alreadyMember) {
                playlistRef.update(
                    mapOf(
                        "collaboratorUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(invite.playlistId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCollaborativeTracks(
        playlistId: String,
        trackIds: List<String>
    ): Result<Unit> {
        val docPath = "collaborativePlaylists/$playlistId"
        return try {
            requireUserId()
            require(playlistId.isNotBlank()) { "Playlist ID cannot be blank." }
            val safeTracks = trackIds.distinct().take(50)
            db.document(docPath)
                .update(
                    mapOf(
                        "trackIds" to safeTracks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun leaveCollaborativePlaylist(userId: String, playlistId: String): Result<Unit> {
        val docPath = "collaborativePlaylists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            db.document(docPath)
                .update(
                    mapOf(
                        "collaboratorUids" to FieldValue.arrayRemove(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCollaborativePlaylist(playlist: CollaborativePlaylist): Result<Unit> {
        val docPath = "collaborativePlaylists/${playlist.id}"
        return try {
            val uid = requireUserId()
            check(playlist.ownerId == uid) { "Only the owner can delete a collaborative playlist." }
            db.runBatch { batch ->
                batch.delete(db.collection("collaborativePlaylists").document(playlist.id))
                if (playlist.inviteCode.isNotBlank()) {
                    batch.delete(db.collection("collabInvites").document(playlist.inviteCode))
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // endregion

    suspend fun toggleLikeTrack(
        userId: String,
        track: Track,
        currentlyLiked: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/likedTracks/${track.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
                .collection("likedTracks").document(track.id)
            if (currentlyLiked) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "trackId" to track.id,
                    "genre" to track.genre.take(40),
                    "likedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (currentlyLiked) OperationType.DELETE else OperationType.CREATE,
                docPath
            )
            Result.failure(e)
        }
    }

    suspend fun recordPlay(userId: String, track: Track): Result<Unit> {
        var docPath = "users/$userId/playHistory"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
                .collection("playHistory").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "trackId" to track.id,
                "genre" to track.genre.take(40),
                "mood" to track.mood.take(40),
                "playedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        userId: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    companion object {
        private val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray()
        private val RNG = SecureRandom()

        fun publicDocId(userId: String, sourcePlaylistId: String): String {
            return "${userId}_${sourcePlaylistId}".take(128)
        }

        fun generateInviteCode(): String {
            val suffix = (1..6).map { CODE_CHARS[RNG.nextInt(CODE_CHARS.size)] }.joinToString("")
            return "SONORA-$suffix"
        }

        fun normalizeHandle(raw: String): String {
            val cleaned = raw.trim().removePrefix("@").lowercase()
                .replace(Regex("[^a-z0-9_]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
            return when {
                cleaned.length >= 3 -> cleaned.take(32)
                else -> (cleaned + "_listener").take(32)
            }
        }

        fun extractInviteCode(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            val afterSlash = trimmed.substringAfterLast("/").substringBefore("?").trim()
            return afterSlash.uppercase().replace(Regex("[^A-Z0-9\\-_]"), "")
        }
    }
}
