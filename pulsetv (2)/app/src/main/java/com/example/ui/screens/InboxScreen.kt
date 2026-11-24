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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.UiState
import com.example.model.UpdateNotice
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MidnightSurfaceContainer
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun InboxScreen(
    updatesState: UiState<List<UpdateNotice>>,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(updatesState) {
        if (updatesState is UiState.Success) {
            onMarkRead()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Update Inbox",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Live announcements & channel updates synced from Firebase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (updatesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            }

            is UiState.Error -> {
                EmptyOrErrorStateCard(
                    icon = Icons.Default.CloudOff,
                    title = "Could Not Load Notices",
                    message = updatesState.message,
                )
            }

            is UiState.Success -> {
                val list = updatesState.data
                if (list.isEmpty()) {
                    EmptyOrErrorStateCard(
                        icon = Icons.Default.Campaign,
                        title = "Inbox Clear",
                        message = "Broadcast notices posted from the Admin Panel appear here instantly for all viewers.",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(list, key = { it.id }) { notice ->
                            UpdateNoticeCard(notice = notice)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateNoticeCard(notice: UpdateNotice) {
    val formattedDate = notice.timestamp?.toDate()?.let { date ->
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
    } ?: "Just now"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = MidnightSurfaceContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = AmberGlow.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = notice.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGlow,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = notice.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
