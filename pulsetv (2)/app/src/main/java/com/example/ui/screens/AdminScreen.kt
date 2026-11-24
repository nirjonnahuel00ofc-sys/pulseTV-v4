package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.model.AppAnalyticsSummary
import com.example.model.BannerNotice
import com.example.model.BrokenReport
import com.example.model.Channel
import com.example.model.LiveEvent
import com.example.model.UiState
import com.example.model.UpdateNotice
import com.example.ui.components.BannerEditorDialog
import com.example.ui.components.ChannelEditorDialog
import com.example.ui.components.ChannelLogoBadge
import com.example.ui.components.LiveEventEditorDialog
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MidnightSurfaceContainer
import com.example.ui.theme.MidnightSurfaceHigh
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    isPinUnlocked: Boolean,
    currentUser: FirebaseUser?,
    allChannelsState: UiState<List<Channel>>,
    allLiveEventsState: UiState<List<LiveEvent>>,
    allBannersState: UiState<List<BannerNotice>>,
    updatesState: UiState<List<UpdateNotice>>,
    brokenReportsState: UiState<List<BrokenReport>>,
    analyticsState: UiState<AppAnalyticsSummary>,
    isImportingPlaylist: Boolean,
    onVerifyPin: (String) -> Boolean,
    onLockAdmin: () -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onCreateChannel: (String, String, String, String, String, String, String, Boolean, (Boolean) -> Unit) -> Unit,
    onUpdateChannel: (String, String, String, String, String, String, String, String, Boolean, (Boolean) -> Unit) -> Unit,
    onToggleChannelActive: (Channel) -> Unit,
    onDeleteChannel: (Channel) -> Unit,
    onImportIndiaPlaylist: () -> Unit,
    onImportM3uUri: (Uri) -> Unit,
    onCreateLiveEvent: (String, String, String, String, String, String, String, Boolean, (Boolean) -> Unit) -> Unit,
    onUpdateLiveEvent: (String, String, String, String, String, String, String, String, Boolean, (Boolean) -> Unit) -> Unit,
    onToggleLiveEventActive: (LiveEvent) -> Unit,
    onDeleteLiveEvent: (LiveEvent) -> Unit,
    onCreateBanner: (String, String, String, String, Boolean, (Boolean) -> Unit) -> Unit,
    onUpdateBanner: (String, String, String, String, String, Boolean, (Boolean) -> Unit) -> Unit,
    onToggleBannerActive: (BannerNotice) -> Unit,
    onDeleteBanner: (BannerNotice) -> Unit,
    onPublishNotice: (String, String, String, (Boolean) -> Unit) -> Unit,
    onDeleteNotice: (String) -> Unit,
    onDeleteReport: (String) -> Unit,
    onClearAllReports: (List<BrokenReport>) -> Unit,
    onSeedPublicChannels: () -> Unit,
    onSeedStarterLiveEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isPinUnlocked) {
        AdminPinGate(
            onVerifyPin = onVerifyPin,
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    // Attempt silent Google sign-in once when PIN is unlocked if not yet signed in
    LaunchedEffect(isPinUnlocked) {
        if (currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(context, credentialManager, scope)
        }
    }

    if (currentUser == null) {
        AdminGoogleSignInGate(
            credentialManager = credentialManager,
            onLockAdmin = onLockAdmin,
            modifier = modifier,
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingChannel by remember { mutableStateOf<Channel?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingLiveEvent by remember { mutableStateOf<LiveEvent?>(null) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<BannerNotice?>(null) }
    var showCreateBannerDialog by remember { mutableStateOf(false) }

    val channels = (allChannelsState as? UiState.Success)?.data.orEmpty()
    val liveEvents = (allLiveEventsState as? UiState.Success)?.data.orEmpty()
    val banners = (allBannersState as? UiState.Success)?.data.orEmpty()
    val notices = (updatesState as? UiState.Success)?.data.orEmpty()
    val reports = (brokenReportsState as? UiState.Success)?.data.orEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // Admin Header Strip
        Surface(
            color = MidnightSurfaceHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = ElectricCyan,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Admin Studio",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = currentUser.email ?: currentUser.uid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onLockAdmin,
                    modifier = Modifier.testTag("admin_lock_button"),
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock Admin")
                }
                IconButton(
                    onClick = {
                        GoogleAuthHelper.signOut(credentialManager, scope)
                    },
                    modifier = Modifier.testTag("admin_sign_out_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out Google")
                }
            }
        }

        val tabs = listOf(
            "Dashboard",
            "Channels (${channels.size})",
            "Events (${liveEvents.size})",
            "Banners (${banners.size})",
            "Notices (${notices.size})",
            "Reports (${reports.size})",
            "Config",
        )
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MidnightSurfaceContainer,
            edgePadding = 12.dp,
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_$idx"),
                )
            }
        }

        when (selectedTab) {
            0 -> AdminDashboardTab(
                analyticsState = analyticsState,
                channels = channels,
                liveEvents = liveEvents,
                banners = banners,
                notices = notices,
                reports = reports,
            )

            1 -> AdminChannelsTab(
                channels = channels,
                isImportingPlaylist = isImportingPlaylist,
                onAddClick = { showCreateDialog = true },
                onSeedClick = onSeedPublicChannels,
                onImportIndiaPlaylist = onImportIndiaPlaylist,
                onImportM3uUri = onImportM3uUri,
                onEditClick = { editingChannel = it },
                onToggleActive = onToggleChannelActive,
                onDeleteClick = onDeleteChannel,
            )

            2 -> AdminLiveEventsTab(
                events = liveEvents,
                onAddClick = { showCreateEventDialog = true },
                onSeedClick = onSeedStarterLiveEvents,
                onEditClick = { editingLiveEvent = it },
                onToggleActive = onToggleLiveEventActive,
                onDeleteClick = onDeleteLiveEvent,
            )

            3 -> AdminBannersTab(
                banners = banners,
                onAddClick = { showCreateBannerDialog = true },
                onEditClick = { editingBanner = it },
                onToggleActive = onToggleBannerActive,
                onDeleteClick = onDeleteBanner,
            )

            4 -> AdminBroadcastsTab(
                notices = notices,
                onPublish = onPublishNotice,
                onDelete = onDeleteNotice,
            )

            5 -> AdminReportsTab(
                reportsState = brokenReportsState,
                channels = channels,
                onFixChannel = { ch -> editingChannel = ch },
                onDeleteReport = onDeleteReport,
                onClearAll = { onClearAllReports(reports) },
            )

            6 -> AdminConfigTab(
                onUpdatePin = onUpdateAdminPin,
            )
        }
    }

    if (showCreateDialog) {
        ChannelEditorDialog(
            initialChannel = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, cat, lang, country, logo, primary, backup, active ->
                onCreateChannel(name, cat, lang, country, logo, primary, backup, active) { ok ->
                    if (ok) showCreateDialog = false
                }
            },
        )
    }

    if (editingChannel != null) {
        val target = editingChannel!!
        ChannelEditorDialog(
            initialChannel = target,
            onDismiss = { editingChannel = null },
            onSave = { name, cat, lang, country, logo, primary, backup, active ->
                onUpdateChannel(target.id, name, cat, lang, country, logo, primary, backup, active) { ok ->
                    if (ok) editingChannel = null
                }
            },
        )
    }

    if (showCreateEventDialog) {
        LiveEventEditorDialog(
            initialEvent = null,
            onDismiss = { showCreateEventDialog = false },
            onSave = { title, sport, teams, status, thumb, stream, backup, active ->
                onCreateLiveEvent(title, sport, teams, status, thumb, stream, backup, active) { ok ->
                    if (ok) showCreateEventDialog = false
                }
            },
        )
    }

    if (editingLiveEvent != null) {
        val target = editingLiveEvent!!
        LiveEventEditorDialog(
            initialEvent = target,
            onDismiss = { editingLiveEvent = null },
            onSave = { title, sport, teams, status, thumb, stream, backup, active ->
                onUpdateLiveEvent(target.id, title, sport, teams, status, thumb, stream, backup, active) { ok ->
                    if (ok) editingLiveEvent = null
                }
            },
        )
    }

    if (showCreateBannerDialog) {
        BannerEditorDialog(
            initialBanner = null,
            onDismiss = { showCreateBannerDialog = false },
            onSave = { title, message, imageData, actionUrl, active ->
                onCreateBanner(title, message, imageData, actionUrl, active) { ok ->
                    if (ok) showCreateBannerDialog = false
                }
            },
        )
    }

    if (editingBanner != null) {
        val target = editingBanner!!
        BannerEditorDialog(
            initialBanner = target,
            onDismiss = { editingBanner = null },
            onSave = { title, message, imageData, actionUrl, active ->
                onUpdateBanner(target.id, title, message, imageData, actionUrl, active) { ok ->
                    if (ok) editingBanner = null
                }
            },
        )
    }
}

@Composable
private fun AdminPinGate(
    onVerifyPin: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MidnightSurfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = "Protected Admin Studio",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Enter your Admin PIN to manage live channels, live event streams, Update Inbox broadcasts, and broken-link reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.length <= 16 && input.all { it.isDigit() }) {
                            pin = input
                            errorMsg = null
                        }
                    },
                    label = { Text("Admin PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = errorMsg != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input"),
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = LiveCrimson,
                    )
                }
                Button(
                    onClick = {
                        if (!onVerifyPin(pin)) {
                            errorMsg = "Incorrect PIN. Try again."
                        }
                    },
                    enabled = pin.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_unlock_button"),
                ) {
                    Text("Unlock Admin")
                }
            }
        }
    }
}

@Composable
private fun AdminGoogleSignInGate(
    credentialManager: CredentialManager,
    onLockAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MidnightSurfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(44.dp),
                )
                Text(
                    text = "Verify Admin Identity",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Guest viewers browse freely without signing in. Admin writes to Cloud Firestore require Google Sign-In verified against firestore.rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (authError != null) {
                    Text(
                        text = authError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = LiveCrimson,
                    )
                }
                Button(
                    onClick = {
                        isLoading = true
                        authError = null
                        GoogleAuthHelper.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = { isLoading = false },
                            onAuthError = { msg ->
                                isLoading = false
                                authError = msg
                            },
                            onAuthCancelled = { isLoading = false },
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_google_signin_button"),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Sign in with Google")
                    }
                }
                TextButton(onClick = onLockAdmin) {
                    Text("Lock & Return to Guest Mode")
                }
            }
        }
    }
}

@Composable
private fun AdminChannelsTab(
    channels: List<Channel>,
    isImportingPlaylist: Boolean,
    onAddClick: () -> Unit,
    onSeedClick: () -> Unit,
    onImportIndiaPlaylist: () -> Unit,
    onImportM3uUri: (Uri) -> Unit,
    onEditClick: (Channel) -> Unit,
    onToggleActive: (Channel) -> Unit,
    onDeleteClick: (Channel) -> Unit,
) {
    var filterQuery by remember { mutableStateOf("") }
    val m3uFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportM3uUri(uri)
        }
    }

    val filteredChannels = remember(channels, filterQuery) {
        val q = filterQuery.trim().lowercase()
        if (q.isEmpty()) channels
        else channels.filter { ch ->
            ch.name.lowercase().contains(q) ||
                ch.category.lowercase().contains(q) ||
                ch.language.lowercase().contains(q) ||
                ch.country.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "channel_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_add_channel_button"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Channel")
                }
                OutlinedButton(
                    onClick = onSeedClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_seed_streams_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Load Public HLS")
                }
            }
        }

        item(key = "bulk_m3u_import_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), MaterialTheme.shapes.medium),
                shape = MaterialTheme.shapes.medium,
                color = MidnightSurfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "M3U Playlist Bulk Sync",
                        style = MaterialTheme.typography.titleSmall,
                        color = ElectricCyan,
                    )
                    Text(
                        text = "One-tap sync all 808 attached India IPTV channels or upload any .m3u / .txt playlist file to Cloud Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onImportIndiaPlaylist,
                            enabled = !isImportingPlaylist,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_import_india_m3u_button"),
                        ) {
                            if (isImportingPlaylist) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(if (isImportingPlaylist) "Syncing..." else "India IPTV (808)")
                        }
                        OutlinedButton(
                            onClick = {
                                m3uFileLauncher.launch(
                                    arrayOf("audio/x-mpegurl", "application/x-mpegurl", "text/*", "*/*")
                                )
                            },
                            enabled = !isImportingPlaylist,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_import_m3u_file_button"),
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pick .M3U File")
                        }
                    }
                }
            }
        }

        if (channels.isNotEmpty()) {
            item(key = "admin_channel_search") {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Search ${channels.size} channels...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filterQuery.isNotEmpty()) {
                            IconButton(onClick = { filterQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_channels_search_input"),
                )
            }
        }

        if (filteredChannels.isEmpty()) {
            item(key = "admin_channels_empty") {
                EmptyOrErrorStateCard(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    title = if (channels.isEmpty()) "No Channels in Firestore" else "No Matching Channels",
                    message = if (channels.isEmpty()) {
                        "Tap India IPTV (808) above to sync your playlist, pick an .m3u file, or add a single channel."
                    } else {
                        "No channels match \"$filterQuery\"."
                    },
                )
            }
        } else {
            items(filteredChannels, key = { it.id }) { channel ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.medium,
                    color = MidnightSurfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChannelLogoBadge(channelName = channel.name, logoUrl = channel.logo, size = 44.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${channel.category} • ${channel.language} • ${channel.country.ifBlank { "Global" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = channel.primaryUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (channel.isActive) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Switch(
                            checked = channel.isActive,
                            onCheckedChange = { onToggleActive(channel) },
                            modifier = Modifier.testTag("admin_toggle_${channel.id}"),
                        )
                        IconButton(
                            onClick = { onEditClick(channel) },
                            modifier = Modifier.testTag("admin_edit_${channel.id}"),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${channel.name}")
                        }
                        IconButton(
                            onClick = { onDeleteClick(channel) },
                            modifier = Modifier.testTag("admin_delete_${channel.id}"),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${channel.name}", tint = LiveCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLiveEventsTab(
    events: List<LiveEvent>,
    onAddClick: () -> Unit,
    onSeedClick: () -> Unit,
    onEditClick: (LiveEvent) -> Unit,
    onToggleActive: (LiveEvent) -> Unit,
    onDeleteClick: (LiveEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "event_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_add_event_button"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Live Event")
                }
                OutlinedButton(
                    onClick = onSeedClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_seed_events_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Load Starters")
                }
            }
        }

        if (events.isEmpty()) {
            item(key = "admin_events_empty") {
                EmptyOrErrorStateCard(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    title = "No Live Events Published",
                    message = "Add a live event link once here and it syncs immediately to every connected phone.",
                )
            }
        } else {
            items(events, key = { it.id }) { event ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.medium,
                    color = MidnightSurfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChannelLogoBadge(channelName = event.title, logoUrl = event.thumbnail, size = 44.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${event.sport} • ${event.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (event.status == "LIVE") LiveCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = event.streamUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (event.isActive) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Switch(
                            checked = event.isActive,
                            onCheckedChange = { onToggleActive(event) },
                            modifier = Modifier.testTag("admin_toggle_event_${event.id}"),
                        )
                        IconButton(
                            onClick = { onEditClick(event) },
                            modifier = Modifier.testTag("admin_edit_event_${event.id}"),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${event.title}")
                        }
                        IconButton(
                            onClick = { onDeleteClick(event) },
                            modifier = Modifier.testTag("admin_delete_event_${event.id}"),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${event.title}", tint = LiveCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBroadcastsTab(
    notices: List<UpdateNotice>,
    onPublish: (String, String, String, (Boolean) -> Unit) -> Unit,
    onDelete: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Notice") }
    var message by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "publish_form") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
                shape = MaterialTheme.shapes.medium,
                color = MidnightSurfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Publish to Update Inbox", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        label = { Text("Headline") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notice_title_input"),
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { if (it.length <= 40) category = it },
                        label = { Text("Badge (e.g. Notice, Channel Update)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { if (it.length <= 1000) message = it },
                        label = { Text("Broadcast Message") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notice_message_input"),
                    )
                    Button(
                        onClick = {
                            onPublish(title, message, category) { ok ->
                                if (ok) {
                                    title = ""
                                    message = ""
                                }
                            }
                        },
                        enabled = title.isNotBlank() && message.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_publish_notice_button"),
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Broadcast Live")
                    }
                }
            }
        }

        items(notices, key = { it.id }) { notice ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
                shape = MaterialTheme.shapes.medium,
                color = MidnightSurfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notice.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            notice.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onDelete(notice.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete notice", tint = LiveCrimson)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReportsTab(
    reportsState: UiState<List<BrokenReport>>,
    channels: List<Channel>,
    onFixChannel: (Channel) -> Unit,
    onDeleteReport: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    when (reportsState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        }

        is UiState.Error -> {
            Box(Modifier.padding(top = 16.dp)) {
                EmptyOrErrorStateCard(
                    icon = Icons.Default.ReportProblem,
                    title = "Admin Verification Required",
                    message = reportsState.message,
                )
            }
        }

        is UiState.Success -> {
            val reports = reportsState.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (reports.isNotEmpty()) {
                    item(key = "clear_reports_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${reports.size} Viewer Reports", style = MaterialTheme.typography.titleMedium)
                            TextButton(
                                onClick = onClearAll,
                                modifier = Modifier.testTag("admin_clear_all_reports_button"),
                            ) {
                                Text("Clear All", color = LiveCrimson)
                            }
                        }
                    }
                }

                if (reports.isEmpty()) {
                    item(key = "reports_empty") {
                        EmptyOrErrorStateCard(
                            icon = Icons.Default.CheckCircle,
                            title = "No Broken Stream Reports",
                            message = "When viewers tap Report Broken Link on an offline stream, reports appear here in real time.",
                        )
                    }
                } else {
                    items(reports, key = { it.id }) { report ->
                        val dateText = report.timestamp?.toDate()?.let {
                            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(it)
                        } ?: "Just now"
                        val matchingChannel = channels.firstOrNull { it.id == report.channelId }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
                            shape = MaterialTheme.shapes.medium,
                            color = MidnightSurfaceContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = report.channelName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = AmberGlow,
                                    )
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = report.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (matchingChannel != null) {
                                        TextButton(onClick = { onFixChannel(matchingChannel) }) {
                                            Text("Edit Channel URL")
                                        }
                                    }
                                    TextButton(
                                        onClick = { onDeleteReport(report.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = LiveCrimson),
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminConfigTab(
    onUpdatePin: (String) -> Unit,
) {
    val context = LocalContext.current
    val databaseId = remember(context) {
        context.getString(R.string.firestore_database_id)
    }
    var newPin by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "db_info") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
                shape = MaterialTheme.shapes.medium,
                color = MidnightSurfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Firebase Remote Sync Status", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Active Firestore Database ID:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        color = MidnightSurfaceHigh,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = databaseId,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Text(
                        text = "To move to another Firebase project without rewriting app logic, swap app/google-services.json, update R.string.firestore_database_id (or CUSTOM_DATABASE_ID_OVERRIDE in FirebaseConfig.kt), and deploy firestore.rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "pin_update") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
                shape = MaterialTheme.shapes.medium,
                color = MidnightSurfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Change Device Admin PIN", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 16 && it.all { ch -> ch.isDigit() }) newPin = it },
                        label = { Text("New PIN (4-16 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            onUpdatePin(newPin)
                            newPin = ""
                        },
                        enabled = newPin.length in 4..16,
                    ) {
                        Text("Save New PIN")
                    }
                }
            }
        }
    }
}
