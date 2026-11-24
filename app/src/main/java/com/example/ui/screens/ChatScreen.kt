package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AttachmentKind
import com.example.data.ChatAttachment
import com.example.data.ChatMessage
import com.example.data.ImageSizeOption
import com.example.data.MomoBot
import com.example.data.MomoModel
import com.example.data.QuotaFormatters
import com.example.data.ReasoningEffort
import com.example.ui.components.BotLookAvatar
import com.example.ui.components.GeneratedInlineImageCard
import com.example.ui.components.LockedLookBadge
import com.example.ui.components.MomoMarkdownContent
import com.example.ui.components.RateLimitAlertCard
import com.example.ui.components.ReasoningCollapsiblePanel
import com.example.ui.theme.LocalMomoExtraColors
import com.example.viewmodel.MomoUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: MomoUiState,
    imageLoader: ImageLoader,
    currentImageSize: ImageSizeOption,
    onBackToBots: () -> Unit,
    onNewSession: () -> Unit,
    onSelectModel: (MomoModel) -> Unit,
    onSelectEffort: (ReasoningEffort) -> Unit,
    onSelectImageSize: (ImageSizeOption) -> Unit,
    onAddPhoto: (Uri) -> Unit,
    onAddFile: (Uri) -> Unit,
    onAddNote: (title: String, content: String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onStopStreaming: () -> Unit,
    onPreviewInlineImage: (String, String) -> Unit,
    onDownloadInlineImage: (String) -> Unit,
    onGoToPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalMomoExtraColors.current
    val bot = uiState.selectedBot
    val activeModel = uiState.activeModel
    val isStudio = bot?.isImageStudio == true
    val messages = if (bot != null) uiState.messagesByBotId[bot.id].orEmpty() else emptyList()
    val sessionTitle = if (bot != null) uiState.sessionTitlesByBotId[bot.id] else null

    var inputText by rememberSaveable(bot?.id) { mutableStateOf("") }
    var showModelSheet by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAddPhoto(uri)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAddFile(uri)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length, messages.lastOrNull()?.reasoning?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Sticky Header (Bot type is LOCKED - never render a look dropdown next to bot name)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBackToBots,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (bot != null) {
                        BotLookAvatar(look = bot.look, size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = bot.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                LockedLookBadge(look = bot.look, showLockIcon = false)
                            }
                            Text(
                                text = sessionTitle?.takeIf { it.isNotBlank() }
                                    ?: if (isStudio) stringResource(R.string.chat_mode_studio) else stringResource(R.string.chat_mode_chat),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "Momo Chat",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (bot != null) {
                        IconButton(
                            onClick = onNewSession,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_new_session_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.chat_new_session_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Mode-Specific Controls Strip
                if (bot != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Model Picker Pill
                            Surface(
                                onClick = { showModelSheet = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, extra.cardBorder),
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("chat_model_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isStudio) Icons.Rounded.Palette else Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = activeModel?.label ?: "Pilih Model",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ExpandMore,
                                        contentDescription = "Ubah model",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (isStudio) {
                                if (activeModel != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "${activeModel.creditCost} kuota/gbr",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                ImageSizeOption.entries.forEach { sizeOption ->
                                    val selected = sizeOption == currentImageSize
                                    ControlChip(
                                        label = sizeOption.shortLabel,
                                        selected = selected,
                                        onClick = { onSelectImageSize(sizeOption) },
                                        testTag = "size_chip_${sizeOption.apiValue}"
                                    )
                                }
                            } else {
                                ReasoningEffort.entries.forEach { effort ->
                                    val selected = effort == uiState.reasoningEffort
                                    ControlChip(
                                        label = effort.label,
                                        selected = selected,
                                        onClick = { onSelectEffort(effort) },
                                        testTag = "effort_chip_${effort.apiValue}"
                                    )
                                }
                            }
                        }

                        if (!isStudio && activeModel != null) {
                            Text(
                                text = QuotaFormatters.formatEffortLegend(activeModel.effortCosts),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                HorizontalDivider(color = extra.cardBorder, thickness = 1.dp)
            }
        }

        if (bot == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extra.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "Belum ada bot terpilih",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pilih atau buat bot di tab Bot untuk memulai percakapan atau Studio Pelukis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onBackToBots,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Buka Daftar Bot")
                        }
                    }
                }
            }
            return@Column
        }

        // Messages Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                ChatEmptyWelcome(
                    bot = bot,
                    onPickStarter = { starter ->
                        inputText = starter
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageRow(
                            message = msg,
                            bot = bot,
                            imageLoader = imageLoader,
                            onPreviewImage = { url -> onPreviewInlineImage(url, msg.text) },
                            onDownloadImage = onDownloadInlineImage
                        )
                    }
                }
            }
        }

        // Rate Limit Card (429)
        uiState.rateLimitInfo?.let { limit ->
            RateLimitAlertCard(
                info = limit,
                nowMs = uiState.nowMs,
                onUpgradeClick = onGoToPlans,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Composer
        val allowAttachments = !isStudio || (activeModel?.supportsImageEdit == true)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.attachments.isNotEmpty() && allowAttachments) {
                    AttachmentStrip(
                        attachments = uiState.attachments,
                        imageLoader = imageLoader,
                        onRemove = onRemoveAttachment
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (allowAttachments) {
                        Box {
                            Surface(
                                onClick = {
                                    if (isStudio) {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    } else {
                                        showAttachMenu = true
                                    }
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, extra.cardBorder),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("chat_attach_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isStudio) Icons.Rounded.AddPhotoAlternate else Icons.Rounded.AttachFile,
                                        contentDescription = stringResource(R.string.chat_attach_cd),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (!isStudio) {
                                DropdownMenu(
                                    expanded = showAttachMenu,
                                    onDismissRequest = { showAttachMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Foto (Galeri)") },
                                        leadingIcon = { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null) },
                                        onClick = {
                                            showAttachMenu = false
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Catatan Teks") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null) },
                                        onClick = {
                                            showAttachMenu = false
                                            showNoteDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Berkas Dokumen") },
                                        leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = null) },
                                        onClick = {
                                            showAttachMenu = false
                                            filePickerLauncher.launch(arrayOf("*/*"))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isStudio) {
                                    stringResource(R.string.studio_input_placeholder)
                                } else {
                                    stringResource(R.string.chat_input_placeholder, bot.name)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = extra.cardBorder,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("chat_message_input")
                    )

                    val canSend = (inputText.isNotBlank() || uiState.attachments.isNotEmpty())
                    Surface(
                        onClick = {
                            if (uiState.isStreaming) {
                                onStopStreaming()
                            } else if (canSend) {
                                val textToSend = inputText
                                inputText = ""
                                onSendMessage(textToSend)
                            }
                        },
                        enabled = uiState.isStreaming || canSend,
                        shape = CircleShape,
                        color = when {
                            uiState.isStreaming -> MaterialTheme.colorScheme.error
                            canSend -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isStreaming) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                                contentDescription = if (uiState.isStreaming) {
                                    stringResource(R.string.chat_stop_cd)
                                } else {
                                    stringResource(R.string.chat_send_cd)
                                },
                                tint = when {
                                    uiState.isStreaming -> MaterialTheme.colorScheme.onError
                                    canSend -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModelSheet) {
        val availableModels = if (isStudio) uiState.imageModels else uiState.chatModels
        ModelPickerBottomSheet(
            models = availableModels,
            selectedModelId = activeModel?.id,
            isStudio = isStudio,
            onSelect = { model ->
                onSelectModel(model)
                showModelSheet = false
            },
            onDismiss = { showModelSheet = false }
        )
    }

    if (showNoteDialog) {
        NoteAttachmentDialog(
            onDismiss = { showNoteDialog = false },
            onConfirm = { title, body ->
                onAddNote(title, body)
                showNoteDialog = false
            }
        )
    }
}

@Composable
private fun ControlChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val extra = LocalMomoExtraColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else extra.cardBorder
        ),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatEmptyWelcome(
    bot: MomoBot,
    onPickStarter: (String) -> Unit
) {
    val extra = LocalMomoExtraColors.current
    val starters = if (bot.isImageStudio) {
        listOf(
            "Ilustrasi kedai kopi hangat di malam hujan gaya sinematik",
            "Poster minimalis produk parfum kaca dengan pencahayaan studio",
            "Karakter kucing astronot lucu di luar angkasa warna pastel"
        )
    } else {
        listOf(
            "Halo ${bot.name}, perkenalkan peranmu secara singkat!",
            "Bantu saya menyusun rencana kerja hari ini dengan rapi.",
            "Berikan 3 ide segar yang relevan dengan keahlianmu."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BotLookAvatar(look = bot.look, size = 64.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = bot.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (bot.job.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bot.job,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            starters.forEach { prompt ->
                Surface(
                    onClick = { onPickStarter(prompt) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extra.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessage,
    bot: MomoBot,
    imageLoader: ImageLoader,
    onPreviewImage: (String) -> Unit,
    onDownloadImage: (String) -> Unit
) {
    val extra = LocalMomoExtraColors.current
    val isUser = message.role == "user"

    if (isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (message.attachmentLabels.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            message.attachmentLabels.forEach { label ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotLookAvatar(look = bot.look, size = 28.dp)
                Text(
                    text = bot.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (message.isStreaming && !message.activityLabel.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(11.dp),
                                strokeWidth = 1.6.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = message.activityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (message.reasoning.isNotBlank()) {
                ReasoningCollapsiblePanel(
                    reasoning = message.reasoning,
                    effort = message.reasoningEffort,
                    isStreaming = message.isStreaming && message.text.isBlank()
                )
            }

            if (message.text.isNotBlank() || message.isStreaming) {
                Surface(
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    color = if (message.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (message.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else extra.cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        if (message.text.isBlank() && message.isStreaming) {
                            Text(
                                text = message.activityLabel ?: "Mengetik…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            MomoMarkdownContent(
                                markdown = message.text,
                                imageLoader = imageLoader,
                                textColor = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                                onImageClick = onPreviewImage,
                                onDownloadImage = onDownloadImage
                            )
                        }
                    }
                }
            }

            message.imageUrls.forEach { imgUrl ->
                GeneratedInlineImageCard(
                    imageUrl = imgUrl,
                    alt = "Gambar buatan ${bot.name}",
                    imageLoader = imageLoader,
                    onPreview = { onPreviewImage(imgUrl) },
                    onDownload = { onDownloadImage(imgUrl) }
                )
            }
        }
    }
}

@Composable
private fun AttachmentStrip(
    attachments: List<ChatAttachment>,
    imageLoader: ImageLoader,
    onRemove: (String) -> Unit
) {
    val extra = LocalMomoExtraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { att ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, extra.cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (att.kind == AttachmentKind.PHOTO && !att.previewUri.isNullOrBlank()) {
                        AsyncImage(
                            model = att.previewUri,
                            imageLoader = imageLoader,
                            contentDescription = att.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Icon(
                            imageVector = if (att.kind == AttachmentKind.NOTE) Icons.AutoMirrored.Rounded.NoteAdd else Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = att.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                    IconButton(
                        onClick = { onRemove(att.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hapus lampiran ${att.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerBottomSheet(
    models: List<MomoModel>,
    selectedModelId: String?,
    isStudio: Boolean,
    onSelect: (MomoModel) -> Unit,
    onDismiss: () -> Unit
) {
    val extra = LocalMomoExtraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isStudio) "Pilih Model Pelukis" else "Pilih Model Chat",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (models.isEmpty()) {
                Text(
                    text = "Belum ada model tersedia. Muat ulang profil Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(models, key = { it.id }) { model ->
                        val isSelected = model.id == selectedModelId
                        Surface(
                            onClick = { onSelect(model) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else extra.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_option_${model.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = model.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (model.brand.isNotBlank()) {
                                            Text(
                                                text = model.brand,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    val cleanDesc = QuotaFormatters.sanitizeNoRawTokens(model.description)
                                    if (cleanDesc.isNotBlank()) {
                                        Text(
                                            text = cleanDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isStudio) {
                                        val editLabel = if (model.supportsImageEdit) "Mendukung referensi foto" else "Teks ke gambar"
                                        Text(
                                            text = "${model.creditCost} kuota/gbr · $editLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = QuotaFormatters.formatEffortLegend(model.effortCosts),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Terpilih",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteAttachmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lampirkan Catatan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul (opsional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Isi catatan") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, body) },
                enabled = body.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Lampirkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
