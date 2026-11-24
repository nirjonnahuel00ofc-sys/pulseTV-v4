package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.LiveEvent
import com.example.model.LiveEventDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveEventEditorDialog(
    initialEvent: LiveEvent?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        sport: String,
        teams: String,
        status: String,
        thumbnail: String,
        streamUrl: String,
        backupUrl: String,
        isActive: Boolean,
    ) -> Unit,
) {
    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title ?: "") }
    var sport by remember(initialEvent) { mutableStateOf(initialEvent?.sport ?: "Cricket") }
    var teams by remember(initialEvent) { mutableStateOf(initialEvent?.teams ?: "") }
    var status by remember(initialEvent) { mutableStateOf(initialEvent?.status ?: "LIVE") }
    var thumbnail by remember(initialEvent) { mutableStateOf(initialEvent?.thumbnail ?: "") }
    var streamUrl by remember(initialEvent) { mutableStateOf(initialEvent?.streamUrl ?: "") }
    var backupUrl by remember(initialEvent) { mutableStateOf(initialEvent?.backupUrl ?: "") }
    var isActive by remember(initialEvent) { mutableStateOf(initialEvent?.isActive ?: true) }
    var sportExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val sportOptions = remember {
        LiveEventDefaults.SPORTS.filter { it != "All" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent == null) "Publish Live Event" else "Edit Live Event") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Event / Match Title *") },
                    placeholder = { Text("e.g. IND vs AUS • 1st T20") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_event_title"),
                )

                ExposedDropdownMenuBox(
                    expanded = sportExpanded,
                    onExpandedChange = { sportExpanded = it },
                ) {
                    OutlinedTextField(
                        value = sport,
                        onValueChange = { if (it.length <= 40) sport = it },
                        label = { Text("Sport / Category *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("editor_event_sport"),
                    )
                    ExposedDropdownMenu(
                        expanded = sportExpanded,
                        onDismissRequest = { sportExpanded = false },
                    ) {
                        sportOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    sport = option
                                    sportExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = teams,
                    onValueChange = { if (it.length <= 100) teams = it },
                    label = { Text("League / Teams Subtitle (Optional)") },
                    placeholder = { Text("e.g. T20 World Cup • Server HD") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_event_teams"),
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Event Status", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiveEventDefaults.STATUSES.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st) },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { if (it.length <= 1000) streamUrl = it },
                    label = { Text("Live Stream Link (.m3u8 / .ts / mp4) *") },
                    placeholder = { Text("https://.../live.m3u8") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_event_stream_url"),
                )

                OutlinedTextField(
                    value = backupUrl,
                    onValueChange = { if (it.length <= 1000) backupUrl = it },
                    label = { Text("Backup Server Link (Optional)") },
                    placeholder = { Text("https://.../server2.m3u8") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_event_backup_url"),
                )

                OutlinedTextField(
                    value = thumbnail,
                    onValueChange = { if (it.length <= 500) thumbnail = it },
                    label = { Text("Poster / Thumbnail URL (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_event_thumbnail"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Visible on All Devices", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("editor_event_active"),
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanStream = streamUrl.trim()
                    val cleanBackup = backupUrl.trim()
                    val cleanThumb = thumbnail.trim()
                    when {
                        title.isBlank() -> validationError = "Event title is required."
                        sport.isBlank() -> validationError = "Sport / category is required."
                        !cleanStream.startsWith("http://") && !cleanStream.startsWith("https://") ->
                            validationError = "Live stream link must start with http:// or https://"
                        cleanBackup.isNotEmpty() && !cleanBackup.startsWith("http://") && !cleanBackup.startsWith("https://") ->
                            validationError = "Backup stream link must start with http:// or https://"
                        cleanThumb.isNotEmpty() && !cleanThumb.startsWith("http://") && !cleanThumb.startsWith("https://") ->
                            validationError = "Thumbnail URL must start with http:// or https://"
                        else -> {
                            validationError = null
                            onSave(
                                title.trim(),
                                sport.trim(),
                                teams.trim(),
                                status,
                                cleanThumb,
                                cleanStream,
                                cleanBackup,
                                isActive,
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("editor_save_event_button"),
            ) {
                Text(if (initialEvent == null) "Publish to All Phones" else "Save Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
