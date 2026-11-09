package com.minsoo.slimtrack.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.Period
import kotlin.math.pow
import kotlin.math.roundToInt

private val Context.profileDataStore by preferencesDataStore(name = "user_profile")

enum class Sex(val labelKo: String) {
    MALE("남성"),
    FEMALE("여성")
}

data class UserProfile(
    val sex: Sex = Sex.MALE,
    val heightCm: Double = 176.0,
    val birthDate: LocalDate = LocalDate.of(1980, 1, 1),
    val activityFactor: Double = 1.2,
    val muscleBaselineKg: Double = 38.5,
    val goalStartDate: LocalDate = LocalDate.of(2026, 11, 9)
) {
    fun ageOn(date: LocalDate): Int =
        Period.between(birthDate, date).years.coerceIn(10, 110)

    fun bmi(weightKg: Double): Double {
        val hMeters = (heightCm / 100.0).coerceAtLeast(1.0)
        return ((weightKg / hMeters.pow(2)) * 10.0).roundToInt() / 10.0
    }

    fun zone2HrRange(date: LocalDate): IntRange {
        val maxHr = (220 - ageOn(date)).coerceAtLeast(120)
        val low = (maxHr * 0.60).roundToInt()
        val high = (maxHr * 0.70).roundToInt()
        return low..high
    }
}

class UserProfileStore(private val context: Context) {
    private object Keys {
        val SEX = stringPreferencesKey("sex")
        val HEIGHT_CM = doublePreferencesKey("height_cm")
        val BIRTH_DATE = stringPreferencesKey("birth_date")
        val ACTIVITY_FACTOR = doublePreferencesKey("activity_factor")
        val MUSCLE_BASELINE = doublePreferencesKey("muscle_baseline_kg")
        val GOAL_START_DATE = stringPreferencesKey("goal_start_date")
    }

    val profileFlow: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        val sex = prefs[Keys.SEX]?.let { runCatching { Sex.valueOf(it) }.getOrNull() } ?: Sex.MALE
        val height = prefs[Keys.HEIGHT_CM] ?: 176.0
        val birth = prefs[Keys.BIRTH_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.of(1980, 1, 1)
        val activity = prefs[Keys.ACTIVITY_FACTOR] ?: 1.2
        val muscle = prefs[Keys.MUSCLE_BASELINE] ?: 38.5
        val start = prefs[Keys.GOAL_START_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.of(2026, 11, 9)
        UserProfile(
            sex = sex,
            heightCm = height,
            birthDate = birth,
            activityFactor = activity,
            muscleBaselineKg = muscle,
            goalStartDate = start
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.SEX] = profile.sex.name
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.BIRTH_DATE] = profile.birthDate.toString()
            prefs[Keys.ACTIVITY_FACTOR] = profile.activityFactor
            prefs[Keys.MUSCLE_BASELINE] = profile.muscleBaselineKg
            prefs[Keys.GOAL_START_DATE] = profile.goalStartDate.toString()
        }
    }
}
