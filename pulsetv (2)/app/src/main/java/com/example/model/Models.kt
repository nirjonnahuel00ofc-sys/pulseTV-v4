package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class Channel(
    val id: String = "",
    val name: String = "",
    val category: String = "Entertainment",
    val language: String = "English",
    val country: String = "Global",
    val logo: String = "",
    val primaryUrl: String = "",
    val backupUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Keep
data class LiveEvent(
    val id: String = "",
    val title: String = "",
    val sport: String = "Cricket",
    val teams: String = "",
    val status: String = "LIVE",
    val thumbnail: String = "",
    val streamUrl: String = "",
    val backupUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toPlayableChannel(): Channel = Channel(
        id = id,
        name = title,
        category = sport,
        language = if (teams.isNotBlank()) teams else status,
        logo = thumbnail,
        primaryUrl = streamUrl,
        backupUrl = backupUrl,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Keep
data class UpdateNotice(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "Notice",
    val timestamp: Timestamp? = null,
)

@Keep
data class BannerNotice(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val imageData: String = "",
    val actionUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

@Keep
data class AppAnalyticsSummary(
    val appOpens: Long = 0L,
    val channelPlays: Long = 0L,
    val eventPlays: Long = 0L,
    val updatedAt: Timestamp? = null,
)

@Keep
data class BrokenReport(
    val id: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val reason: String = "",
    val timestamp: Timestamp? = null,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface PlaybackStatus {
    data object Idle : PlaybackStatus
    data object Buffering : PlaybackStatus
    data class Playing(val usingBackup: Boolean) : PlaybackStatus
    data class Reconnecting(val attempt: Int, val usingBackup: Boolean) : PlaybackStatus
    data class Offline(val reason: String) : PlaybackStatus
}

object ChannelCatalogDefaults {
    val CATEGORIES = listOf(
        "All",
        "News",
        "Sports",
        "Movies",
        "Entertainment",
        "Kids",
        "Documentary",
        "Music",
    )

    val LANGUAGES = listOf(
        "Bengali",
        "English",
        "Hindi",
        "Telugu",
        "Tamil",
        "Malayalam",
        "Kannada",
        "Marathi",
        "Gujarati",
        "Punjabi",
        "Odia",
        "Urdu",
        "Arabic",
        "Spanish",
        "French",
        "Global",
    )

    val COUNTRIES = listOf(
        "Bangladesh",
        "India",
        "Pakistan",
        "United Kingdom",
        "United States",
        "UAE",
        "Saudi Arabia",
        "Global",
    )

    val REPORT_REASONS = listOf(
        "Stream will not load / Offline",
        "Infinite buffering or stalled video",
        "Audio or video out of sync",
        "Wrong channel feed",
    )

    /**
     * Real, publicly accessible HLS (.m3u8) test streams that an Admin can
     * import in one tap to verify live playback and backup URL failover.
     */
    val PUBLIC_HLS_STARTERS = listOf(
        Channel(
            name = "Big Buck Bunny Cinema",
            category = "Movies",
            language = "English",
            country = "United States",
            logo = "",
            primaryUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            backupUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            isActive = true,
        ),
        Channel(
            name = "Tears of Steel 4K Feed",
            category = "Entertainment",
            language = "English",
            country = "United Kingdom",
            logo = "",
            primaryUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            backupUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isActive = true,
        ),
        Channel(
            name = "Akamai Live Sports Test",
            category = "Sports",
            language = "Global",
            country = "Global",
            logo = "",
            primaryUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
            backupUrl = "https://moctobpltc-i.akamaihd.net/hls/live/571329/eight/playlist.m3u8",
            isActive = true,
        ),
        Channel(
            name = "Apple BipBop Broadcast",
            category = "News",
            language = "English",
            country = "United States",
            logo = "",
            primaryUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            backupUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isActive = true,
        ),
        Channel(
            name = "Failover Demo (Broken Primary)",
            category = "Documentary",
            language = "Global",
            country = "Global",
            logo = "",
            primaryUrl = "https://invalid-stream.example.invalid/offline.m3u8",
            backupUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isActive = true,
        ),
    )
}

object LiveEventDefaults {
    val SPORTS = listOf(
        "All",
        "Cricket",
        "Football",
        "Combat",
        "Motorsport",
        "Basketball",
        "Special",
    )

    val STATUSES = listOf("LIVE", "UPCOMING", "ENDED")

    val STARTER_EVENTS = listOf(
        LiveEvent(
            title = "Super T20 International Clash",
            sport = "Cricket",
            teams = "T20 Series • Stadium Feed",
            status = "LIVE",
            thumbnail = "",
            streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
            backupUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isActive = true,
        ),
        LiveEvent(
            title = "Champions League Matchday Live",
            sport = "Football",
            teams = "European Cup • Multi-Server HD",
            status = "LIVE",
            thumbnail = "",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            backupUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            isActive = true,
        ),
        LiveEvent(
            title = "Global Grand Prix Track Feed",
            sport = "Motorsport",
            teams = "World Circuit • Qualifying",
            status = "UPCOMING",
            thumbnail = "",
            streamUrl = "https://moctobpltc-i.akamaihd.net/hls/live/571329/eight/playlist.m3u8",
            backupUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isActive = true,
        ),
    )
}
