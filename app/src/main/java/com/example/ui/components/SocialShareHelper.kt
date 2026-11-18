package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AlbumInfo
import com.example.data.model.Track
import com.example.ui.theme.SonoraAmber
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

sealed interface SocialShareTarget {
    data class TrackTarget(val track: Track) : SocialShareTarget
    data class AlbumTarget(val album: AlbumInfo) : SocialShareTarget
    data class PlaylistTarget(
        val playlistId: String,
        val title: String,
        val trackCount: Int,
        val collabInviteCode: String? = null,
        val publicPlaylistId: String? = null
    ) : SocialShareTarget
}

enum class SocialPlatform(
    val label: String,
    val badge: String,
    val packageName: String,
    val accent: Color
) {
    TWITTER("Twitter / X", "X", "com.twitter.android", Color(0xFF1DA1F2)),
    FACEBOOK("Facebook", "f", "com.facebook.katana", Color(0xFF1877F2)),
    INSTAGRAM("Instagram", "IG", "com.instagram.android", Color(0xFFE1306C))
}

fun buildSharePayload(target: SocialShareTarget): Pair<String, String> {
    return when (target) {
        is SocialShareTarget.TrackTarget -> {
            val url = "https://sonora.app/track/${target.track.id}"
            val text = "Listening to \"${target.track.title}\" by ${target.track.artist} (${target.track.genre}) on Sonora $url"
            url to text
        }

        is SocialShareTarget.AlbumTarget -> {
            val slug = target.album.title.lowercase().replace(" ", "-")
            val url = "https://sonora.app/album/$slug"
            val text = "Check out the album \"${target.album.title}\" by ${target.album.artist} (${target.album.tracks.size} tracks) on Sonora $url"
            url to text
        }

        is SocialShareTarget.PlaylistTarget -> {
            val code = target.collabInviteCode
            val publicId = target.publicPlaylistId
            when {
                !code.isNullOrBlank() -> {
                    val url = "https://sonora.app/collab/$code"
                    val text = "Join my collaborative playlist \"${target.title}\" (${target.trackCount} tracks) on Sonora!\nInvite Link: $url\nDeep Link: sonora://collab/$code\nInvite Code: $code"
                    url to text
                }
                !publicId.isNullOrBlank() -> {
                    val url = "https://sonora.app/public/$publicId"
                    val text = "Check out my public Sonora playlist \"${target.title}\" (${target.trackCount} tracks) — open for everyone to listen!\nListen Link: $url\nApp Deep Link: sonora://public/$publicId"
                    url to text
                }
                else -> {
                    val url = "https://sonora.app/playlist/${target.playlistId}"
                    val text = "Listen to my Sonora playlist \"${target.title}\" (${target.trackCount} tracks) $url"
                    url to text
                }
            }
        }
    }
}

fun shareToPlatform(
    context: Context,
    platform: SocialPlatform,
    shareUrl: String,
    shareText: String
) {
    val hasApp = runCatching {
        context.packageManager.getPackageInfo(platform.packageName, PackageManager.PackageInfoFlags.of(0))
    }.isSuccess || runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(platform.packageName, 0)
    }.isSuccess

    if (hasApp) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            setPackage(platform.packageName)
        }
        runCatching { context.startActivity(sendIntent) }
            .onSuccess { return }
    }

    val webUri = when (platform) {
        SocialPlatform.TWITTER -> Uri.parse(
            "https://twitter.com/intent/tweet?text=${Uri.encode(shareText)}"
        )
        SocialPlatform.FACEBOOK -> Uri.parse(
            "https://www.facebook.com/sharer/sharer.php?u=${Uri.encode(shareUrl)}&quote=${Uri.encode(shareText)}"
        )
        SocialPlatform.INSTAGRAM -> null
    }

    if (webUri != null) {
        val browserIntent = Intent(Intent.ACTION_VIEW, webUri)
        if (browserIntent.resolveActivity(context.packageManager) != null) {
            runCatching { context.startActivity(browserIntent) }
                .onSuccess { return }
        }
    }

    shareViaSystemSheet(context, shareText)
}

fun shareViaSystemSheet(context: Context, shareText: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(sendIntent, "Share via Sonora")
    context.startActivity(chooser)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialShareSheet(
    target: SocialShareTarget,
    onDismiss: () -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val (shareUrl, shareText) = buildSharePayload(target)

    val titleLabel = when (target) {
        is SocialShareTarget.TrackTarget -> target.track.title
        is SocialShareTarget.AlbumTarget -> target.album.title
        is SocialShareTarget.PlaylistTarget -> target.title
    }
    val subtitleLabel = when (target) {
        is SocialShareTarget.TrackTarget -> "Song • ${target.track.artist}"
        is SocialShareTarget.AlbumTarget -> "Album • ${target.album.artist}"
        is SocialShareTarget.PlaylistTarget -> when {
            !target.collabInviteCode.isNullOrBlank() ->
                "Collaborative Invite • Code ${target.collabInviteCode}"
            !target.publicPlaylistId.isNullOrBlank() ->
                "Public Online Playlist • ${target.trackCount} tracks"
            else ->
                "Playlist • ${target.trackCount} tracks"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Share to Social Media",
                style = MaterialTheme.typography.titleLarge,
                color = SonoraTextPrimary
            )

            Surface(
                color = SonoraSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SonoraOutline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = titleLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = SonoraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = shareUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SocialPlatform.entries.forEach { platform ->
                    Surface(
                        color = platform.accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, platform.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                shareToPlatform(context, platform, shareUrl, shareText)
                                onDismiss()
                            }
                            .testTag("share_platform_${platform.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(platform.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = platform.badge,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = platform.label.substringBefore(" "),
                                style = MaterialTheme.typography.labelMedium,
                                color = SonoraTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = SonoraSurfaceElevated,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, SonoraOutline, RoundedCornerShape(50))
                        .clickable {
                            clipboard.setText(AnnotatedString(shareUrl))
                            onCopiedNotice("Copied link: $shareUrl")
                            onDismiss()
                        }
                        .testTag("share_copy_link_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy link",
                            tint = SonoraTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Link",
                            style = MaterialTheme.typography.labelLarge,
                            color = SonoraTextPrimary
                        )
                    }
                }

                Surface(
                    color = SonoraEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, SonoraEmerald.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .clickable {
                            shareViaSystemSheet(context, shareText)
                            onDismiss()
                        }
                        .testTag("share_more_apps_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "More share targets",
                            tint = SonoraEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "More Apps",
                            style = MaterialTheme.typography.labelLarge,
                            color = SonoraEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
