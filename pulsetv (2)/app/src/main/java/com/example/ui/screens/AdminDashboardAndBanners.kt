package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.model.AppAnalyticsSummary
import com.example.model.BannerNotice
import com.example.model.BrokenReport
import com.example.model.Channel
import com.example.model.LiveEvent
import com.example.model.UiState
import com.example.model.UpdateNotice
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.MidnightSurfaceContainer
import com.example.ui.theme.MidnightSurfaceHigh
import com.example.util.BannerImageUtils

@Composable
fun AdminDashboardTab(
    analyticsState: UiState<AppAnalyticsSummary>,
    channels: List<Channel>,
    liveEvents: List<LiveEvent>,
    banners: List<BannerNotice>,
    notices: List<UpdateNotice>,
    reports: List<BrokenReport>,
) {
    val activeChannels = remember(channels) { channels.count { it.isActive } }
    val activeEvents = remember(liveEvents) { liveEvents.count { it.isActive } }
    val activeBanners = remember(banners) { banners.count { it.isActive } }

    val countryBreakdown = remember(channels) {
        channels.groupBy { it.country.trim().ifBlank { "Global" } }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(6)
    }

    val categoryBreakdown = remember(channels) {
        channels.groupBy { it.category.trim().ifBlank { "Entertainment" } }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(6)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "analytics_header") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricCyan.copy(alpha = 0.28f), MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large,
                color = MidnightSurfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Analytics Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Live Cloud Firestore metrics across all viewer phones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        when (analyticsState) {
            is UiState.Loading -> {
                item(key = "analytics_loading") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                }
            }

            is UiState.Error -> {
                item(key = "analytics_error") {
                    EmptyOrErrorStateCard(
                        icon = Icons.Default.Analytics,
                        title = "Telemetry Unavailable",
                        message = analyticsState.message,
                    )
                }
            }

            is UiState.Success -> {
                val metrics = analyticsState.data
                item(key = "kpi_telemetry_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DashboardKpiCard(
                            label = "App Opens",
                            value = metrics.appOpens.toString(),
                            icon = Icons.Default.Visibility,
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            label = "Channel Plays",
                            value = metrics.channelPlays.toString(),
                            icon = Icons.Default.PlayCircle,
                            accent = EmeraldLive,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardKpiCard(
                            label = "Event Plays",
                            value = metrics.eventPlays.toString(),
                            icon = Icons.Default.Bolt,
                            accent = AmberGlow,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item(key = "kpi_inventory_row_1") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DashboardKpiCard(
                    label = "Active Channels",
                    value = "$activeChannels / ${channels.size}",
                    icon = Icons.Default.LiveTv,
                    accent = ElectricCyan,
                    modifier = Modifier.weight(1f),
                )
                DashboardKpiCard(
                    label = "Live Events",
                    value = "$activeEvents / ${liveEvents.size}",
                    icon = Icons.Default.Bolt,
                    accent = LiveCrimson,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "kpi_inventory_row_2") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DashboardKpiCard(
                    label = "Notice Banners",
                    value = "$activeBanners / ${banners.size}",
                    icon = Icons.Default.Campaign,
                    accent = AmberGlow,
                    modifier = Modifier.weight(1f),
                )
                DashboardKpiCard(
                    label = "Broken Reports",
                    value = "${reports.size} (${notices.size} Inbox)",
                    icon = Icons.Default.ReportProblem,
                    accent = if (reports.isNotEmpty()) LiveCrimson else EmeraldLive,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (countryBreakdown.isNotEmpty()) {
            item(key = "country_breakdown") {
                DistributionCard(
                    title = "Channels by Country",
                    icon = Icons.Default.Public,
                    entries = countryBreakdown.map { it.key to it.value },
                    total = channels.size.coerceAtLeast(1),
                    barColor = ElectricCyan,
                )
            }
        }

        if (categoryBreakdown.isNotEmpty()) {
            item(key = "category_breakdown") {
                DistributionCard(
                    title = "Channels by Category",
                    icon = Icons.Default.LiveTv,
                    entries = categoryBreakdown.map { it.key to it.value },
                    total = channels.size.coerceAtLeast(1),
                    barColor = EmeraldLive,
                )
            }
        }
    }
}

@Composable
private fun DashboardKpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = MidnightSurfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DistributionCard(
    title: String,
    icon: ImageVector,
    entries: List<Pair<String, Int>>,
    total: Int,
    barColor: Color,
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = barColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            entries.forEach { (name, count) ->
                val fraction = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(name, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        color = barColor,
                        trackColor = MidnightSurfaceHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

@Composable
fun AdminBannersTab(
    banners: List<BannerNotice>,
    onAddClick: () -> Unit,
    onEditClick: (BannerNotice) -> Unit,
    onToggleActive: (BannerNotice) -> Unit,
    onDeleteClick: (BannerNotice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "banner_actions") {
            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_add_banner_button"),
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Upload Notice Banner")
            }
        }

        if (banners.isEmpty()) {
            item(key = "admin_banners_empty") {
                EmptyOrErrorStateCard(
                    icon = Icons.Default.AddPhotoAlternate,
                    title = "No Notice Banners Uploaded",
                    message = "Upload an image banner and notice headline here. It appears live on every viewer's Home screen.",
                )
            }
        } else {
            items(banners, key = { it.id }) { banner ->
                val model = remember(banner.imageData) { BannerImageUtils.toCoilModel(banner.imageData) }
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
                        SubcomposeAsyncImage(
                            model = model,
                            contentDescription = banner.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 72.dp, height = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MidnightSurfaceHigh),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = banner.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (banner.message.isNotBlank()) {
                                Text(
                                    text = banner.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = if (banner.isActive) "Visible on Home" else "Hidden",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (banner.isActive) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = banner.isActive,
                            onCheckedChange = { onToggleActive(banner) },
                            modifier = Modifier.testTag("admin_toggle_banner_${banner.id}"),
                        )
                        IconButton(
                            onClick = { onEditClick(banner) },
                            modifier = Modifier.testTag("admin_edit_banner_${banner.id}"),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit banner")
                        }
                        IconButton(
                            onClick = { onDeleteClick(banner) },
                            modifier = Modifier.testTag("admin_delete_banner_${banner.id}"),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete banner", tint = LiveCrimson)
                        }
                    }
                }
            }
        }
    }
}
