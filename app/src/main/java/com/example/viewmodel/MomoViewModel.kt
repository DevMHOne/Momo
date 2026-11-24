package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.example.data.AndroidMediaHelper
import com.example.data.AttachmentKind
import com.example.data.ChatAttachment
import com.example.data.ChatMessage
import com.example.data.GeneratedImageItem
import com.example.data.ImageSizeOption
import com.example.data.MomoApiClient
import com.example.data.MomoBot
import com.example.data.MomoModel
import com.example.data.MomoPlan
import com.example.data.MomoPreferences
import com.example.data.MomoTab
import com.example.data.MomoUser
import com.example.data.QrisCheckoutSession
import com.example.data.QuotaFormatters
import com.example.data.RateLimitInfo
import com.example.data.RateLimitedException
import com.example.data.ReasoningEffort
import com.example.data.SseEvent
import com.example.data.UnauthorizedException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AuthUiState {
    data object Checking : AuthUiState
    data object Unauthenticated : AuthUiState
    data object Authenticated : AuthUiState
}

data class MomoUiState(
    val authState: AuthUiState = AuthUiState.Checking,
    val isDarkTheme: Boolean = true,
    val currentTab: MomoTab = MomoTab.BOT,
    val user: MomoUser? = null,
    val isAuthBusy: Boolean = false,
    val authError: String? = null,

    // Bots
    val bots: List<MomoBot> = emptyList(),
    val isBotsLoading: Boolean = false,
    val isBotSaving: Boolean = false,
    val selectedBotId: String? = null,

    // Models & Settings
    val models: List<MomoModel> = emptyList(),
    val defaultModelId: String? = null,
    val selectedChatModelId: String? = null,
    val selectedImageModelId: String? = null,
    val reasoningEffort: ReasoningEffort = ReasoningEffort.MEDIUM,
    val imageSizesByModelKey: Map<String, ImageSizeOption> = emptyMap(),

    // Chat / Studio
    val messagesByBotId: Map<String, List<ChatMessage>> = emptyMap(),
    val sessionTitlesByBotId: Map<String, String> = emptyMap(),
    val attachments: List<ChatAttachment> = emptyList(),
    val isStreaming: Boolean = false,
    val rateLimitInfo: RateLimitInfo? = null,

    // Gallery
    val galleryImages: List<GeneratedImageItem> = emptyList(),
    val isGalleryLoading: Boolean = false,
    val previewImage: GeneratedImageItem? = null,
    val isDownloadingImage: Boolean = false,

    // Plans & QRIS
    val plans: List<MomoPlan> = emptyList(),
    val isPlansLoading: Boolean = false,
    val isPlanActionBusy: Boolean = false,
    val activeQrisSession: QrisCheckoutSession? = null,
    val isPollingQris: Boolean = false,

    // Clock for live countdowns
    val nowMs: Long = System.currentTimeMillis()
) {
    val selectedBot: MomoBot?
        get() = bots.firstOrNull { it.id == selectedBotId } ?: bots.firstOrNull()

    val chatModels: List<MomoModel>
        get() = models.filter { it.isChat }

    val imageModels: List<MomoModel>
        get() = models.filter { it.isImage }

    val activeModel: MomoModel?
        get() {
            val bot = selectedBot
            return if (bot?.isImageStudio == true) {
                imageModels.firstOrNull { it.id == selectedImageModelId } ?: imageModels.firstOrNull()
            } else {
                chatModels.firstOrNull { it.id == selectedChatModelId }
                    ?: chatModels.firstOrNull { it.id == defaultModelId }
                    ?: chatModels.firstOrNull()
            }
        }
}

class MomoViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = MomoPreferences(application.applicationContext)
    val apiClient = MomoApiClient(application.applicationContext)
    val imageLoader: ImageLoader get() = apiClient.imageLoader

    private val _uiState = MutableStateFlow(MomoUiState())
    val uiState: StateFlow<MomoUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var streamJob: Job? = null
    private var qrisPollingJob: Job? = null

    init {
        observePreferences()
        startCountdownTicker()
        bootstrapSession()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.isDarkThemeFlow.collect { isDark ->
                _uiState.update { it.copy(isDarkTheme = isDark) }
            }
        }
        viewModelScope.launch {
            preferences.selectedChatModelFlow.collect { id ->
                if (!id.isNullOrBlank()) {
                    _uiState.update { it.copy(selectedChatModelId = id) }
                }
            }
        }
        viewModelScope.launch {
            preferences.selectedImageModelFlow.collect { id ->
                if (!id.isNullOrBlank()) {
                    _uiState.update { it.copy(selectedImageModelId = id) }
                }
            }
        }
        viewModelScope.launch {
            preferences.reasoningEffortFlow.collect { effort ->
                _uiState.update { it.copy(reasoningEffort = effort) }
            }
        }
        viewModelScope.launch {
            preferences.allImageSizesFlow.collect { map ->
                _uiState.update { it.copy(imageSizesByModelKey = map) }
            }
        }
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                _uiState.update { it.copy(nowMs = System.currentTimeMillis()) }
            }
        }
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthUiState.Checking) }
            val savedToken = preferences.getToken()
            if (!savedToken.isNullOrBlank()) {
                apiClient.setToken(savedToken)
            }
            try {
                val me = apiClient.getMe()
                if (me != null) {
                    _uiState.update {
                        it.copy(
                            authState = AuthUiState.Authenticated,
                            user = me,
                            authError = null
                        )
                    }
                    loadWorkspaceData()
                } else {
                    preferences.clearToken()
                    apiClient.clearSession()
                    _uiState.update { it.copy(authState = AuthUiState.Unauthenticated, user = null) }
                }
            } catch (_: Exception) {
                // Keep token if offline only? Better to fall back to unauthenticated if token missing or invalid
                if (savedToken.isNullOrBlank()) {
                    _uiState.update { it.copy(authState = AuthUiState.Unauthenticated, user = null) }
                } else {
                    _uiState.update {
                        it.copy(
                            authState = AuthUiState.Unauthenticated,
                            authError = "Tidak dapat terhubung ke server Momo AI. Periksa koneksi internet Anda."
                        )
                    }
                }
            }
        }
    }

    fun toggleTheme() {
        val next = !_uiState.value.isDarkTheme
        viewModelScope.launch {
            preferences.setDarkTheme(next)
        }
    }

    fun selectTab(tab: MomoTab) {
        _uiState.update { it.copy(currentTab = tab) }
        when (tab) {
            MomoTab.GALERI -> if (_uiState.value.galleryImages.isEmpty()) refreshGallery()
            MomoTab.AKUN -> {
                refreshProfile()
                if (_uiState.value.plans.isEmpty()) refreshPlans()
            }
            MomoTab.BOT -> if (_uiState.value.bots.isEmpty()) refreshBots()
            MomoTab.CHAT -> Unit
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Email dan kata sandi wajib diisi.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true, authError = null) }
            try {
                val res = apiClient.login(email, password)
                if (res.token.isNotBlank()) {
                    preferences.saveToken(res.token)
                }
                _uiState.update {
                    it.copy(
                        isAuthBusy = false,
                        authState = AuthUiState.Authenticated,
                        user = res.user,
                        authError = null,
                        currentTab = MomoTab.BOT
                    )
                }
                emitMessage("Selamat datang kembali, ${res.user.name}!")
                loadWorkspaceData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAuthBusy = false,
                        authError = e.message ?: "Gagal masuk. Periksa email dan kata sandi."
                    )
                }
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            _uiState.update {
                it.copy(authError = "Isi nama lengkap, email valid, dan kata sandi minimal 6 karakter.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true, authError = null) }
            try {
                val res = apiClient.register(name, email, password)
                if (res.token.isNotBlank()) {
                    preferences.saveToken(res.token)
                }
                _uiState.update {
                    it.copy(
                        isAuthBusy = false,
                        authState = AuthUiState.Authenticated,
                        user = res.user,
                        authError = null,
                        currentTab = MomoTab.BOT
                    )
                }
                emitMessage("Akun berhasil dibuat! Selamat datang, ${res.user.name}.")
                loadWorkspaceData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAuthBusy = false,
                        authError = e.message ?: "Pendaftaran gagal. Coba lagi."
                    )
                }
            }
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun logout() {
        streamJob?.cancel()
        qrisPollingJob?.cancel()
        viewModelScope.launch {
            apiClient.logout()
            preferences.clearToken()
            _uiState.update { state ->
                MomoUiState(
                    authState = AuthUiState.Unauthenticated,
                    isDarkTheme = state.isDarkTheme
                )
            }
            emitMessage("Anda telah keluar dari akun.")
        }
    }

    private fun handleUnauthorized(message: String) {
        streamJob?.cancel()
        qrisPollingJob?.cancel()
        viewModelScope.launch {
            preferences.clearToken()
            apiClient.clearSession()
            _uiState.update { state ->
                MomoUiState(
                    authState = AuthUiState.Unauthenticated,
                    isDarkTheme = state.isDarkTheme,
                    authError = message
                )
            }
            emitMessage(message)
        }
    }

    fun loadWorkspaceData() {
        viewModelScope.launch {
            val botsDeferred = async { runCatching { apiClient.getBots() } }
            val modelsDeferred = async { runCatching { apiClient.getModels() } }
            val galleryDeferred = async { runCatching { apiClient.getGeneratedImages() } }
            val currentUser = _uiState.value.user
            val plansDeferred = async {
                runCatching {
                    apiClient.getPlans(
                        currentPlanId = currentUser?.quota?.planId.orEmpty(),
                        currentPlanName = currentUser?.quota?.planName.orEmpty()
                    )
                }
            }

            val botsRes = botsDeferred.await()
            val modelsRes = modelsDeferred.await()
            val galleryRes = galleryDeferred.await()
            val plansRes = plansDeferred.await()

            listOf(botsRes, modelsRes, galleryRes, plansRes).forEach { r ->
                val ex = r.exceptionOrNull()
                if (ex is UnauthorizedException) {
                    handleUnauthorized(ex.message ?: "Sesi berakhir")
                    return@launch
                }
            }

            val bots = botsRes.getOrElse { _uiState.value.bots }
            val modelsPayload = modelsRes.getOrNull()
            val models = modelsPayload?.models ?: _uiState.value.models
            val defaultModelId = modelsPayload?.defaultModelId ?: _uiState.value.defaultModelId
            val gallery = galleryRes.getOrElse { _uiState.value.galleryImages }
            val plans = plansRes.getOrElse { _uiState.value.plans }

            _uiState.update { state ->
                val chatModels = models.filter { it.isChat }
                val imageModels = models.filter { it.isImage }
                val validChatModelId = state.selectedChatModelId?.takeIf { id -> chatModels.any { it.id == id } }
                    ?: defaultModelId?.takeIf { id -> chatModels.any { it.id == id } }
                    ?: chatModels.firstOrNull()?.id
                val validImageModelId = state.selectedImageModelId?.takeIf { id -> imageModels.any { it.id == id } }
                    ?: imageModels.firstOrNull()?.id
                val validBotId = state.selectedBotId?.takeIf { id -> bots.any { it.id == id } }
                    ?: bots.firstOrNull()?.id

                state.copy(
                    bots = bots,
                    selectedBotId = validBotId,
                    models = models,
                    defaultModelId = defaultModelId,
                    selectedChatModelId = validChatModelId,
                    selectedImageModelId = validImageModelId,
                    galleryImages = gallery,
                    plans = plans
                )
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val me = apiClient.getMe()
                if (me == null) {
                    handleUnauthorized("Sesi berakhir. Silakan masuk kembali.")
                } else {
                    _uiState.update { it.copy(user = me, nowMs = System.currentTimeMillis()) }
                }
            } catch (e: UnauthorizedException) {
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (_: Exception) {
                // Keep last known profile silently on transient refresh error
            }
        }
    }

    fun refreshBots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBotsLoading = true) }
            try {
                val bots = apiClient.getBots()
                _uiState.update { state ->
                    val validBotId = state.selectedBotId?.takeIf { id -> bots.any { it.id == id } }
                        ?: bots.firstOrNull()?.id
                    state.copy(bots = bots, selectedBotId = validBotId, isBotsLoading = false)
                }
                refreshProfile()
            } catch (e: UnauthorizedException) {
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (e: Exception) {
                _uiState.update { it.copy(isBotsLoading = false) }
                emitMessage(e.message ?: "Gagal memuat daftar bot")
            }
        }
    }

    fun createBot(name: String, look: String, job: String, onSuccess: () -> Unit = {}) {
        if (name.isBlank()) {
            emitMessage("Nama bot tidak boleh kosong.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBotSaving = true) }
            try {
                val created = apiClient.createBot(name, look, job)
                _uiState.update { state ->
                    state.copy(
                        bots = listOf(created) + state.bots.filterNot { it.id == created.id },
                        selectedBotId = created.id,
                        isBotSaving = false
                    )
                }
                refreshProfile()
                emitMessage("Bot \"${created.name}\" berhasil dibuat.")
                onSuccess()
            } catch (e: UnauthorizedException) {
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (e: Exception) {
                _uiState.update { it.copy(isBotSaving = false) }
                emitMessage(e.message ?: "Gagal membuat bot")
            }
        }
    }

    fun updateBot(bot: MomoBot, newName: String, newJob: String, onSuccess: () -> Unit = {}) {
        if (newName.isBlank()) {
            emitMessage("Nama bot tidak boleh kosong.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBotSaving = true) }
            try {
                // Look is strictly locked after creation!
                val updated = apiClient.updateBot(bot.id, newName, bot.look, newJob)
                _uiState.update { state ->
                    state.copy(
                        bots = state.bots.map { if (it.id == updated.id) updated else it },
                        isBotSaving = false
                    )
                }
                emitMessage("Perubahan bot \"${updated.name}\" disimpan.")
                onSuccess()
            } catch (e: UnauthorizedException) {
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (e: Exception) {
                _uiState.update { it.copy(isBotSaving = false) }
                emitMessage(e.message ?: "Gagal memperbarui bot")
            }
        }
    }

    fun deleteBot(bot: MomoBot) {
        viewModelScope.launch {
            try {
                apiClient.deleteBot(bot.id)
                _uiState.update { state ->
                    val remaining = state.bots.filterNot { it.id == bot.id }
                    val nextSelected = if (state.selectedBotId == bot.id) {
                        remaining.firstOrNull()?.id
                    } else {
                        state.selectedBotId
                    }
                    state.copy(
                        bots = remaining,
                        selectedBotId = nextSelected,
                        messagesByBotId = state.messagesByBotId - bot.id,
                        sessionTitlesByBotId = state.sessionTitlesByBotId - bot.id
                    )
                }
                refreshProfile()
                emitMessage("Bot \"${bot.name}\" dihapus.")
            } catch (e: UnauthorizedException) {
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (e: Exception) {
                emitMessage(e.message ?: "Gagal menghapus bot")
            }
        }
    }

    fun openBotInChat(bot: MomoBot) {
        _uiState.update { state ->
            val filteredAttachments = if (bot.isImageStudio) {
                val activeImgModel = state.imageModels.firstOrNull { it.id == state.selectedImageModelId }
                    ?: state.imageModels.firstOrNull()
                if (activeImgModel?.supportsImageEdit == true) {
                    state.attachments.filter { it.kind == AttachmentKind.PHOTO }
                } else {
                    emptyList()
                }
            } else {
                state.attachments
            }
            state.copy(
                selectedBotId = bot.id,
                currentTab = MomoTab.CHAT,
                attachments = filteredAttachments
            )
        }
    }

    fun selectModel(model: MomoModel) {
        viewModelScope.launch {
            if (model.isImage) {
                preferences.setSelectedImageModel(model.id)
                _uiState.update { state ->
                    val cleanedAttachments = if (model.supportsImageEdit) {
                        state.attachments.filter { it.kind == AttachmentKind.PHOTO }
                    } else {
                        emptyList()
                    }
                    state.copy(selectedImageModelId = model.id, attachments = cleanedAttachments)
                }
            } else {
                preferences.setSelectedChatModel(model.id)
                _uiState.update { it.copy(selectedChatModelId = model.id) }
            }
        }
    }

    fun setReasoningEffort(effort: ReasoningEffort) {
        viewModelScope.launch {
            preferences.setReasoningEffort(effort)
        }
    }

    fun currentImageSizeForModel(modelId: String?): ImageSizeOption {
        if (modelId.isNullOrBlank()) return ImageSizeOption.SQUARE
        val key = preferences.imageSizeKeyForModel(modelId)
        return _uiState.value.imageSizesByModelKey[key] ?: ImageSizeOption.SQUARE
    }

    fun setImageSizeForCurrentModel(size: ImageSizeOption) {
        val modelId = _uiState.value.activeModel?.id ?: return
        viewModelScope.launch {
            preferences.setImageSizeForModel(modelId, size)
        }
    }

    fun addPhotoAttachment(uri: Uri) {
        viewModelScope.launch {
            val res = AndroidMediaHelper.readUriAsAttachment(getApplication(), uri, AttachmentKind.PHOTO)
            res.onSuccess { att ->
                _uiState.update { it.copy(attachments = it.attachments + att) }
            }.onFailure { err ->
                emitMessage(err.message ?: "Gagal memuat foto")
            }
        }
    }

    fun addFileAttachment(uri: Uri) {
        viewModelScope.launch {
            val res = AndroidMediaHelper.readUriAsAttachment(getApplication(), uri, AttachmentKind.FILE)
            res.onSuccess { att ->
                _uiState.update { it.copy(attachments = it.attachments + att) }
            }.onFailure { err ->
                emitMessage(err.message ?: "Gagal memuat berkas")
            }
        }
    }

    fun addNoteAttachment(title: String, content: String) {
        if (content.isBlank()) return
        val note = ChatAttachment(
            id = UUID.randomUUID().toString(),
            kind = AttachmentKind.NOTE,
            name = title.ifBlank { "Catatan" }.trim(),
            mimeType = "text/plain",
            textContent = content.trim()
        )
        _uiState.update { it.copy(attachments = it.attachments + note) }
    }

    fun removeAttachment(id: String) {
        _uiState.update { it.copy(attachments = it.attachments.filterNot { att -> att.id == id }) }
    }

    fun clearCurrentChatSession() {
        val botId = _uiState.value.selectedBot?.id ?: return
        stopStreaming()
        _uiState.update { state ->
            state.copy(
                messagesByBotId = state.messagesByBotId - botId,
                sessionTitlesByBotId = state.sessionTitlesByBotId - botId,
                attachments = emptyList(),
                rateLimitInfo = null
            )
        }
        emitMessage("Percakapan baru dimulai.")
    }

    fun dismissRateLimitBanner() {
        _uiState.update { it.copy(rateLimitInfo = null) }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        val botId = _uiState.value.selectedBot?.id
        _uiState.update { state ->
            val updatedMap = if (botId != null) {
                val list = state.messagesByBotId[botId].orEmpty().map { msg ->
                    if (msg.isStreaming) msg.copy(isStreaming = false, activityLabel = null) else msg
                }
                state.messagesByBotId + (botId to list)
            } else {
                state.messagesByBotId
            }
            state.copy(isStreaming = false, messagesByBotId = updatedMap)
        }
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        val state = _uiState.value
        val bot = state.selectedBot
        if (bot == null) {
            emitMessage("Pilih atau buat bot terlebih dahulu.")
            return
        }
        if (text.isEmpty() && state.attachments.isEmpty()) return
        if (state.isStreaming) return

        val model = state.activeModel
        if (model == null) {
            emitMessage("Daftar model belum tersedia. Muat ulang halaman.")
            return
        }

        val currentHistory = state.messagesByBotId[bot.id].orEmpty()
        val needsTitle = currentHistory.isEmpty()
        val sendingAttachments = state.attachments
        val effort = state.reasoningEffort
        val imageSize = currentImageSizeForModel(model.id)

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = text.ifEmpty { "Lampiran dikirim" },
            attachmentLabels = sendingAttachments.map { it.name }
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantMsgId,
            role = "assistant",
            text = "",
            reasoningEffort = effort,
            activityLabel = if (bot.isImageStudio) "Melukis gambar…" else "Menghubungkan ke Momo…",
            isStreaming = true
        )

        val updatedHistory = currentHistory + userMsg
        _uiState.update { s ->
            s.copy(
                messagesByBotId = s.messagesByBotId + (bot.id to (updatedHistory + assistantPlaceholder)),
                attachments = emptyList(),
                isStreaming = true,
                rateLimitInfo = null
            )
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            try {
                apiClient.streamBrain(
                    messages = updatedHistory,
                    modelId = model.id,
                    effort = effort,
                    bot = bot,
                    needsTitle = needsTitle,
                    imageSize = imageSize,
                    attachments = sendingAttachments
                ).collect { event ->
                    when (event) {
                        is SseEvent.TurnMeta -> Unit
                        is SseEvent.ActivityUpdate -> {
                            updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                                msg.copy(activityLabel = event.label)
                            }
                        }
                        is SseEvent.ReasoningDelta -> {
                            updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                                msg.copy(reasoning = msg.reasoning + event.delta)
                            }
                        }
                        is SseEvent.PartStart -> {
                            if (!event.imageUrl.isNullOrBlank()) {
                                updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                                    val urls = (msg.imageUrls + event.imageUrl).distinct()
                                    msg.copy(imageUrls = urls)
                                }
                            }
                        }
                        is SseEvent.TextDelta -> {
                            updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                                msg.copy(text = msg.text + event.delta, activityLabel = null)
                            }
                        }
                        is SseEvent.PartEnd -> {
                            if (!event.imageUrl.isNullOrBlank()) {
                                updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                                    val urls = (msg.imageUrls + event.imageUrl).distinct()
                                    msg.copy(imageUrls = urls, activityLabel = null)
                                }
                            }
                        }
                        is SseEvent.SessionTitle -> {
                            _uiState.update { s ->
                                s.copy(sessionTitlesByBotId = s.sessionTitlesByBotId + (bot.id to event.title))
                            }
                        }
                        is SseEvent.TurnError -> {
                            updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                                msg.copy(
                                    text = if (msg.text.isBlank()) event.message else "${msg.text}\n\n_${event.message}_",
                                    isStreaming = false,
                                    isError = true,
                                    activityLabel = null
                                )
                            }
                        }
                        is SseEvent.TurnDone -> {
                            updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                                msg.copy(isStreaming = false, activityLabel = null)
                            }
                            refreshProfile()
                            if (bot.isImageStudio) {
                                refreshGallery()
                            }
                        }
                    }
                }
                // Stream closed cleanly
                updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                    msg.copy(isStreaming = false, activityLabel = null)
                }
                _uiState.update { it.copy(isStreaming = false) }
                refreshProfile()
                if (bot.isImageStudio) {
                    refreshGallery()
                }
            } catch (e: UnauthorizedException) {
                _uiState.update { it.copy(isStreaming = false) }
                handleUnauthorized(e.message ?: "Sesi login berakhir")
            } catch (e: RateLimitedException) {
                val quota = _uiState.value.user?.quota
                val fallbackReset = e.resetAt ?: when (e.reason ?: quota?.chatBlockedReason) {
                    "weekly" -> quota?.chatWeeklyResetAt
                    else -> quota?.chat5hResetAt
                }
                updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                    msg.copy(
                        text = e.message ?: "Kuota sementara tercapai.",
                        isStreaming = false,
                        isError = true,
                        activityLabel = null
                    )
                }
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        rateLimitInfo = RateLimitInfo(
                            message = e.message ?: "Kuota sementara tercapai.",
                            resetAt = fallbackReset,
                            reason = e.reason
                        )
                    )
                }
                refreshProfile()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                updateAssistantMessage(bot.id, assistantMsgId) { msg ->
                    msg.copy(
                        text = if (msg.text.isBlank()) (e.message ?: "Koneksi terputus saat streaming.") else msg.text,
                        isStreaming = false,
                        isError = msg.text.isBlank(),
                        activityLabel = null
                    )
                }
                _uiState.update { it.copy(isStreaming = false) }
            }
        }
    }

    private fun updateAssistantMessage(
        botId: String,
        messageId: String,
        transform: (ChatMessage) -> ChatMessage
    ) {
        _uiState.update { state ->
            val current = state.messagesByBotId[botId].orEmpty()
            val updated = current.map { if (it.id == messageId) transform(it) else it }
            state.copy(messagesByBotId = state.messagesByBotId + (botId to updated))
        }
    }

    fun refreshGallery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGalleryLoading = true) }
            try {
                val items = apiClient.getGeneratedImages()
                _uiState.update { it.copy(galleryImages = items, isGalleryLoading = false) }
            } catch (e: UnauthorizedException) {
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (e: Exception) {
                _uiState.update { it.copy(isGalleryLoading = false) }
                emitMessage(e.message ?: "Gagal memuat galeri")
            }
        }
    }

    fun openImagePreview(item: GeneratedImageItem) {
        _uiState.update { it.copy(previewImage = item) }
    }

    fun openInlineImagePreview(url: String, prompt: String = "") {
        openImagePreview(
            GeneratedImageItem(
                id = url.hashCode().toString(),
                url = QuotaFormatters.resolveMomoUrl(url),
                prompt = prompt,
                model = _uiState.value.activeModel?.label.orEmpty(),
                size = currentImageSizeForModel(_uiState.value.activeModel?.id).apiValue
            )
        )
    }

    fun closeImagePreview() {
        _uiState.update { it.copy(previewImage = null) }
    }

    fun downloadImageToAndroid(url: String?, dataUrl: String? = null, prefix: String = "momo_ai") {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingImage = true) }
            val result = AndroidMediaHelper.saveDataUrlOrRemoteImage(
                context = getApplication(),
                apiClient = apiClient,
                imageUrl = url,
                dataUrl = dataUrl,
                fileNamePrefix = prefix
            )
            _uiState.update { it.copy(isDownloadingImage = false) }
            result.onSuccess { msg ->
                emitMessage(msg)
            }.onFailure { err ->
                emitMessage(err.message ?: "Gagal menyimpan gambar ke HP Android")
                if (!url.isNullOrBlank()) {
                    AndroidMediaHelper.openDownloadFallbackLink(getApplication(), url)
                }
            }
        }
    }

    fun shareImageToAndroid(url: String, caption: String = "Karya dari Momo AI") {
        viewModelScope.launch {
            val result = AndroidMediaHelper.shareRemoteImage(
                context = getApplication(),
                apiClient = apiClient,
                imageUrl = url,
                caption = caption
            )
            result.onFailure { err ->
                emitMessage(err.message ?: "Gagal membagikan gambar")
            }
        }
    }

    fun refreshPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlansLoading = true) }
            try {
                val quota = _uiState.value.user?.quota
                val plans = apiClient.getPlans(
                    currentPlanId = quota?.planId.orEmpty(),
                    currentPlanName = quota?.planName.orEmpty()
                )
                _uiState.update { it.copy(plans = plans, isPlansLoading = false) }
            } catch (e: UnauthorizedException) {
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (e: Exception) {
                _uiState.update { it.copy(isPlansLoading = false) }
                emitMessage(e.message ?: "Gagal memuat daftar paket")
            }
        }
    }

    fun startQrisCheckout(plan: MomoPlan) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlanActionBusy = true) }
            try {
                val session = apiClient.checkoutQris(plan.id, plan.name, plan.priceIdr)
                _uiState.update {
                    it.copy(
                        isPlanActionBusy = false,
                        activeQrisSession = session,
                        isPollingQris = true
                    )
                }
                startQrisPolling(session.orderId)
            } catch (e: UnauthorizedException) {
                _uiState.update { it.copy(isPlanActionBusy = false) }
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (e: Exception) {
                _uiState.update { it.copy(isPlanActionBusy = false) }
                emitMessage(e.message ?: "Gagal membuat tagihan QRIS")
            }
        }
    }

    private fun startQrisPolling(orderId: String) {
        if (orderId.isBlank()) return
        qrisPollingJob?.cancel()
        qrisPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(3_000L)
                try {
                    val status = apiClient.getBillingStatus(orderId)
                    _uiState.update { state ->
                        val updated = state.activeQrisSession?.copy(status = status)
                        state.copy(activeQrisSession = updated)
                    }
                    if (status == "paid" || status == "settlement" || status == "completed" || status == "success") {
                        _uiState.update { it.copy(isPollingQris = false) }
                        emitMessage("Pembayaran QRIS berhasil! Paket Anda telah diperbarui.")
                        refreshProfile()
                        refreshPlans()
                        break
                    } else if (status == "expired" || status == "failed" || status == "cancelled") {
                        _uiState.update { it.copy(isPollingQris = false) }
                        emitMessage("Transaksi QRIS berakhir ($status).")
                        break
                    }
                } catch (e: UnauthorizedException) {
                    _uiState.update { it.copy(isPollingQris = false) }
                    handleUnauthorized(e.message ?: "Sesi berakhir")
                    break
                } catch (_: Exception) {
                    // Continue polling on transient error
                }
            }
        }
    }

    fun closeQrisModal() {
        qrisPollingJob?.cancel()
        qrisPollingJob = null
        _uiState.update { it.copy(activeQrisSession = null, isPollingQris = false) }
    }

    fun changePlanFree(plan: MomoPlan) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlanActionBusy = true) }
            try {
                apiClient.changePlan(plan.id)
                _uiState.update { it.copy(isPlanActionBusy = false) }
                emitMessage("Paket berhasil diubah ke ${plan.name}.")
                refreshProfile()
                refreshPlans()
            } catch (e: UnauthorizedException) {
                _uiState.update { it.copy(isPlanActionBusy = false) }
                handleUnauthorized(e.message ?: "Sesi berakhir")
            } catch (e: Exception) {
                _uiState.update { it.copy(isPlanActionBusy = false) }
                emitMessage(e.message ?: "Gagal mengubah paket")
            }
        }
    }

    private fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }
}
