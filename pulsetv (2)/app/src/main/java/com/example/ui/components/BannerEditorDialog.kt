package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.model.BannerNotice
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MidnightSurfaceHigh
import com.example.util.BannerImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BannerEditorDialog(
    initialBanner: BannerNotice?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        message: String,
        imageData: String,
        actionUrl: String,
        isActive: Boolean,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember(initialBanner) { mutableStateOf(initialBanner?.title ?: "") }
    var message by remember(initialBanner) { mutableStateOf(initialBanner?.message ?: "") }
    var imageData by remember(initialBanner) { mutableStateOf(initialBanner?.imageData ?: "") }
    var imageUrlInput by remember(initialBanner) {
        val initial = initialBanner?.imageData ?: ""
        mutableStateOf(if (initial.startsWith("http", ignoreCase = true)) initial else "")
    }
    var actionUrl by remember(initialBanner) { mutableStateOf(initialBanner?.actionUrl ?: "") }
    var isActive by remember(initialBanner) { mutableStateOf(initialBanner?.isActive ?: true) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingImage = true
            validationError = null
            scope.launch {
                val encodedResult = withContext(Dispatchers.IO) {
                    BannerImageUtils.encodeGalleryImageToDataUri(context, uri)
                }
                isProcessingImage = false
                encodedResult.onSuccess { dataUri ->
                    imageData = dataUri
                    imageUrlInput = ""
                }.onFailure { err ->
                    validationError = err.localizedMessage ?: "Could not process selected banner image."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBanner == null) "Upload Notice Banner" else "Edit Notice Banner") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightSurfaceHigh)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isProcessingImage -> CircularProgressIndicator(color = ElectricCyan)
                        imageData.isNotBlank() -> {
                            val model = remember(imageData) { BannerImageUtils.toCoilModel(imageData) }
                            SubcomposeAsyncImage(
                                model = model,
                                contentDescription = "Banner preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Pick a banner image or paste an image URL",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessingImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_pick_image_button"),
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageData.startsWith("data:")) "Change Uploaded Banner Image" else "Upload Banner From Gallery")
                }

                OutlinedTextField(
                    value = imageUrlInput,
                    onValueChange = { url ->
                        if (url.length <= 1000) {
                            imageUrlInput = url
                            if (url.isNotBlank()) {
                                imageData = url.trim()
                            }
                        }
                    },
                    label = { Text("Or Banner Image URL (https://...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_image_url_input"),
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Notice Headline *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_title_input"),
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 400) message = it },
                    label = { Text("Notice Details (Optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_message_input"),
                )

                OutlinedTextField(
                    value = actionUrl,
                    onValueChange = { if (it.length <= 1000) actionUrl = it },
                    label = { Text("Optional Tap Link (https://...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_action_url_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Show Banner on Home Screen", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("banner_active_switch"),
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
                    val cleanImage = imageData.trim()
                    val cleanAction = actionUrl.trim()
                    when {
                        title.isBlank() -> validationError = "Notice headline is required."
                        cleanImage.length < 10 -> validationError = "Please upload a banner image or paste an image URL."
                        cleanAction.isNotEmpty() && !cleanAction.startsWith("http://") && !cleanAction.startsWith("https://") ->
                            validationError = "Tap link must start with http:// or https://"
                        else -> {
                            validationError = null
                            onSave(
                                title.trim(),
                                message.trim(),
                                cleanImage,
                                cleanAction,
                                isActive,
                            )
                        }
                    }
                },
                enabled = !isProcessingImage,
                modifier = Modifier.testTag("banner_save_button"),
            ) {
                Text(if (initialBanner == null) "Publish Banner" else "Save Banner")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
