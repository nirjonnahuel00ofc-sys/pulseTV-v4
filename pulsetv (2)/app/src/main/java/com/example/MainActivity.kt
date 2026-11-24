package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.config.FirebaseConfig
import com.example.data.BannerAndAnalyticsRepository
import com.example.data.TvRepository
import com.example.data.UserPreferencesRepository
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.CountriesScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.LiveEventsScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.MidnightSurfaceHigh
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PulseTvTheme
import com.example.viewmodel.PulseTvViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object LiveEventsRoute

@Serializable
data object CountriesRoute

@Serializable
data object FavoritesRoute

@Serializable
data object InboxRoute

@Serializable
data object AdminRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        enableEdgeToEdge()
        setContent {
            PulseTvTheme {
                PulseTvApp()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PulseTvApp() {
    val viewModel: PulseTvViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseConfig.getFirestore(app)
                PulseTvViewModel(
                    tvRepository = TvRepository(db),
                    prefsRepository = UserPreferencesRepository(app),
                    bannerAnalyticsRepository = BannerAndAnalyticsRepository(db),
                )
            }
        }
    )

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val filteredChannelsState by viewModel.filteredActiveChannels.collectAsStateWithLifecycle()
    val allChannelsState by viewModel.allChannelsState.collectAsStateWithLifecycle()
    val filteredLiveEventsState by viewModel.filteredActiveLiveEvents.collectAsStateWithLifecycle()
    val allLiveEventsState by viewModel.allLiveEventsState.collectAsStateWithLifecycle()
    val allBannersState by viewModel.allBannersState.collectAsStateWithLifecycle()
    val activeBanners by viewModel.activeBanners.collectAsStateWithLifecycle()
    val updatesState by viewModel.updatesState.collectAsStateWithLifecycle()
    val brokenReportsState by viewModel.brokenReportsState.collectAsStateWithLifecycle()
    val analyticsState by viewModel.analyticsState.collectAsStateWithLifecycle()
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val activeLiveEvent by viewModel.activeLiveEvent.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
    val recentChannels by viewModel.recentChannels.collectAsStateWithLifecycle()
    val categories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val languages by viewModel.availableLanguages.collectAsStateWithLifecycle()
    val countries by viewModel.availableCountries.collectAsStateWithLifecycle()
    val countryPlaylists by viewModel.countryPlaylists.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedCountryFilter by viewModel.selectedCountryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sports by viewModel.availableSports.collectAsStateWithLifecycle()
    val selectedSport by viewModel.selectedSport.collectAsStateWithLifecycle()
    val eventSearchQuery by viewModel.eventSearchQuery.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNoticesCount.collectAsStateWithLifecycle()
    val isAdminPinUnlocked by viewModel.isAdminPinUnlocked.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isImportingPlaylist by viewModel.isImportingPlaylist.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navigateTo: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isHome = currentDestination?.hasRoute<HomeRoute>() == true
    val isLiveEvents = currentDestination?.hasRoute<LiveEventsRoute>() == true
    val isCountries = currentDestination?.hasRoute<CountriesRoute>() == true
    val isFavorites = currentDestination?.hasRoute<FavoritesRoute>() == true
    val isInbox = currentDestination?.hasRoute<InboxRoute>() == true
    val isAdmin = currentDestination?.hasRoute<AdminRoute>() == true

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Surface(
                    color = MidnightSurface,
                    tonalElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Secret Admin Trigger: Long-press the PulseTV logo to open Admin Studio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (!isHome) navigateTo(HomeRoute)
                                    },
                                    onLongClick = {
                                        navigateTo(AdminRoute)
                                    },
                                )
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .testTag("brand_logo_admin_trigger"),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(LiveCrimson, ElectricCyan)
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = stringResource(R.string.cd_app_logo),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Made By JOY",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = ElectricCyan.copy(alpha = 0.88f),
                                    modifier = Modifier.testTag("made_by_joy_label"),
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.guest_mode_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        if (isAdminPinUnlocked) {
                            IconButton(
                                onClick = { navigateTo(AdminRoute) },
                                modifier = Modifier.testTag("top_admin_shortcut"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Open Admin Studio",
                                    tint = ElectricCyan,
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("more_options_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Contact") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = ElectricCyan)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showContactDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_contact"),
                                )
                                DropdownMenuItem(
                                    text = { Text("App & Sync Info") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Info, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showAboutDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_info"),
                                )
                                DropdownMenuItem(
                                    text = { Text("Admin Studio") },
                                    leadingIcon = {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        navigateTo(AdminRoute)
                                    },
                                    modifier = Modifier.testTag("menu_admin"),
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MidnightSurface,
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        NavigationBarItem(
                            selected = isHome,
                            onClick = { navigateTo(HomeRoute) },
                            icon = {
                                Icon(
                                    if (isHome) Icons.Filled.LiveTv else Icons.Outlined.LiveTv,
                                    contentDescription = "Live TV",
                                )
                            },
                            label = { Text("Live TV") },
                            modifier = Modifier.testTag("nav_live_tv"),
                        )
                        NavigationBarItem(
                            selected = isLiveEvents,
                            onClick = { navigateTo(LiveEventsRoute) },
                            icon = {
                                Icon(
                                    if (isLiveEvents) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                                    contentDescription = "Events",
                                )
                            },
                            label = { Text("Events") },
                            modifier = Modifier.testTag("nav_live_events"),
                        )
                        NavigationBarItem(
                            selected = isCountries,
                            onClick = { navigateTo(CountriesRoute) },
                            icon = {
                                Icon(
                                    if (isCountries) Icons.Filled.Public else Icons.Outlined.Public,
                                    contentDescription = "Country",
                                )
                            },
                            label = { Text("Country") },
                            modifier = Modifier.testTag("nav_countries"),
                        )
                        NavigationBarItem(
                            selected = isFavorites,
                            onClick = { navigateTo(FavoritesRoute) },
                            icon = {
                                Icon(
                                    if (isFavorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorites",
                                )
                            },
                            label = { Text("Favorites") },
                            modifier = Modifier.testTag("nav_favorites"),
                        )
                        NavigationBarItem(
                            selected = isInbox,
                            onClick = { navigateTo(InboxRoute) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge(containerColor = LiveCrimson) {
                                                Text(unreadCount.coerceAtMost(99).toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isInbox) Icons.Filled.Campaign else Icons.Outlined.Campaign,
                                        contentDescription = "Updates",
                                    )
                                }
                            },
                            label = { Text("Updates") },
                            modifier = Modifier.testTag("nav_updates"),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MidnightSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        Spacer(Modifier.weight(0.2f))
                        NavigationRailItem(
                            selected = isHome,
                            onClick = { navigateTo(HomeRoute) },
                            icon = { Icon(Icons.Filled.LiveTv, contentDescription = "Live TV") },
                            label = { Text("Live TV") },
                        )
                        NavigationRailItem(
                            selected = isLiveEvents,
                            onClick = { navigateTo(LiveEventsRoute) },
                            icon = { Icon(Icons.Filled.Bolt, contentDescription = "Events") },
                            label = { Text("Events") },
                        )
                        NavigationRailItem(
                            selected = isCountries,
                            onClick = { navigateTo(CountriesRoute) },
                            icon = { Icon(Icons.Filled.Public, contentDescription = "Country") },
                            label = { Text("Country") },
                        )
                        NavigationRailItem(
                            selected = isFavorites,
                            onClick = { navigateTo(FavoritesRoute) },
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
                            label = { Text("Favorites") },
                        )
                        NavigationRailItem(
                            selected = isInbox,
                            onClick = { navigateTo(InboxRoute) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge(containerColor = LiveCrimson) {
                                                Text(unreadCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Campaign, contentDescription = "Updates")
                                }
                            },
                            label = { Text("Updates") },
                        )
                        if (isAdminPinUnlocked) {
                            NavigationRailItem(
                                selected = isAdmin,
                                onClick = { navigateTo(AdminRoute) },
                                icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin") },
                                label = { Text("Admin") },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier.weight(1f),
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            channelsState = filteredChannelsState,
                            activeBanners = activeBanners,
                            activeChannel = activeChannel,
                            recentChannels = recentChannels,
                            favoriteIds = favoriteIds,
                            categories = categories,
                            languages = languages,
                            selectedCategory = selectedCategory,
                            selectedLanguage = selectedLanguage,
                            searchQuery = searchQuery,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectCategory,
                            onSelectLanguage = viewModel::selectLanguage,
                            onPlayChannel = viewModel::playChannel,
                            onClosePlayer = viewModel::stopPlayback,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onReportBrokenLink = viewModel::submitBrokenReport,
                        )
                    }

                    composable<LiveEventsRoute> {
                        LiveEventsScreen(
                            eventsState = filteredLiveEventsState,
                            activeEvent = activeLiveEvent,
                            sports = sports,
                            selectedSport = selectedSport,
                            searchQuery = eventSearchQuery,
                            onSearchQueryChange = viewModel::updateEventSearchQuery,
                            onSelectSport = viewModel::selectSport,
                            onPlayEvent = viewModel::playLiveEvent,
                            onClosePlayer = viewModel::stopPlayback,
                            onReportBrokenLink = viewModel::submitBrokenReport,
                        )
                    }

                    composable<CountriesRoute> {
                        CountriesScreen(
                            channelsState = allChannelsState,
                            countryPlaylists = countryPlaylists,
                            availableCountries = countries,
                            selectedCountry = selectedCountryFilter,
                            activeChannel = activeChannel,
                            favoriteIds = favoriteIds,
                            onSelectCountry = viewModel::selectCountryFilter,
                            onPlayChannel = viewModel::playChannel,
                            onClosePlayer = viewModel::stopPlayback,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onReportBrokenLink = viewModel::submitBrokenReport,
                        )
                    }

                    composable<FavoritesRoute> {
                        FavoritesScreen(
                            favoriteChannels = favoriteChannels,
                            recentChannels = recentChannels,
                            activeChannelId = activeChannel?.id,
                            favoriteIds = favoriteIds,
                            onPlayChannel = { ch ->
                                viewModel.playChannel(ch)
                                navigateTo(HomeRoute)
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onClearRecents = viewModel::clearRecentHistory,
                        )
                    }

                    composable<InboxRoute> {
                        InboxScreen(
                            updatesState = updatesState,
                            onMarkRead = viewModel::markInboxAsRead,
                        )
                    }

                    composable<AdminRoute> {
                        AdminScreen(
                            isPinUnlocked = isAdminPinUnlocked,
                            currentUser = currentUser,
                            allChannelsState = allChannelsState,
                            allLiveEventsState = allLiveEventsState,
                            allBannersState = allBannersState,
                            updatesState = updatesState,
                            brokenReportsState = brokenReportsState,
                            analyticsState = analyticsState,
                            isImportingPlaylist = isImportingPlaylist,
                            onVerifyPin = viewModel::verifyAndUnlockAdmin,
                            onLockAdmin = {
                                viewModel.lockAdminPanel()
                                navigateTo(HomeRoute)
                            },
                            onUpdateAdminPin = viewModel::updateAdminPin,
                            onCreateChannel = viewModel::createChannel,
                            onUpdateChannel = viewModel::updateChannel,
                            onToggleChannelActive = viewModel::toggleChannelActive,
                            onDeleteChannel = viewModel::deleteChannel,
                            onImportIndiaPlaylist = { viewModel.importBundledIndiaPlaylist(context) },
                            onImportM3uUri = { uri -> viewModel.importM3uPlaylistFromUri(context, uri) },
                            onCreateLiveEvent = viewModel::createLiveEvent,
                            onUpdateLiveEvent = viewModel::updateLiveEvent,
                            onToggleLiveEventActive = viewModel::toggleLiveEventActive,
                            onDeleteLiveEvent = viewModel::deleteLiveEvent,
                            onCreateBanner = viewModel::createBanner,
                            onUpdateBanner = viewModel::updateBanner,
                            onToggleBannerActive = viewModel::toggleBannerActive,
                            onDeleteBanner = viewModel::deleteBanner,
                            onPublishNotice = viewModel::publishUpdateNotice,
                            onDeleteNotice = viewModel::deleteUpdateNotice,
                            onDeleteReport = viewModel::deleteBrokenReport,
                            onClearAllReports = viewModel::clearAllBrokenReports,
                            onSeedPublicChannels = viewModel::seedPublicTestChannels,
                            onSeedStarterLiveEvents = viewModel::seedStarterLiveEvents,
                        )
                    }
                }
            }
        }
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact PulseTV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Need help, stream requests, or support? Reach us directly:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_SENDTO,
                                            Uri.parse("mailto:${FirebaseConfig.CONTACT_EMAIL}"),
                                        )
                                    )
                                }
                            },
                        color = MidnightSurfaceHigh,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = ElectricCyan)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Email", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                Text(FirebaseConfig.CONTACT_EMAIL, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(FirebaseConfig.CONTACT_EMAIL))
                                    scope.launch { snackbarHostState.showSnackbar("Email copied") }
                                },
                                modifier = Modifier.testTag("copy_contact_email_button"),
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy email", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(FirebaseConfig.CONTACT_TELEGRAM_URL),
                                        )
                                    )
                                }
                            },
                        color = MidnightSurfaceHigh,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = ElectricCyan)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Telegram", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                Text(FirebaseConfig.CONTACT_TELEGRAM_DISPLAY, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(FirebaseConfig.CONTACT_TELEGRAM_URL))
                                    scope.launch { snackbarHostState.showSnackbar("Telegram link copied") }
                                },
                                modifier = Modifier.testTag("copy_contact_telegram_button"),
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Telegram link", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showContactDialog = false },
                    modifier = Modifier.testTag("close_contact_dialog_button"),
                ) {
                    Text("Close")
                }
            },
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("PulseTV Cloud Sync") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "All channels, country playlists, live event streams, notice banners, and Update Inbox broadcasts sync in real time via Cloud Firestore across all phones.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Admin Access: Long-press the PulseTV logo in the top bar and enter your Admin PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricCyan,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAboutDialog = false
                        navigateTo(AdminRoute)
                    },
                    modifier = Modifier.testTag("dialog_open_admin_button"),
                ) {
                    Text("Open Admin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}
