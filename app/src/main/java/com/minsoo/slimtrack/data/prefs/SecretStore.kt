package com.minsoo.slimtrack.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecretStore(context: Context) {
    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "slimtrack_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        // Fallback in JVM / Robolectric tests where AndroidKeyStore is not mounted
        context.getSharedPreferences("slimtrack_secure_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val _apiKeyFlow = MutableStateFlow(getGeminiApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    fun getGeminiApiKey(): String = prefs.getString(KEY_GEMINI_API, "")?.trim().orEmpty()

    fun setGeminiApiKey(key: String) {
        val clean = key.trim()
        prefs.edit().putString(KEY_GEMINI_API, clean).apply()
        _apiKeyFlow.value = clean
    }

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
    }
}
