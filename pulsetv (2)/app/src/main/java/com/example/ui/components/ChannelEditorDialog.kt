package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.Channel
import com.example.model.ChannelAutoFillCatalog
import com.example.model.ChannelCatalogDefaults
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MidnightSurfaceHigh

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChannelEditorDialog(
    initialChannel: Channel?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: String,
        language: String,
        country: String,
        logo: String,
        primaryUrl: String,
        backupUrl: String,
        isActive: Boolean,
    ) -> Unit,
) {
    var name by remember(initialChannel) { mutableStateOf(initialChannel?.name ?: "") }
    var category by remember(initialChannel) { mutableStateOf(initialChannel?.category ?: "Entertainment") }
    var language by remember(initialChannel) { mutableStateOf(initialChannel?.language ?: "English") }
    var country by remember(initialChannel) { mutableStateOf(initialChannel?.country ?: "Global") }
    var logo by remember(initialChannel) { mutableStateOf(initialChannel?.logo ?: "") }
    var primaryUrl by remember(initialChannel) { mutableStateOf(initialChannel?.primaryUrl ?: "") }
    var backupUrl by remember(initialChannel) { mutableStateOf(initialChannel?.backupUrl ?: "") }
    var isActive by remember(initialChannel) { mutableStateOf(initialChannel?.isActive ?: true) }

    var categoryTouched by remember(initialChannel) { mutableStateOf(initialChannel != null) }
    var languageTouched by remember(initialChannel) { mutableStateOf(initialChannel != null) }
    var countryTouched by remember(initialChannel) { mutableStateOf(initialChannel != null) }
    var logoTouched by remember(initialChannel) { mutableStateOf(initialChannel != null) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val categoryOptions = remember {
        ChannelCatalogDefaults.CATEGORIES.filter { it != "All" }
    }
    val languageOptions = remember { ChannelCatalogDefaults.LANGUAGES }
    val countryOptions = remember { ChannelCatalogDefaults.COUNTRIES }
    val suggestions = remember(name) { ChannelAutoFillCatalog.suggestionsFor(name, limit = 4) }

    fun applyAutoFill(forceAll: Boolean) {
        if (name.isBlank()) return
        val result = ChannelAutoFillCatalog.lookupOrInfer(name)
        if (forceAll && result.matchedPreset) {
            name = result.canonicalName
        }
        if (forceAll || !categoryTouched) category = result.category
        if (forceAll || !languageTouched) language = result.language
        if (forceAll || !countryTouched) country = result.country
        if ((forceAll || !logoTouched) && result.logoUrl.isNotBlank()) {
            logo = result.logoUrl
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialChannel == null) "Add Live Channel" else "Edit Channel") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { input ->
                        if (input.length <= 80) {
                            name = input
                            if (initialChannel == null && input.trim().length >= 3) {
                                val inferred = ChannelAutoFillCatalog.lookupOrInfer(input)
                                if (!categoryTouched) category = inferred.category
                                if (!languageTouched) language = inferred.language
                                if (!countryTouched) country = inferred.country
                                if (!logoTouched && inferred.logoUrl.isNotBlank()) {
                                    logo = inferred.logoUrl
                                }
                            }
                        }
                    },
                    label = { Text("Channel Name *") },
                    placeholder = { Text("Type channel name to auto-fill details") },
                    trailingIcon = {
                        IconButton(
                            onClick = { applyAutoFill(forceAll = true) },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.testTag("editor_autofill_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Auto-fill channel details",
                                tint = if (name.isNotBlank()) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_channel_name"),
                )

                if (suggestions.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        suggestions.forEach { preset ->
                            AssistChip(
                                onClick = {
                                    name = preset.canonicalName
                                    category = preset.category
                                    language = preset.language
                                    country = preset.country
                                    logo = preset.logoUrl
                                    categoryTouched = true
                                    languageTouched = true
                                    countryTouched = true
                                    logoTouched = true
                                },
                                label = { Text("${preset.canonicalName} • ${preset.country}") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = ElectricCyan,
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MidnightSurfaceHigh,
                                ),
                                modifier = Modifier.testTag("autofill_suggestion_${preset.canonicalName}"),
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {
                            if (it.length <= 40) {
                                category = it
                                categoryTouched = true
                            }
                        },
                        label = { Text("Category *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("editor_channel_category"),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                    ) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryTouched = true
                                    categoryDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = languageDropdownExpanded,
                    onExpandedChange = { languageDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = language,
                        onValueChange = {
                            if (it.length <= 40) {
                                language = it
                                languageTouched = true
                            }
                        },
                        label = { Text("Language *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageDropdownExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("editor_channel_language"),
                    )
                    ExposedDropdownMenu(
                        expanded = languageDropdownExpanded,
                        onDismissRequest = { languageDropdownExpanded = false },
                    ) {
                        languageOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    language = option
                                    languageTouched = true
                                    languageDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = countryDropdownExpanded,
                    onExpandedChange = { countryDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = {
                            if (it.length <= 50) {
                                country = it
                                countryTouched = true
                            }
                        },
                        label = { Text("Country / Playlist *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryDropdownExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("editor_channel_country"),
                    )
                    ExposedDropdownMenu(
                        expanded = countryDropdownExpanded,
                        onDismissRequest = { countryDropdownExpanded = false },
                    ) {
                        countryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    country = option
                                    countryTouched = true
                                    countryDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = primaryUrl,
                    onValueChange = { if (it.length <= 1000) primaryUrl = it },
                    label = { Text("Primary Stream URL (.m3u8 / .ts / mp4) *") },
                    placeholder = { Text("https://.../playlist.m3u8") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_channel_primary_url"),
                )

                OutlinedTextField(
                    value = backupUrl,
                    onValueChange = { if (it.length <= 1000) backupUrl = it },
                    label = { Text("Backup Stream URL (Optional)") },
                    placeholder = { Text("https://.../backup.m3u8") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_channel_backup_url"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (logo.isNotBlank()) {
                        ChannelLogoBadge(
                            channelName = name.ifBlank { "TV" },
                            logoUrl = logo,
                            size = 44.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    OutlinedTextField(
                        value = logo,
                        onValueChange = {
                            if (it.length <= 500) {
                                logo = it
                                logoTouched = true
                            }
                        },
                        label = { Text("Logo Image URL (Auto-filled or Paste)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("editor_channel_logo"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Channel Active (Visible to Viewers)", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("editor_channel_active"),
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
                    val cleanPrimary = primaryUrl.trim()
                    val cleanBackup = backupUrl.trim()
                    when {
                        name.isBlank() -> validationError = "Channel name is required."
                        category.isBlank() -> validationError = "Category is required."
                        language.isBlank() -> validationError = "Language is required."
                        country.isBlank() -> validationError = "Country is required."
                        !cleanPrimary.startsWith("http://") && !cleanPrimary.startsWith("https://") ->
                            validationError = "Primary URL must start with http:// or https://"
                        cleanBackup.isNotEmpty() && !cleanBackup.startsWith("http://") && !cleanBackup.startsWith("https://") ->
                            validationError = "Backup URL must start with http:// or https://"
                        else -> {
                            validationError = null
                            onSave(
                                name.trim(),
                                category.trim(),
                                language.trim(),
                                country.trim(),
                                logo.trim(),
                                cleanPrimary,
                                cleanBackup,
                                isActive,
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("editor_save_channel_button"),
            ) {
                Text(if (initialChannel == null) "Publish Live" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
