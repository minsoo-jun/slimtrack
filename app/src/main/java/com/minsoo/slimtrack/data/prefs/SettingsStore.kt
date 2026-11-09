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

data class AppSettings(
    val geminiModel: String = "gemini-2.5-flash",
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
        AppSettings(
            geminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-2.5-flash",
            toleranceKcal = prefs[Keys.TOLERANCE_KCAL] ?: 150,
            strictness = strictness,
            sundayMorningAlarmEnabled = prefs[Keys.SUNDAY_ALARM] ?: true,
            missingMeal21AlarmEnabled = prefs[Keys.MISSING_MEAL_ALARM] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false
        )
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.GEMINI_MODEL] = settings.geminiModel
            prefs[Keys.TOLERANCE_KCAL] = settings.toleranceKcal
            prefs[Keys.STRICTNESS] = settings.strictness.name
            prefs[Keys.SUNDAY_ALARM] = settings.sundayMorningAlarmEnabled
            prefs[Keys.MISSING_MEAL_ALARM] = settings.missingMeal21AlarmEnabled
            prefs[Keys.ONBOARDING_DONE] = settings.onboardingCompleted
        }
    }
}
