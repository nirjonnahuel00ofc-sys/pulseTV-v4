package com.example.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.model.Channel
import com.example.model.ChannelCatalogDefaults
import com.example.model.PlaybackStatus
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LiveCrimson
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun StreamPlayerSection(
    channel: Channel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClosePlayer: () -> Unit,
    onReportBrokenLink: (Channel, String) -> Unit,
    showFavoriteButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isFullscreen by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isPausedByUser by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showReportDialog by remember { mutableStateOf(false) }

    var usingBackup by remember(channel.id, channel.primaryUrl, channel.backupUrl) {
        mutableStateOf(false)
    }
    var retryAttempt by remember(channel.id, channel.primaryUrl, channel.backupUrl) {
        mutableIntStateOf(0)
    }
    var manualReloadNonce by remember(channel.id) { mutableIntStateOf(0) }
    var playbackStatus by remember(channel.id, channel.primaryUrl, channel.backupUrl) {
        mutableStateOf<PlaybackStatus>(PlaybackStatus.Buffering)
    }

    // Lightweight Media3 LoadControl tuned for fast live-TV start & low memory footprint
    val exoPlayer = remember(context) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 4_000,
                /* maxBufferMs = */ 18_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500,
            )
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    // Manage Activity pause/resume lifecycle cleanly
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (!isPausedByUser) exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Mute sync
    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Auto-hide overlay controls after 3.5s while playing
    LaunchedEffect(showControls, playbackStatus) {
        if (showControls && playbackStatus is PlaybackStatus.Playing && !isPausedByUser) {
            delay(3500L)
            showControls = false
        }
    }

    // Load active stream URL & handle automatic reconnect + backup URL failover
    val activeUrl = if (usingBackup && channel.backupUrl.isNotBlank()) {
        channel.backupUrl
    } else {
        channel.primaryUrl
    }

    LaunchedEffect(activeUrl, retryAttempt, manualReloadNonce) {
        if (activeUrl.isBlank()) {
            playbackStatus = PlaybackStatus.Offline("No valid stream URL configured for this channel.")
            return@LaunchedEffect
        }
        if (retryAttempt > 0) {
            playbackStatus = PlaybackStatus.Reconnecting(retryAttempt, usingBackup)
            delay(1200L)
        } else {
            playbackStatus = PlaybackStatus.Buffering
        }
        val itemBuilder = MediaItem.Builder().setUri(activeUrl)
        if (activeUrl.contains(".m3u8", ignoreCase = true)) {
            itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }
        exoPlayer.setMediaItem(itemBuilder.build())
        exoPlayer.prepare()
        if (!isPausedByUser) {
            exoPlayer.play()
        }
    }

    DisposableEffect(exoPlayer, channel.id, channel.backupUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        if (playbackStatus !is PlaybackStatus.Reconnecting &&
                            playbackStatus !is PlaybackStatus.Offline
                        ) {
                            playbackStatus = PlaybackStatus.Buffering
                        }
                    }
                    Player.STATE_READY -> {
                        retryAttempt = 0
                        playbackStatus = PlaybackStatus.Playing(usingBackup)
                    }
                    Player.STATE_ENDED -> {
                        // Live feeds occasionally close segment windows; attempt reconnect
                        if (retryAttempt < 2) {
                            retryAttempt += 1
                        } else if (!usingBackup && channel.backupUrl.isNotBlank()) {
                            usingBackup = true
                            retryAttempt = 0
                        } else {
                            playbackStatus = PlaybackStatus.Offline("Broadcast stream ended or stalled.")
                        }
                    }
                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (retryAttempt < 2) {
                    retryAttempt += 1
                } else if (!usingBackup && channel.backupUrl.isNotBlank()) {
                    usingBackup = true
                    retryAttempt = 0
                } else {
                    playbackStatus = PlaybackStatus.Offline(
                        error.localizedMessage ?: "Stream unreachable or format unsupported."
                    )
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    val playerSurfaceContent: @Composable (Boolean) -> Unit = { fullscreen ->
        PlayerStageContent(
            exoPlayer = exoPlayer,
            channel = channel,
            playbackStatus = playbackStatus,
            isFavorite = isFavorite,
            showFavoriteButton = showFavoriteButton,
            isMuted = isMuted,
            isPausedByUser = isPausedByUser,
            isFullscreen = fullscreen,
            showControls = showControls,
            onToggleControls = { showControls = !showControls },
            onTogglePlayPause = {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    isPausedByUser = true
                } else {
                    exoPlayer.play()
                    isPausedByUser = false
                }
                showControls = true
            },
            onToggleMute = {
                isMuted = !isMuted
                showControls = true
            },
            onToggleFullscreen = {
                isFullscreen = !isFullscreen
                showControls = true
            },
            onToggleFavorite = onToggleFavorite,
            onRetry = {
                usingBackup = false
                retryAttempt = 0
                manualReloadNonce += 1
            },
            onOpenReport = { showReportDialog = true },
            onClosePlayer = onClosePlayer,
        )
    }

    if (!isFullscreen) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = Color.Black,
                tonalElevation = 4.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    playerSurfaceContent(false)
                }
            }

            if (channel.backupUrl.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Streams:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        onClick = {
                            usingBackup = false
                            retryAttempt = 0
                            manualReloadNonce += 1
                        },
                        color = if (!usingBackup) MaterialTheme.colorScheme.primaryContainer else Color(0xFF171E2E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("server_1_pill"),
                    ) {
                        Text(
                            text = "Server 1 (Primary)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (!usingBackup) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Surface(
                        onClick = {
                            usingBackup = true
                            retryAttempt = 0
                            manualReloadNonce += 1
                        },
                        color = if (usingBackup) AmberGlow.copy(alpha = 0.22f) else Color(0xFF171E2E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("server_2_pill"),
                    ) {
                        Text(
                            text = "Server 2 (Backup)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (usingBackup) AmberGlow else Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    } else {
        FullscreenPlayerDialog(
            onDismiss = { isFullscreen = false },
            content = { playerSurfaceContent(true) },
        )
    }

    if (showReportDialog) {
        ReportBrokenLinkDialog(
            channel = channel,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                showReportDialog = false
                onReportBrokenLink(channel, reason)
            },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerStageContent(
    exoPlayer: ExoPlayer,
    channel: Channel,
    playbackStatus: PlaybackStatus,
    isFavorite: Boolean,
    showFavoriteButton: Boolean,
    isMuted: Boolean,
    isPausedByUser: Boolean,
    isFullscreen: Boolean,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRetry: () -> Unit,
    onOpenReport: () -> Unit,
    onClosePlayer: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleControls,
            )
    ) {
        if (playbackStatus !is PlaybackStatus.Offline) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        keepScreenOn = true
                        player = exoPlayer
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Buffering or Reconnecting indicator
        if (playbackStatus is PlaybackStatus.Buffering || playbackStatus is PlaybackStatus.Reconnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(38.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    val statusText = when (playbackStatus) {
                        is PlaybackStatus.Reconnecting ->
                            if (playbackStatus.usingBackup) {
                                "Switching to backup stream (Retry ${playbackStatus.attempt}/2)..."
                            } else {
                                "Reconnecting stream (Attempt ${playbackStatus.attempt}/2)..."
                            }
                        else -> "Buffering live feed..."
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                    )
                }
            }
        }

        // Clean Stream Offline / Unavailable card with Report Broken Link action
        if (playbackStatus is PlaybackStatus.Offline) {
            StreamOfflineOverlay(
                reason = playbackStatus.reason,
                onRetry = onRetry,
                onReportBrokenLink = onOpenReport,
                onClose = onClosePlayer,
            )
        } else {
            // Controls Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f),
                                )
                            )
                        )
                        .padding(10.dp)
                ) {
                    // Top bar inside player
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = LiveCrimson,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = stringResource(R.string.live_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        if ((playbackStatus as? PlaybackStatus.Playing)?.usingBackup == true) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = AmberGlow.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "BACKUP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberGlow,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                        } else if (playbackStatus is PlaybackStatus.Playing) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = EmeraldLive.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "HD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldLive,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                        IconButton(
                            onClick = onOpenReport,
                            modifier = Modifier.testTag("player_report_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Report broken link",
                                tint = Color.White.copy(alpha = 0.85f),
                            )
                        }

                        if (showFavoriteButton) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.testTag("player_favorite_button"),
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = stringResource(R.string.cd_favorite_toggle),
                                    tint = if (isFavorite) LiveCrimson else Color.White,
                                )
                            }
                        }

                        if (!isFullscreen) {
                            IconButton(
                                onClick = onClosePlayer,
                                modifier = Modifier.testTag("player_close_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close player",
                                    tint = Color.White,
                                )
                            }
                        }
                    }

                    // Bottom bar inside player
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.testTag("player_play_pause_button"),
                        ) {
                            Icon(
                                imageVector = if (isPausedByUser) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = stringResource(R.string.cd_play_pause),
                                tint = Color.White,
                            )
                        }

                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.testTag("player_mute_button"),
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.cd_mute_toggle),
                                tint = Color.White,
                            )
                        }

                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("player_reload_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reconnect stream",
                                tint = Color.White.copy(alpha = 0.9f),
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Text(
                            text = "${channel.category} • ${channel.language}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.width(4.dp))

                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.testTag("player_fullscreen_button"),
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.cd_fullscreen_toggle),
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamOfflineOverlay(
    reason: String,
    onRetry: () -> Unit,
    onReportBrokenLink: () -> Unit,
    onClose: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_stream_offline),
            contentDescription = stringResource(R.string.cd_stream_offline),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.5f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp),
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("offline_close_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close offline card",
                    tint = Color.White,
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.SignalWifiOff,
                    contentDescription = null,
                    tint = LiveCrimson,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Stream Offline / Unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("offline_retry_button"),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry")
                    }
                    Button(
                        onClick = onReportBrokenLink,
                        colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                        modifier = Modifier.testTag("report_broken_link_button"),
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Report Broken Link")
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenPlayerDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = previousOrientation
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler { onDismiss() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            content()
        }
    }
}

@Composable
fun ReportBrokenLinkDialog(
    channel: Channel,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var selectedReason by remember { mutableStateOf(ChannelCatalogDefaults.REPORT_REASONS.first()) }
    var customNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Broken Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Reporting: ${channel.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                ChannelCatalogDefaults.REPORT_REASONS.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                OutlinedTextField(
                    value = customNote,
                    onValueChange = { if (it.length <= 200) customNote = it },
                    label = { Text("Optional note") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_custom_note_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullReason = if (customNote.isBlank()) selectedReason else "$selectedReason: ${customNote.trim()}"
                    onSubmit(fullReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                modifier = Modifier.testTag("submit_broken_report_button"),
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
