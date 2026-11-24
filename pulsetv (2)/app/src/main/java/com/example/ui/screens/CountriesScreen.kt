package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.Channel
import com.example.model.UiState
import com.example.ui.components.ChannelListCard
import com.example.ui.player.StreamPlayerSection
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MidnightSurfaceContainer
import com.example.ui.theme.MidnightSurfaceHigh

@Composable
fun CountriesScreen(
    channelsState: UiState<List<Channel>>,
    countryPlaylists: Map<String, List<Channel>>,
    availableCountries: List<String>,
    selectedCountry: String,
    activeChannel: Channel?,
    favoriteIds: Set<String>,
    onSelectCountry: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onClosePlayer: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onReportBrokenLink: (Channel, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePlaylist = remember(activeChannel, countryPlaylists) {
        if (activeChannel == null) emptyList()
        else {
            val key = activeChannel.country.trim().ifBlank { "Global" }
            countryPlaylists[key]
                ?: countryPlaylists.values.firstOrNull { list -> list.any { it.id == activeChannel.id } }
                ?: emptyList()
        }
    }
    val activeIndex = remember(activeChannel, activePlaylist) {
        if (activeChannel == null) -1
        else activePlaylist.indexOfFirst { it.id == activeChannel.id }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (activeChannel != null) {
            Spacer(Modifier.height(6.dp))
            StreamPlayerSection(
                channel = activeChannel,
                isFavorite = activeChannel.id in favoriteIds,
                onToggleFavorite = { onToggleFavorite(activeChannel.id) },
                onClosePlayer = onClosePlayer,
                onReportBrokenLink = onReportBrokenLink,
            )
            if (activePlaylist.size > 1 && activeIndex >= 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = MidnightSurfaceHigh,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${activeChannel.country.ifBlank { "Global" }} Playlist",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Channel ${activeIndex + 1} of ${activePlaylist.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val prevIdx = if (activeIndex - 1 < 0) activePlaylist.lastIndex else activeIndex - 1
                                    onPlayChannel(activePlaylist[prevIdx])
                                },
                                modifier = Modifier.testTag("playlist_prev_button"),
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous channel in playlist")
                            }
                            IconButton(
                                onClick = {
                                    val nextIdx = (activeIndex + 1) % activePlaylist.size
                                    onPlayChannel(activePlaylist[nextIdx])
                                },
                                modifier = Modifier.testTag("playlist_next_button"),
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next channel in playlist")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "country_header_banner") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.25f), MaterialTheme.shapes.large),
                    shape = MaterialTheme.shapes.large,
                    color = MidnightSurfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Country Playlists",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Browse & stream live TV playlists organized by country.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item(key = "country_filter_chips") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(availableCountries, key = { it }) { country ->
                        val selected = country.equals(selectedCountry, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCountry(country) },
                            label = { Text(country) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                containerColor = MidnightSurfaceContainer,
                            ),
                            modifier = Modifier.testTag("country_chip_$country"),
                        )
                    }
                }
            }

            when (channelsState) {
                is UiState.Loading -> {
                    item(key = "country_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = ElectricCyan)
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "country_error") {
                        EmptyOrErrorStateCard(
                            icon = Icons.Default.Public,
                            title = "Country Playlists Unavailable",
                            message = channelsState.message,
                        )
                    }
                }

                is UiState.Success -> {
                    if (countryPlaylists.isEmpty()) {
                        item(key = "country_empty") {
                            EmptyOrErrorStateCard(
                                icon = Icons.Default.Public,
                                title = "No Country Playlists Yet",
                                message = "Channels added in Admin Studio with a Country field appear grouped into playable Country Playlists here.",
                                actionLabel = if (selectedCountry != "All") "Show All Countries" else null,
                                onAction = if (selectedCountry != "All") {
                                    { onSelectCountry("All") }
                                } else null,
                            )
                        }
                    } else {
                        countryPlaylists.forEach { (countryName, playlistChannels) ->
                            item(key = "playlist_header_$countryName") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MidnightSurfaceHigh,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = countryName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = "${playlistChannels.size} channels in playlist",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Button(
                                            onClick = { onPlayChannel(playlistChannels.first()) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ElectricCyan,
                                                contentColor = Color(0xFF001F26),
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("play_country_playlist_$countryName"),
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Play Playlist")
                                        }
                                    }
                                }
                            }

                            items(
                                items = playlistChannels,
                                key = { "country_${countryName}_${it.id}" },
                            ) { channel ->
                                ChannelListCard(
                                    channel = channel,
                                    isPlaying = activeChannel?.id == channel.id,
                                    isFavorite = channel.id in favoriteIds,
                                    onPlay = { onPlayChannel(channel) },
                                    onToggleFavorite = { onToggleFavorite(channel.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
