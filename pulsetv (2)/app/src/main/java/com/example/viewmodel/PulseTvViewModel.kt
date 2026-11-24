package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.config.FirebaseConfig
import com.example.data.BannerAndAnalyticsRepository
import com.example.data.TvRepository
import com.example.data.UserPreferencesRepository
import com.example.model.AppAnalyticsSummary
import com.example.model.BannerNotice
import com.example.model.BrokenReport
import com.example.model.Channel
import com.example.model.ChannelCatalogDefaults
import com.example.model.LiveEvent
import com.example.model.LiveEventDefaults
import com.example.model.M3uPlaylistParser
import com.example.model.UiState
import com.example.model.UpdateNotice
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class PulseTvViewModel(
    private val tvRepository: TvRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val bannerAnalyticsRepository: BannerAndAnalyticsRepository,
    private val auth: FirebaseAuth = Firebase.auth,
) : ViewModel() {

    init {
        viewModelScope.launch {
            bannerAnalyticsRepository.incrementAppOpen()
        }
    }

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isImportingPlaylist = MutableStateFlow(false)
    val isImportingPlaylist: StateFlow<Boolean> = _isImportingPlaylist.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = auth.currentUser,
    )

    val allChannelsState: StateFlow<UiState<List<Channel>>> = tvRepository.observeChannels()
        .map<List<Channel>, UiState<List<Channel>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing channels", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to sync channels"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading,
        )

    val allLiveEventsState: StateFlow<UiState<List<LiveEvent>>> = tvRepository.observeLiveEvents()
        .map<List<LiveEvent>, UiState<List<LiveEvent>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing live events", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to sync live events"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading,
        )

    val updatesState: StateFlow<UiState<List<UpdateNotice>>> = tvRepository.observeUpdatesInbox()
        .map<List<UpdateNotice>, UiState<List<UpdateNotice>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing updates inbox", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to sync updates"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading,
        )

    val allBannersState: StateFlow<UiState<List<BannerNotice>>> = bannerAnalyticsRepository.observeBanners()
        .map<List<BannerNotice>, UiState<List<BannerNotice>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing banners", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to sync notice banners"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading,
        )

    val activeBanners: StateFlow<List<BannerNotice>> = allBannersState
        .map { state ->
            (state as? UiState.Success)?.data.orEmpty().filter { it.isActive }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList(),
        )

    private val _isAdminPinUnlocked = MutableStateFlow(false)
    val isAdminPinUnlocked: StateFlow<Boolean> = _isAdminPinUnlocked.asStateFlow()

    val brokenReportsState: StateFlow<UiState<List<BrokenReport>>> =
        combine(currentUser, _isAdminPinUnlocked) { user, unlocked ->
            user != null && unlocked
        }.flatMapLatest { canObserveReports ->
            if (!canObserveReports) {
                flowOf<UiState<List<BrokenReport>>>(UiState.Success(emptyList()))
            } else {
                tvRepository.observeBrokenReports()
                    .map<List<BrokenReport>, UiState<List<BrokenReport>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        val root = firestoreError ?: error
                        Log.w(TAG, "Error observing broken reports", root)
                        emit(
                            UiState.Error(
                                if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                    "Signed-in account does not have admin permission in firestore.rules."
                                } else {
                                    root.localizedMessage ?: "Failed to load broken reports"
                                }
                            )
                        )
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Success(emptyList()),
        )

    val analyticsState: StateFlow<UiState<AppAnalyticsSummary>> =
        combine(currentUser, _isAdminPinUnlocked) { user, unlocked ->
            user != null && unlocked
        }.flatMapLatest { canObserveAnalytics ->
            if (!canObserveAnalytics) {
                flowOf<UiState<AppAnalyticsSummary>>(UiState.Success(AppAnalyticsSummary()))
            } else {
                bannerAnalyticsRepository.observeAppAnalytics()
                    .map<AppAnalyticsSummary, UiState<AppAnalyticsSummary>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        val root = firestoreError ?: error
                        Log.w(TAG, "Error observing app analytics", root)
                        emit(
                            UiState.Error(
                                if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                    "Signed-in account does not have admin analytics permission in firestore.rules."
                                } else {
                                    root.localizedMessage ?: "Failed to load analytics"
                                }
                            )
                        )
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Success(AppAnalyticsSummary()),
        )

    val favoriteIds: StateFlow<Set<String>> = prefsRepository.favoriteIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptySet(),
    )

    val recentIds: StateFlow<List<String>> = prefsRepository.recentIds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
    )

    val adminPin: StateFlow<String> = prefsRepository.adminPin.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FirebaseConfig.DEFAULT_ADMIN_PIN,
    )

    val unreadNoticesCount: StateFlow<Int> = combine(
        updatesState,
        prefsRepository.lastReadInboxMs,
    ) { state, lastReadMs ->
        val list = (state as? UiState.Success)?.data ?: return@combine 0
        list.count { (it.timestamp?.toDate()?.time ?: 0L) > lastReadMs }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = 0,
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("All")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _selectedCountryFilter = MutableStateFlow("All")
    val selectedCountryFilter: StateFlow<String> = _selectedCountryFilter.asStateFlow()

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    private val _selectedLiveEventId = MutableStateFlow<String?>(null)

    private val _eventSearchQuery = MutableStateFlow("")
    val eventSearchQuery: StateFlow<String> = _eventSearchQuery.asStateFlow()

    private val _selectedSport = MutableStateFlow("All")
    val selectedSport: StateFlow<String> = _selectedSport.asStateFlow()

    val activeChannel: StateFlow<Channel?> = combine(
        allChannelsState,
        _selectedChannelId,
    ) { state, selectedId ->
        val channels = (state as? UiState.Success)?.data ?: return@combine null
        if (selectedId == null) return@combine null
        channels.firstOrNull { it.id == selectedId && it.isActive }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null,
    )

    val activeLiveEvent: StateFlow<LiveEvent?> = combine(
        allLiveEventsState,
        _selectedLiveEventId,
    ) { state, selectedId ->
        val events = (state as? UiState.Success)?.data ?: return@combine null
        if (selectedId == null) return@combine null
        events.firstOrNull { it.id == selectedId && it.isActive }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null,
    )

    val availableCategories: StateFlow<List<String>> = allChannelsState.map { state ->
        val channels = (state as? UiState.Success)?.data.orEmpty().filter { it.isActive }
        val dynamicCats = channels.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
        (ChannelCatalogDefaults.CATEGORIES + dynamicCats).distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ChannelCatalogDefaults.CATEGORIES,
    )

    val availableLanguages: StateFlow<List<String>> = allChannelsState.map { state ->
        val channels = (state as? UiState.Success)?.data.orEmpty().filter { it.isActive }
        val langs = channels.map { it.language.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("All") + langs
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = listOf("All"),
    )

    val availableCountries: StateFlow<List<String>> = allChannelsState.map { state ->
        val channels = (state as? UiState.Success)?.data.orEmpty().filter { it.isActive }
        val countries = channels.map { it.country.trim().ifBlank { "Global" } }
            .distinct()
            .sorted()
        listOf("All") + countries
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = listOf("All"),
    )

    val countryPlaylists: StateFlow<Map<String, List<Channel>>> = combine(
        allChannelsState,
        _selectedCountryFilter,
    ) { state, countryFilter ->
        val active = (state as? UiState.Success)?.data.orEmpty().filter { it.isActive }
        val grouped = active.groupBy { it.country.trim().ifBlank { "Global" } }
            .toSortedMap(compareBy<String> { if (it == "Global") 1 else 0 }.thenBy { it })
        if (countryFilter == "All") grouped
        else grouped.filterKeys { it.equals(countryFilter, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyMap(),
    )

    val filteredActiveChannels: StateFlow<UiState<List<Channel>>> = combine(
        allChannelsState,
        _searchQuery,
        _selectedCategory,
        _selectedLanguage,
    ) { state, query, category, language ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val q = query.trim().lowercase()
                val filtered = state.data.filter { ch ->
                    ch.isActive &&
                        (category == "All" || ch.category.equals(category, ignoreCase = true)) &&
                        (language == "All" || ch.language.equals(language, ignoreCase = true)) &&
                        (q.isEmpty() ||
                            ch.name.lowercase().contains(q) ||
                            ch.category.lowercase().contains(q) ||
                            ch.language.lowercase().contains(q) ||
                            ch.country.lowercase().contains(q))
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
    )

    val favoriteChannels: StateFlow<List<Channel>> = combine(
        allChannelsState,
        favoriteIds,
    ) { state, favs ->
        val channels = (state as? UiState.Success)?.data.orEmpty()
        channels.filter { it.isActive && it.id in favs }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
    )

    val recentChannels: StateFlow<List<Channel>> = combine(
        allChannelsState,
        recentIds,
    ) { state, recents ->
        val map = (state as? UiState.Success)?.data.orEmpty()
            .filter { it.isActive }
            .associateBy { it.id }
        recents.mapNotNull { map[it] }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
    )

    val availableSports: StateFlow<List<String>> = allLiveEventsState.map { state ->
        val events = (state as? UiState.Success)?.data.orEmpty().filter { it.isActive }
        val dynamicSports = events.map { it.sport.trim() }.filter { it.isNotBlank() }.distinct()
        (LiveEventDefaults.SPORTS + dynamicSports).distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LiveEventDefaults.SPORTS,
    )

    val filteredActiveLiveEvents: StateFlow<UiState<List<LiveEvent>>> = combine(
        allLiveEventsState,
        _eventSearchQuery,
        _selectedSport,
    ) { state, query, sport ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val q = query.trim().lowercase()
                val statusOrder = mapOf("LIVE" to 0, "UPCOMING" to 1, "ENDED" to 2)
                val filtered = state.data
                    .filter { evt ->
                        evt.isActive &&
                            (sport == "All" || evt.sport.equals(sport, ignoreCase = true)) &&
                            (q.isEmpty() ||
                                evt.title.lowercase().contains(q) ||
                                evt.sport.lowercase().contains(q) ||
                                evt.teams.lowercase().contains(q))
                    }
                    .sortedBy { statusOrder[it.status] ?: 3 }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun selectCountryFilter(country: String) {
        _selectedCountryFilter.value = country
    }

    fun updateEventSearchQuery(query: String) {
        _eventSearchQuery.value = query
    }

    fun selectSport(sport: String) {
        _selectedSport.value = sport
    }

    fun playChannel(channel: Channel) {
        _selectedLiveEventId.value = null
        _selectedChannelId.value = channel.id
        viewModelScope.launch {
            prefsRepository.recordWatched(channel.id)
            bannerAnalyticsRepository.incrementChannelPlay()
        }
    }

    fun playLiveEvent(event: LiveEvent) {
        _selectedChannelId.value = null
        _selectedLiveEventId.value = event.id
        viewModelScope.launch {
            bannerAnalyticsRepository.incrementEventPlay()
        }
    }

    fun stopPlayback() {
        _selectedChannelId.value = null
        _selectedLiveEventId.value = null
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            prefsRepository.toggleFavorite(channelId)
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            prefsRepository.clearRecentlyWatched()
            _toastMessage.tryEmit("Recently watched history cleared")
        }
    }

    fun markInboxAsRead() {
        val notices = (updatesState.value as? UiState.Success)?.data.orEmpty()
        val latestMs = notices.maxOfOrNull { it.timestamp?.toDate()?.time ?: 0L } ?: System.currentTimeMillis()
        viewModelScope.launch {
            prefsRepository.markInboxRead(latestMs)
        }
    }

    fun verifyAndUnlockAdmin(enteredPin: String): Boolean {
        val valid = enteredPin == adminPin.value
        if (valid) {
            _isAdminPinUnlocked.value = true
        }
        return valid
    }

    fun lockAdminPanel() {
        _isAdminPinUnlocked.value = false
    }

    fun updateAdminPin(newPin: String) {
        viewModelScope.launch {
            prefsRepository.updateAdminPin(newPin)
            _toastMessage.tryEmit("Admin PIN updated on this device")
        }
    }

    fun submitBrokenReport(channel: Channel, reason: String) {
        viewModelScope.launch {
            val result = tvRepository.submitBrokenReport(channel, reason)
            _toastMessage.tryEmit(
                if (result.isSuccess) "Report sent to admins. Thank you!"
                else "Could not submit report: ${result.exceptionOrNull()?.localizedMessage ?: "Network error"}"
            )
        }
    }

    fun createChannel(
        name: String,
        category: String,
        language: String,
        country: String,
        logo: String,
        primaryUrl: String,
        backupUrl: String,
        isActive: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = tvRepository.createChannel(
                name = name,
                category = category,
                language = language,
                country = country,
                logo = logo,
                primaryUrl = primaryUrl,
                backupUrl = backupUrl,
                isActive = isActive,
            )
            if (result.isSuccess) {
                _toastMessage.tryEmit("Channel \"$name\" published live")
                onComplete(true)
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                onComplete(false)
            }
        }
    }

    fun updateChannel(
        channelId: String,
        name: String,
        category: String,
        language: String,
        country: String,
        logo: String,
        primaryUrl: String,
        backupUrl: String,
        isActive: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = tvRepository.updateChannel(
                channelId = channelId,
                name = name,
                category = category,
                language = language,
                country = country,
                logo = logo,
                primaryUrl = primaryUrl,
                backupUrl = backupUrl,
                isActive = isActive,
            )
            if (result.isSuccess) {
                _toastMessage.tryEmit("Channel \"$name\" updated")
                onComplete(true)
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                onComplete(false)
            }
        }
    }

    fun toggleChannelActive(channel: Channel) {
        viewModelScope.launch {
            val result = tvRepository.toggleChannelActive(channel.id, !channel.isActive)
            if (result.isSuccess) {
                val stateLabel = if (!channel.isActive) "activated" else "paused"
                _toastMessage.tryEmit("${channel.name} $stateLabel")
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
            }
        }
    }

    fun deleteChannel(channel: Channel) {
        viewModelScope.launch {
            val result = tvRepository.deleteChannel(channel.id)
            if (result.isSuccess) {
                if (_selectedChannelId.value == channel.id) {
                    _selectedChannelId.value = null
                }
                _toastMessage.tryEmit("Deleted ${channel.name}")
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
            }
        }
    }

    fun publishUpdateNotice(title: String, message: String, category: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = tvRepository.publishUpdateNotice(title, message, category)
            if (result.isSuccess) {
                _toastMessage.tryEmit("Broadcast published to Update Inbox")
                onComplete(true)
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                onComplete(false)
            }
        }
    }

    fun deleteUpdateNotice(noticeId: String) {
        viewModelScope.launch {
            val result = tvRepository.deleteUpdateNotice(noticeId)
            _toastMessage.tryEmit(
                if (result.isSuccess) "Notice removed"
                else friendlyAdminError(result.exceptionOrNull())
            )
        }
    }

    fun deleteBrokenReport(reportId: String) {
        viewModelScope.launch {
            val result = tvRepository.deleteBrokenReport(reportId)
            _toastMessage.tryEmit(
                if (result.isSuccess) "Report cleared"
                else friendlyAdminError(result.exceptionOrNull())
            )
        }
    }

    fun clearAllBrokenReports(reports: List<BrokenReport>) {
        viewModelScope.launch {
            val result = tvRepository.clearAllBrokenReports(reports.map { it.id })
            _toastMessage.tryEmit(
                if (result.isSuccess) "Cleared ${reports.size} reports"
                else friendlyAdminError(result.exceptionOrNull())
            )
        }
    }

    fun seedPublicTestChannels() {
        viewModelScope.launch {
            val result = tvRepository.seedPublicTestChannels()
            _toastMessage.tryEmit(
                if (result.isSuccess) "Loaded 5 public HLS test channels"
                else friendlyAdminError(result.exceptionOrNull())
            )
        }
    }

    fun importBundledIndiaPlaylist(context: Context) {
        if (_isImportingPlaylist.value) return
        viewModelScope.launch {
            _isImportingPlaylist.value = true
            try {
                val parsed = withContext(Dispatchers.IO) {
                    context.resources.openRawResource(R.raw.india_iptv_playlist)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                        .let { M3uPlaylistParser.parse(it, defaultCountry = "India") }
                }
                val result = tvRepository.importChannelsBatch(parsed)
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    _toastMessage.tryEmit(
                        if (count > 0) "Synced $count India TV channels live to all phones"
                        else "All ${parsed.size} India IPTV channels are already synced"
                    )
                } else {
                    _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _toastMessage.tryEmit("Could not read bundled playlist: ${e.localizedMessage ?: "Error"}")
            } finally {
                _isImportingPlaylist.value = false
            }
        }
    }

    fun importM3uPlaylistFromUri(context: Context, uri: Uri) {
        if (_isImportingPlaylist.value) return
        viewModelScope.launch {
            _isImportingPlaylist.value = true
            try {
                val rawText = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: ""
                }
                val parsed = M3uPlaylistParser.parse(rawText)
                if (parsed.isEmpty()) {
                    _toastMessage.tryEmit("No valid stream URLs found in selected M3U file")
                    return@launch
                }
                val result = tvRepository.importChannelsBatch(parsed)
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    _toastMessage.tryEmit(
                        if (count > 0) "Synced $count channels from M3U playlist"
                        else "All ${parsed.size} channels from this M3U are already synced"
                    )
                } else {
                    _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _toastMessage.tryEmit("Failed to import M3U: ${e.localizedMessage ?: "Read error"}")
            } finally {
                _isImportingPlaylist.value = false
            }
        }
    }

    fun createLiveEvent(
        title: String,
        sport: String,
        teams: String,
        status: String,
        thumbnail: String,
        streamUrl: String,
        backupUrl: String,
        isActive: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = tvRepository.createLiveEvent(
                title, sport, teams, status, thumbnail, streamUrl, backupUrl, isActive
            )
            if (result.isSuccess) {
                _toastMessage.tryEmit("Live event \"$title\" published to all devices")
                onComplete(true)
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                onComplete(false)
            }
        }
    }

    fun updateLiveEvent(
        eventId: String,
        title: String,
        sport: String,
        teams: String,
        status: String,
        thumbnail: String,
        streamUrl: String,
        backupUrl: String,
        isActive: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = tvRepository.updateLiveEvent(
                eventId, title, sport, teams, status, thumbnail, streamUrl, backupUrl, isActive
            )
            if (result.isSuccess) {
                _toastMessage.tryEmit("Live event \"$title\" updated")
                onComplete(true)
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                onComplete(false)
            }
        }
    }

    fun toggleLiveEventActive(event: LiveEvent) {
        viewModelScope.launch {
            val result = tvRepository.toggleLiveEventActive(event.id, !event.isActive)
            if (result.isSuccess) {
                val stateLabel = if (!event.isActive) "live on all devices" else "hidden"
                _toastMessage.tryEmit("${event.title} is now $stateLabel")
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
            }
        }
    }

    fun deleteLiveEvent(event: LiveEvent) {
        viewModelScope.launch {
            val result = tvRepository.deleteLiveEvent(event.id)
            if (result.isSuccess) {
                if (_selectedLiveEventId.value == event.id) {
                    _selectedLiveEventId.value = null
                }
                _toastMessage.tryEmit("Removed ${event.title}")
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
            }
        }
    }

    fun seedStarterLiveEvents() {
        viewModelScope.launch {
            val result = tvRepository.seedStarterLiveEvents()
            _toastMessage.tryEmit(
                if (result.isSuccess) "Published starter live events to all devices"
                else friendlyAdminError(result.exceptionOrNull())
            )
        }
    }

    fun createBanner(
        title: String,
        message: String,
        imageData: String,
        actionUrl: String,
        isActive: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = bannerAnalyticsRepository.createBanner(
                title = title,
                message = message,
                imageData = imageData,
                actionUrl = actionUrl,
                isActive = isActive,
            )
            if (result.isSuccess) {
                _toastMessage.tryEmit("Notice banner \"$title\" published live")
                onComplete(true)
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                onComplete(false)
            }
        }
    }

    fun updateBanner(
        bannerId: String,
        title: String,
        message: String,
        imageData: String,
        actionUrl: String,
        isActive: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = bannerAnalyticsRepository.updateBanner(
                bannerId = bannerId,
                title = title,
                message = message,
                imageData = imageData,
                actionUrl = actionUrl,
                isActive = isActive,
            )
            if (result.isSuccess) {
                _toastMessage.tryEmit("Notice banner \"$title\" updated")
                onComplete(true)
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
                onComplete(false)
            }
        }
    }

    fun toggleBannerActive(banner: BannerNotice) {
        viewModelScope.launch {
            val result = bannerAnalyticsRepository.toggleBannerActive(banner.id, !banner.isActive)
            if (result.isSuccess) {
                val stateLabel = if (!banner.isActive) "visible" else "hidden"
                _toastMessage.tryEmit("Banner \"${banner.title}\" is now $stateLabel")
            } else {
                _toastMessage.tryEmit(friendlyAdminError(result.exceptionOrNull()))
            }
        }
    }

    fun deleteBanner(banner: BannerNotice) {
        viewModelScope.launch {
            val result = bannerAnalyticsRepository.deleteBanner(banner.id)
            _toastMessage.tryEmit(
                if (result.isSuccess) "Removed banner \"${banner.title}\""
                else friendlyAdminError(result.exceptionOrNull())
            )
        }
    }

    private fun friendlyAdminError(error: Throwable?): String {
        val firestoreError = error?.let { unwrapFirestoreError(it) }
        return when {
            firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Permission denied. Ensure your signed-in Google email is listed as admin in firestore.rules."
            else -> error?.localizedMessage ?: "Operation failed"
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "PulseTvVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
