package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.momoDataStore: DataStore<Preferences> by preferencesDataStore(name = "momo_preferences")

class MomoPreferences(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("momo_token")
        val DARK_THEME = booleanPreferencesKey("momo_dark_theme")
        val SELECTED_CHAT_MODEL = stringPreferencesKey("momo_selected_chat_model")
        val SELECTED_IMAGE_MODEL = stringPreferencesKey("momo_selected_image_model")
        val REASONING_EFFORT = stringPreferencesKey("momo_reasoning_effort")
        fun imageSizeForModel(modelId: String) =
            stringPreferencesKey("momo_img_size_${modelId.replace(Regex("[^a-zA-Z0-9_]"), "_")}")
    }

    val tokenFlow: Flow<String?> = context.momoDataStore.data.map { it[Keys.TOKEN] }

    val isDarkThemeFlow: Flow<Boolean> = context.momoDataStore.data.map { it[Keys.DARK_THEME] ?: true }

    val selectedChatModelFlow: Flow<String?> = context.momoDataStore.data.map { it[Keys.SELECTED_CHAT_MODEL] }

    val selectedImageModelFlow: Flow<String?> = context.momoDataStore.data.map { it[Keys.SELECTED_IMAGE_MODEL] }

    val reasoningEffortFlow: Flow<ReasoningEffort> = context.momoDataStore.data.map {
        ReasoningEffort.fromApi(it[Keys.REASONING_EFFORT])
    }

    val allImageSizesFlow: Flow<Map<String, ImageSizeOption>> = context.momoDataStore.data.map { prefs ->
        buildMap {
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith("momo_img_size_") && value is String) {
                    put(key.name.removePrefix("momo_img_size_"), ImageSizeOption.fromApi(value))
                }
            }
        }
    }

    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun saveToken(token: String) {
        context.momoDataStore.edit { it[Keys.TOKEN] = token }
    }

    suspend fun clearToken() {
        context.momoDataStore.edit { it.remove(Keys.TOKEN) }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.momoDataStore.edit { it[Keys.DARK_THEME] = isDark }
    }

    suspend fun setSelectedChatModel(modelId: String) {
        context.momoDataStore.edit { it[Keys.SELECTED_CHAT_MODEL] = modelId }
    }

    suspend fun setSelectedImageModel(modelId: String) {
        context.momoDataStore.edit { it[Keys.SELECTED_IMAGE_MODEL] = modelId }
    }

    suspend fun setReasoningEffort(effort: ReasoningEffort) {
        context.momoDataStore.edit { it[Keys.REASONING_EFFORT] = effort.apiValue }
    }

    suspend fun setImageSizeForModel(modelId: String, size: ImageSizeOption) {
        context.momoDataStore.edit { it[Keys.imageSizeForModel(modelId)] = size.apiValue }
    }

    fun imageSizeKeyForModel(modelId: String): String =
        modelId.replace(Regex("[^a-zA-Z0-9_]"), "_")
}
