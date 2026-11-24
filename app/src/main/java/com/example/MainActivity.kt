package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MomoTab
import com.example.ui.screens.AccountPlansScreen
import com.example.ui.screens.AuthCheckingSplash
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BotScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FullScreenImagePreviewDialog
import com.example.ui.screens.GalleryScreen
import com.example.ui.theme.LocalMomoExtraColors
import com.example.ui.theme.MomoTheme
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.MomoViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MomoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (uiState.isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (uiState.isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }

            MomoTheme(darkTheme = uiState.isDarkTheme) {
                MomoAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun MomoAppRoot(viewModel: MomoViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val extra = LocalMomoExtraColors.current

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Native Android Back Navigation handling
    BackHandler(
        enabled = uiState.authState == AuthUiState.Authenticated &&
            (uiState.previewImage != null || uiState.activeQrisSession != null || uiState.currentTab != MomoTab.BOT)
    ) {
        when {
            uiState.previewImage != null -> viewModel.closeImagePreview()
            uiState.activeQrisSession != null -> viewModel.closeQrisModal()
            uiState.currentTab != MomoTab.BOT -> viewModel.selectTab(MomoTab.BOT)
        }
    }

    when (uiState.authState) {
        AuthUiState.Checking -> {
            AuthCheckingSplash()
        }
        AuthUiState.Unauthenticated -> {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AuthScreen(
                        isDarkTheme = uiState.isDarkTheme,
                        isBusy = uiState.isAuthBusy,
                        errorMessage = uiState.authError,
                        onToggleTheme = viewModel::toggleTheme,
                        onLogin = viewModel::login,
                        onRegister = viewModel::register,
                        onClearError = viewModel::clearAuthError
                    )
                }
            }
        }
        AuthUiState.Authenticated -> {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Box {
                        HorizontalDivider(color = extra.cardBorder, thickness = 1.dp)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            val navColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            NavigationBarItem(
                                selected = uiState.currentTab == MomoTab.BOT,
                                onClick = { viewModel.selectTab(MomoTab.BOT) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == MomoTab.BOT) Icons.Rounded.SmartToy else Icons.Outlined.SmartToy,
                                        contentDescription = stringResource(R.string.nav_bot)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_bot)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_tab_bot")
                            )
                            NavigationBarItem(
                                selected = uiState.currentTab == MomoTab.CHAT,
                                onClick = { viewModel.selectTab(MomoTab.CHAT) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == MomoTab.CHAT) Icons.Rounded.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = stringResource(R.string.nav_chat)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_chat)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_tab_chat")
                            )
                            NavigationBarItem(
                                selected = uiState.currentTab == MomoTab.GALERI,
                                onClick = { viewModel.selectTab(MomoTab.GALERI) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == MomoTab.GALERI) Icons.Rounded.Collections else Icons.Outlined.Collections,
                                        contentDescription = stringResource(R.string.nav_galeri)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_galeri)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_tab_galeri")
                            )
                            NavigationBarItem(
                                selected = uiState.currentTab == MomoTab.AKUN,
                                onClick = { viewModel.selectTab(MomoTab.AKUN) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == MomoTab.AKUN) Icons.Rounded.AccountCircle else Icons.Outlined.AccountCircle,
                                        contentDescription = stringResource(R.string.nav_akun)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_akun)) },
                                colors = navColors,
                                modifier = Modifier.testTag("nav_tab_akun")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (uiState.currentTab) {
                        MomoTab.BOT -> BotScreen(
                            uiState = uiState,
                            onRefresh = viewModel::refreshBots,
                            onToggleTheme = viewModel::toggleTheme,
                            onOpenBot = viewModel::openBotInChat,
                            onCreateBot = viewModel::createBot,
                            onUpdateBot = viewModel::updateBot,
                            onDeleteBot = viewModel::deleteBot
                        )
                        MomoTab.CHAT -> {
                            val activeModelId = uiState.activeModel?.id
                            ChatScreen(
                                uiState = uiState,
                                imageLoader = viewModel.imageLoader,
                                currentImageSize = viewModel.currentImageSizeForModel(activeModelId),
                                onBackToBots = { viewModel.selectTab(MomoTab.BOT) },
                                onNewSession = viewModel::clearCurrentChatSession,
                                onSelectModel = viewModel::selectModel,
                                onSelectEffort = viewModel::setReasoningEffort,
                                onSelectImageSize = viewModel::setImageSizeForCurrentModel,
                                onAddPhoto = viewModel::addPhotoAttachment,
                                onAddFile = viewModel::addFileAttachment,
                                onAddNote = viewModel::addNoteAttachment,
                                onRemoveAttachment = viewModel::removeAttachment,
                                onSendMessage = viewModel::sendMessage,
                                onStopStreaming = viewModel::stopStreaming,
                                onPreviewInlineImage = viewModel::openInlineImagePreview,
                                onDownloadInlineImage = { url ->
                                    viewModel.downloadImageToAndroid(url = url, prefix = "momo_studio")
                                },
                                onGoToPlans = {
                                    viewModel.dismissRateLimitBanner()
                                    viewModel.selectTab(MomoTab.AKUN)
                                }
                            )
                        }
                        MomoTab.GALERI -> GalleryScreen(
                            uiState = uiState,
                            imageLoader = viewModel.imageLoader,
                            onRefresh = viewModel::refreshGallery,
                            onSelectImage = viewModel::openImagePreview,
                            onDownloadImage = { item ->
                                viewModel.downloadImageToAndroid(url = item.url, prefix = "momo_galeri")
                            }
                        )
                        MomoTab.AKUN -> AccountPlansScreen(
                            uiState = uiState,
                            imageLoader = viewModel.imageLoader,
                            onRefresh = {
                                viewModel.refreshProfile()
                                viewModel.refreshPlans()
                            },
                            onToggleTheme = viewModel::toggleTheme,
                            onUpgradeQris = viewModel::startQrisCheckout,
                            onDowngradeFree = viewModel::changePlanFree,
                            onSaveQrImage = { session ->
                                viewModel.downloadImageToAndroid(
                                    url = session.qrImageUrl,
                                    dataUrl = session.qrDataUrl,
                                    prefix = "momo_qris_${session.orderId.ifBlank { "pay" }}"
                                )
                            },
                            onCloseQris = viewModel::closeQrisModal,
                            onLogout = viewModel::logout
                        )
                    }
                }
            }
        }
    }

    uiState.previewImage?.let { item ->
        FullScreenImagePreviewDialog(
            item = item,
            imageLoader = viewModel.imageLoader,
            isDownloading = uiState.isDownloadingImage,
            onDismiss = viewModel::closeImagePreview,
            onDownload = { viewModel.downloadImageToAndroid(url = item.url, prefix = "momo_image") },
            onShare = {
                viewModel.shareImageToAndroid(
                    url = item.url,
                    caption = item.prompt.ifBlank { "Karya dari Momo AI" }
                )
            }
        )
    }
}
