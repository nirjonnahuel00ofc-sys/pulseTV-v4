package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.model.BannerNotice
import com.example.model.Channel
import com.example.model.UiState
import com.example.ui.components.ChannelListCard
import com.example.ui.components.HeroBannerCard
import com.example.ui.components.NoticeBannerCarousel
import com.example.ui.components.RecentChannelChipCard
import com.example.ui.player.StreamPlayerSection
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MidnightSurfaceContainer
import com.example.ui.theme.MidnightSurfaceHigh
import com.example.util.BannerImageUtils

@Composable
fun HomeScreen(
    channelsState: UiState<List<Channel>>,
    activeBanners: List<BannerNotice> = emptyList(),
    activeChannel: Channel?,
    recentChannels: List<Channel>,
    favoriteIds: Set<String>,
    categories: List<String>,
    languages: List<String>,
    selectedCategory: String,
    selectedLanguage: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onClosePlayer: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onReportBrokenLink: (Channel, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var selectedBanner by remember { mutableStateOf<BannerNotice?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Active Stream Player pinned cleanly above catalog on phones, or Hero Banner when idle
        if (activeChannel != null) {
            Spacer(Modifier.height(6.dp))
            StreamPlayerSection(
                channel = activeChannel,
                isFavorite = activeChannel.id in favoriteIds,
                onToggleFavorite = { onToggleFavorite(activeChannel.id) },
                onClosePlayer = onClosePlayer,
                onReportBrokenLink = onReportBrokenLink,
            )
            Spacer(Modifier.height(10.dp))
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth >= 600.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (activeChannel == null) {
                    if (activeBanners.isNotEmpty()) {
                        item(key = "notice_banners") {
                            Spacer(Modifier.height(4.dp))
                            NoticeBannerCarousel(
                                banners = activeBanners,
                                onBannerClick = { selectedBanner = it },
                            )
                        }
                    }

                    item(key = "hero") {
                        val list = (channelsState as? UiState.Success)?.data.orEmpty()
                        if (activeBanners.isEmpty()) {
                            Spacer(Modifier.height(4.dp))
                        }
                        HeroBannerCard(
                            liveChannelCount = list.size,
                            featuredChannel = list.firstOrNull(),
                            onWatchFeatured = onPlayChannel,
                        )
                    }
                }

                // Search + Language Filter Row
                item(key = "search_bar") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search channels, genres...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                        modifier = Modifier.testTag("clear_search_button"),
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.cd_search_clear),
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MidnightSurfaceContainer,
                                unfocusedContainerColor = MidnightSurfaceContainer,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderSubtle,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("channel_search_input"),
                        )

                        Box {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectedLanguage != "All") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MidnightSurfaceContainer
                                },
                                onClick = { languageMenuExpanded = true },
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("language_filter_button"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = "Filter by language",
                                        tint = if (selectedLanguage != "All") ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (selectedLanguage != "All") {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = selectedLanguage,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ElectricCyan,
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false },
                            ) {
                                languages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(if (lang == "All") "All Languages" else lang) },
                                        onClick = {
                                            onSelectLanguage(lang)
                                            languageMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Chips
                item(key = "categories") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(categories, key = { it }) { category ->
                            val selected = category.equals(selectedCategory, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCategory(category) },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan,
                                    containerColor = MidnightSurfaceContainer,
                                ),
                                modifier = Modifier.testTag("category_chip_$category"),
                            )
                        }
                    }
                }

                // Recently Watched Rail
                if (recentChannels.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "All") {
                    item(key = "recent_header") {
                        Text(
                            text = "Recently Watched",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    item(key = "recent_rail") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(recentChannels, key = { "recent_${it.id}" }) { ch ->
                                RecentChannelChipCard(
                                    channel = ch,
                                    isPlaying = activeChannel?.id == ch.id,
                                    onClick = { onPlayChannel(ch) },
                                )
                            }
                        }
                    }
                }

                // Channels Header
                item(key = "channels_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (selectedCategory == "All") "Live Channels" else "$selectedCategory Channels",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (channelsState is UiState.Success) {
                            Text(
                                text = "${channelsState.data.size} active",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                when (channelsState) {
                    is UiState.Loading -> {
                        item(key = "loading") {
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
                        item(key = "error") {
                            EmptyOrErrorStateCard(
                                icon = Icons.Default.CloudOff,
                                title = "Sync Unavailable",
                                message = channelsState.message,
                            )
                        }
                    }

                    is UiState.Success -> {
                        val list = channelsState.data
                        if (list.isEmpty()) {
                            item(key = "empty") {
                                val isFiltered = searchQuery.isNotBlank() ||
                                    selectedCategory != "All" ||
                                    selectedLanguage != "All"
                                EmptyOrErrorStateCard(
                                    icon = Icons.Default.LiveTv,
                                    title = if (isFiltered) "No Matching Channels" else "No Live Channels Published",
                                    message = if (isFiltered) {
                                        "Try clearing your search query or switching category/language filters."
                                    } else {
                                        "Long-press the PulseTV logo in the top bar to open the Admin Panel and publish live streams."
                                    },
                                    actionLabel = if (isFiltered) "Reset Filters" else null,
                                    onAction = if (isFiltered) {
                                        {
                                            onSearchQueryChange("")
                                            onSelectCategory("All")
                                            onSelectLanguage("All")
                                        }
                                    } else null,
                                )
                            }
                        } else if (isWide) {
                            val rows = list.chunked(2)
                            items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    pair.forEach { channel ->
                                        ChannelListCard(
                                            channel = channel,
                                            isPlaying = activeChannel?.id == channel.id,
                                            isFavorite = channel.id in favoriteIds,
                                            onPlay = { onPlayChannel(channel) },
                                            onToggleFavorite = { onToggleFavorite(channel.id) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(list, key = { it.id }) { channel ->
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

    selectedBanner?.let { banner ->
        val model = remember(banner.imageData) { BannerImageUtils.toCoilModel(banner.imageData) }
        AlertDialog(
            onDismissRequest = { selectedBanner = null },
            title = { Text(banner.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SubcomposeAsyncImage(
                        model = model,
                        contentDescription = banner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MidnightSurfaceHigh),
                    )
                    if (banner.message.isNotBlank()) {
                        Text(
                            text = banner.message,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                if (banner.actionUrl.isNotBlank()) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(banner.actionUrl))
                                )
                            }
                            selectedBanner = null
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open Link")
                    }
                } else {
                    TextButton(onClick = { selectedBanner = null }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (banner.actionUrl.isNotBlank()) {
                    TextButton(onClick = { selectedBanner = null }) {
                        Text("Close")
                    }
                }
            },
        )
    }
}

@Composable
fun EmptyOrErrorStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MidnightSurfaceHigh,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
