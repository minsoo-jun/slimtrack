package com.minsoo.slimtrack.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.minsoo.slimtrack.domain.AdviceStrictness
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

data class GeminiModelOption(
    val id: String,
    val titleKo: String,
    val badgeKo: String,
    val descKo: String
)

object Gemini3Catalog {
    const val DEFAULT_MODEL_ID = "gemini-3-flash-preview"

    val presetModels: List<GeminiModelOption> = listOf(
        GeminiModelOption(
            id = "gemini-3-flash-preview",
            titleKo = "Gemini 3 Flash (기본 추천)",
            badgeKo = "3.0 Flash",
            descKo = "빠른 식사 사진/InBody OCR 분석 및 코칭"
        ),
        GeminiModelOption(
            id = "gemini-3.1-pro-preview",
            titleKo = "Gemini 3.1 Pro",
            badgeKo = "3.1 Pro",
            descKo = "고정밀 음식 영양소 추론 및 심층 코칭"
        ),
        GeminiModelOption(
            id = "gemini-3-pro-preview",
            titleKo = "Gemini 3.0 Pro",
            badgeKo = "3.0 Pro",
            descKo = "멀티모달 정밀 이미지 분석"
        ),
        GeminiModelOption(
            id = "gemini-3.1-flash-lite-preview",
            titleKo = "Gemini 3.1 Flash Lite",
            badgeKo = "3.1 Lite",
            descKo = "초고속·저지연 응답"
        ),
        GeminiModelOption(
            id = "gemini-3.8-flash",
            titleKo = "Gemini 3.8 Flash",
            badgeKo = "3.8 Flash",
            descKo = "최신 3.x 세대 고속 모델"
        )
    )

    /**
     * Migrates any legacy 1.x / 2.x model name (e.g. gemini-2.5-flash) to Gemini 3.x default.
     */
    fun ensureGemini3Model(rawModel: String?): String {
        val clean = rawModel?.trim()?.removePrefix("models/").orEmpty()
        if (clean.isBlank() || clean.startsWith("gemini-2") || clean.startsWith("gemini-1")) {
            return DEFAULT_MODEL_ID
        }
        return clean
    }
}

data class AppSettings(
    val geminiModel: String = Gemini3Catalog.DEFAULT_MODEL_ID,
    val toleranceKcal: Int = 150,
    val strictness: AdviceStrictness = AdviceStrictness.STRICT,
    val sundayMorningAlarmEnabled: Boolean = true,
    val missingMeal21AlarmEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val TOLERANCE_KCAL = intPreferencesKey("tolerance_kcal")
        val STRICTNESS = stringPreferencesKey("strictness")
        val SUNDAY_ALARM = booleanPreferencesKey("sunday_morning_alarm")
        val MISSING_MEAL_ALARM = booleanPreferencesKey("missing_meal_21_alarm")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val strictness = prefs[Keys.STRICTNESS]?.let {
            runCatching { AdviceStrictness.valueOf(it) }.getOrNull()
        } ?: AdviceStrictness.STRICT
        val resolvedModel = Gemini3Catalog.ensureGemini3Model(prefs[Keys.GEMINI_MODEL])
        AppSettings(
            geminiModel = resolvedModel,
            toleranceKcal = prefs[Keys.TOLERANCE_KCAL] ?: 150,
            strictness = strictness,
            sundayMorningAlarmEnabled = prefs[Keys.SUNDAY_ALARM] ?: true,
            missingMeal21AlarmEnabled = prefs[Keys.MISSING_MEAL_ALARM] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.GEMINI_MODEL] = Gemini3Catalog.ensureGemini3Model(settings.geminiModel)
            prefs[Keys.TOLERANCE_KCAL] = settings.toleranceKcal
            prefs[Keys.STRICTNESS] = settings.strictness.name
            prefs[Keys.SUNDAY_ALARM] = settings.sundayMorningAlarmEnabled
            prefs[Keys.MISSING_MEAL_ALARM] = settings.missingMeal21AlarmEnabled
            prefs[Keys.ONBOARDING_DONE] = settings.onboardingCompleted
        }
    }
}
