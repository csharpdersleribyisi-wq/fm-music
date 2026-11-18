package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.ui.SonoraApp
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.SonoraTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    private val deepLinkCollabCode = mutableStateOf<String?>(null)
    private val deepLinkPublicPlaylistId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        deepLinkCollabCode.value = extractCollabCodeFromIntent(intent)
        deepLinkPublicPlaylistId.value = extractPublicPlaylistIdFromIntent(intent)
        enableEdgeToEdge()
        setContent {
            SonoraTheme {
                val pendingInvite by deepLinkCollabCode
                val pendingPublicId by deepLinkPublicPlaylistId
                AppNavigation(
                    pendingInviteCode = pendingInvite,
                    onConsumePendingInvite = { deepLinkCollabCode.value = null },
                    pendingPublicPlaylistId = pendingPublicId,
                    onConsumePendingPublicPlaylist = { deepLinkPublicPlaylistId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractCollabCodeFromIntent(intent)?.let { code ->
            deepLinkCollabCode.value = code
        }
        extractPublicPlaylistIdFromIntent(intent)?.let { publicId ->
            deepLinkPublicPlaylistId.value = publicId
        }
    }

    private fun extractCollabCodeFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == "sonora" && data.host == "collab") {
            return data.lastPathSegment?.takeIf { it.isNotBlank() }
        }
        if (data.scheme == "https" && data.host == "sonora.app" && data.pathSegments.firstOrNull() == "collab") {
            return data.pathSegments.getOrNull(1)?.takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun extractPublicPlaylistIdFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == "sonora" && data.host == "public") {
            return data.lastPathSegment?.takeIf { it.isNotBlank() }
        }
        if (data.scheme == "https" && data.host == "sonora.app" && data.pathSegments.firstOrNull() == "public") {
            return data.pathSegments.getOrNull(1)?.takeIf { it.isNotBlank() }
        }
        return null
    }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    pendingInviteCode: String? = null,
    onConsumePendingInvite: () -> Unit = {},
    pendingPublicPlaylistId: String? = null,
    onConsumePendingPublicPlaylist: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            SonoraApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName,
                userEmail = user.email,
                pendingInviteCode = pendingInviteCode,
                onConsumePendingInvite = onConsumePendingInvite,
                pendingPublicPlaylistId = pendingPublicPlaylistId,
                onConsumePendingPublicPlaylist = onConsumePendingPublicPlaylist
            )
        }
    }
}
