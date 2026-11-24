package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Channel
import com.example.ui.components.ChannelListCard

@Composable
fun FavoritesScreen(
    favoriteChannels: List<Channel>,
    recentChannels: List<Channel>,
    activeChannelId: String?,
    favoriteIds: Set<String>,
    onPlayChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onClearRecents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "fav_header") {
            Text(
                text = "Saved Favorites",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        if (favoriteChannels.isEmpty()) {
            item(key = "fav_empty") {
                EmptyOrErrorStateCard(
                    icon = Icons.Default.Favorite,
                    title = "No Favorites Yet",
                    message = "Tap the heart icon on any live channel or inside the video player to pin it here.",
                )
            }
        } else {
            items(favoriteChannels, key = { "fav_${it.id}" }) { channel ->
                ChannelListCard(
                    channel = channel,
                    isPlaying = activeChannelId == channel.id,
                    isFavorite = channel.id in favoriteIds,
                    onPlay = { onPlayChannel(channel) },
                    onToggleFavorite = { onToggleFavorite(channel.id) },
                )
            }
        }

        item(key = "recent_section_header") {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recently Watched",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (recentChannels.isNotEmpty()) {
                    TextButton(
                        onClick = onClearRecents,
                        modifier = Modifier.testTag("clear_recents_button"),
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Text(" Clear")
                    }
                }
            }
        }

        if (recentChannels.isEmpty()) {
            item(key = "recent_empty") {
                Text(
                    text = "Channels you watch will appear here for one-tap resume.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(recentChannels, key = { "history_${it.id}" }) { channel ->
                ChannelListCard(
                    channel = channel,
                    isPlaying = activeChannelId == channel.id,
                    isFavorite = channel.id in favoriteIds,
                    onPlay = { onPlayChannel(channel) },
                    onToggleFavorite = { onToggleFavorite(channel.id) },
                )
            }
        }
    }
}
