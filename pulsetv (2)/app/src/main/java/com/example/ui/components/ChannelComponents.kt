package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.model.BannerNotice
import com.example.model.Channel
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MidnightSurfaceContainer
import com.example.ui.theme.MidnightSurfaceHigh
import com.example.util.BannerImageUtils

@Composable
fun ChannelLogoBadge(
    channelName: String,
    logoUrl: String,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier,
) {
    val initials = channelName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "TV" }

    val fallbackContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            ElectricCyan.copy(alpha = 0.28f),
                            LiveCrimson.copy(alpha = 0.28f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightSurfaceHigh)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (logoUrl.isBlank()) {
            fallbackContent()
        } else {
            SubcomposeAsyncImage(
                model = logoUrl,
                contentDescription = "$channelName logo",
                contentScale = ContentScale.Crop,
                loading = { fallbackContent() },
                error = { fallbackContent() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun HeroBannerCard(
    liveChannelCount: Int,
    featuredChannel: Channel?,
    onWatchFeatured: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MidnightSurfaceContainer,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE607090F),
                                Color(0x9907090F),
                                Color(0x4D07090F),
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = LiveCrimson.copy(alpha = 0.92f),
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
                                text = "$liveChannelCount LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }

                    Surface(
                        color = ElectricCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "INSTANT CLOUD SYNC",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Column {
                    Text(
                        text = featuredChannel?.name ?: "Live Broadcasts Ready",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (featuredChannel != null) {
                            "${featuredChannel.category} • ${featuredChannel.language} — Tap to stream in HD"
                        } else {
                            "Guest Mode active. Long-press PulseTV logo to add live channels in Admin."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (featuredChannel != null) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onWatchFeatured(featuredChannel) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF001F26),
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("hero_watch_button"),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Watch Live",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeBannerCarousel(
    banners: List<BannerNotice>,
    onBannerClick: (BannerNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notice_banner_carousel"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            pageSpacing = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val banner = banners[page]
            val imageModel = androidx.compose.runtime.remember(banner.imageData) {
                BannerImageUtils.toCoilModel(banner.imageData)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), MaterialTheme.shapes.large)
                    .clickable { onBannerClick(banner) }
                    .testTag("notice_banner_card_${banner.id}"),
                shape = MaterialTheme.shapes.large,
                color = MidnightSurfaceContainer,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = imageModel,
                        contentDescription = banner.title,
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MidnightSurfaceHigh)
                            )
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MidnightSurfaceHigh, ElectricCyan.copy(alpha = 0.25f))
                                        )
                                    )
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x5507090F),
                                        Color(0xCC07090F),
                                        Color(0xF007090F),
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(
                                color = AmberGlow.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = Color(0xFF1A1200),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = "NOTICE BANNER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1A1200),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (banner.actionUrl.isNotBlank()) {
                                Surface(
                                    color = ElectricCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(
                                            text = "LINK",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyan,
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Text(
                                text = banner.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (banner.message.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = banner.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (banners.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(banners.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (selected) 16.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) ElectricCyan else BorderSubtle)
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelListCard(
    channel: Channel,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isPlaying) ElectricCyan else BorderSubtle.copy(alpha = 0.65f)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, borderColor, MaterialTheme.shapes.medium)
            .clickable(onClick = onPlay)
            .testTag("channel_card_${channel.id}"),
        color = if (isPlaying) MidnightSurfaceHigh else MidnightSurfaceContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isPlaying) 4.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelLogoBadge(
                channelName = channel.name,
                logoUrl = channel.logo,
                size = 52.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isPlaying) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldLive)
                    )
                    val meta = buildString {
                        append(channel.category)
                        append(" • ")
                        append(channel.language)
                        if (channel.country.isNotBlank()) {
                            append(" • ")
                            append(channel.country)
                        }
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (channel.backupUrl.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "2x",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("fav_toggle_${channel.id}"),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite_toggle),
                    tint = if (isFavorite) LiveCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = "Play ${channel.name}",
                tint = if (isPlaying) ElectricCyan else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun RecentChannelChipCard(
    channel: Channel,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(156.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = if (isPlaying) ElectricCyan else BorderSubtle,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .testTag("recent_chip_${channel.id}"),
        color = MidnightSurfaceContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelLogoBadge(
                channelName = channel.name,
                logoUrl = channel.logo,
                size = 38.dp,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
