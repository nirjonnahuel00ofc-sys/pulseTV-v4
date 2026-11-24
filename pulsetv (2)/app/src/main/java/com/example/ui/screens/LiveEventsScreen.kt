package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Channel
import com.example.model.LiveEvent
import com.example.model.UiState
import com.example.ui.components.ChannelLogoBadge
import com.example.ui.player.StreamPlayerSection
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MidnightSurfaceContainer
import com.example.ui.theme.MidnightSurfaceHigh

@Composable
fun LiveEventsScreen(
    eventsState: UiState<List<LiveEvent>>,
    activeEvent: LiveEvent?,
    sports: List<String>,
    selectedSport: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectSport: (String) -> Unit,
    onPlayEvent: (LiveEvent) -> Unit,
    onClosePlayer: () -> Unit,
    onReportBrokenLink: (Channel, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (activeEvent != null) {
            Spacer(Modifier.height(6.dp))
            StreamPlayerSection(
                channel = activeEvent.toPlayableChannel(),
                isFavorite = false,
                showFavoriteButton = false,
                onToggleFavorite = {},
                onClosePlayer = onClosePlayer,
                onReportBrokenLink = onReportBrokenLink,
            )
            Spacer(Modifier.height(10.dp))
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth >= 600.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "events_banner_header") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(1.dp, BorderSubtle, MaterialTheme.shapes.large),
                        shape = MaterialTheme.shapes.large,
                        color = MidnightSurfaceContainer,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            LiveCrimson.copy(alpha = 0.22f),
                                            ElectricCyan.copy(alpha = 0.12f),
                                            Color.Transparent,
                                        )
                                    )
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LiveCrimson.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = LiveCrimson,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Events Center",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Instant match & event streams synced across all phones in real time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item(key = "event_search_bar") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search matches, teams, tournaments...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.testTag("clear_event_search_button"),
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
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("event_search_input"),
                    )
                }

                item(key = "sport_chips") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sports, key = { it }) { sport ->
                            val selected = sport.equals(selectedSport, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectSport(sport) },
                                label = { Text(sport) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LiveCrimson.copy(alpha = 0.22f),
                                    selectedLabelColor = Color.White,
                                    containerColor = MidnightSurfaceContainer,
                                ),
                                modifier = Modifier.testTag("sport_chip_$sport"),
                            )
                        }
                    }
                }

                when (eventsState) {
                    is UiState.Loading -> {
                        item(key = "events_loading") {
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
                        item(key = "events_error") {
                            EmptyOrErrorStateCard(
                                icon = Icons.Default.CloudOff,
                                title = "Events Sync Unavailable",
                                message = eventsState.message,
                            )
                        }
                    }

                    is UiState.Success -> {
                        val list = eventsState.data
                        if (list.isEmpty()) {
                            val isFiltered = searchQuery.isNotBlank() || selectedSport != "All"
                            item(key = "events_empty") {
                                EmptyOrErrorStateCard(
                                    icon = Icons.Default.Sports,
                                    title = if (isFiltered) "No Matching Live Events" else "No Live Events Scheduled",
                                    message = if (isFiltered) {
                                        "Try clearing your search or switching sport filters."
                                    } else {
                                        "Live match links published from Admin Studio appear here simultaneously on all devices."
                                    },
                                    actionLabel = if (isFiltered) "Reset Filters" else null,
                                    onAction = if (isFiltered) {
                                        {
                                            onSearchQueryChange("")
                                            onSelectSport("All")
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
                                    pair.forEach { evt ->
                                        LiveEventCard(
                                            event = evt,
                                            isPlaying = activeEvent?.id == evt.id,
                                            onPlay = { onPlayEvent(evt) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(list, key = { it.id }) { evt ->
                                LiveEventCard(
                                    event = evt,
                                    isPlaying = activeEvent?.id == evt.id,
                                    onPlay = { onPlayEvent(evt) },
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

@Composable
private fun LiveEventCard(
    event: LiveEvent,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isPlaying -> ElectricCyan
        event.status == "LIVE" -> LiveCrimson.copy(alpha = 0.55f)
        else -> BorderSubtle.copy(alpha = 0.7f)
    }

    val statusColor = when (event.status) {
        "LIVE" -> LiveCrimson
        "UPCOMING" -> AmberGlow
        else -> Color(0xFF7B8499)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, borderColor, MaterialTheme.shapes.large)
            .clickable(onClick = onPlay)
            .testTag("live_event_card_${event.id}"),
        color = if (isPlaying) MidnightSurfaceHigh else MidnightSurfaceContainer,
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (isPlaying) 4.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top badge row: LIVE / UPCOMING + Sport + Server badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (event.status == "LIVE") {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(LiveCrimson)
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = event.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Surface(
                        color = ElectricCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = event.sport.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                if (event.backupUrl.isNotBlank()) {
                    Surface(
                        color = EmeraldLive.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "2 SERVERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLive,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            // Event info + poster/badge + Watch button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelLogoBadge(
                    channelName = event.title,
                    logoUrl = event.thumbnail,
                    size = 54.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (event.teams.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = event.teams,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) ElectricCyan else LiveCrimson,
                        contentColor = if (isPlaying) Color(0xFF001F26) else Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("watch_event_button_${event.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "Playing" else "Watch",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
