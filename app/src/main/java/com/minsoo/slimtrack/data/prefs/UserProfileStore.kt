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
    val goalStartDate: LocalDate = LocalDate.now()
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

/**
 * Dual-backed persistent profile store (DataStore + synchronous SharedPreferences backup)
 * so user profile edits are immediately flushed to disk and never lost across restarts or APK updates.
 */
class UserProfileStore(private val context: Context) {
    private object Keys {
        val SEX = stringPreferencesKey("sex")
        val HEIGHT_CM = doublePreferencesKey("height_cm")
        val BIRTH_DATE = stringPreferencesKey("birth_date")
        val ACTIVITY_FACTOR = doublePreferencesKey("activity_factor")
        val MUSCLE_BASELINE = doublePreferencesKey("muscle_baseline_kg")
        val GOAL_START_DATE = stringPreferencesKey("goal_start_date")
    }

    private val syncPrefs = context.getSharedPreferences("slimtrack_profile_backup", Context.MODE_PRIVATE)

    fun readBackupProfile(): UserProfile {
        val sexStr = syncPrefs.getString("sex", null)
        val sex = sexStr?.let { runCatching { Sex.valueOf(it) }.getOrNull() } ?: Sex.MALE
        val height = syncPrefs.getString("height_cm", null)?.toDoubleOrNull() ?: 176.0
        val birth = syncPrefs.getString("birth_date", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.of(1980, 1, 1)
        val activity = syncPrefs.getString("activity_factor", null)?.toDoubleOrNull() ?: 1.2
        val muscle = syncPrefs.getString("muscle_baseline_kg", null)?.toDoubleOrNull() ?: 38.5
        val start = syncPrefs.getString("goal_start_date", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()
        return UserProfile(
            sex = sex,
            heightCm = height,
            birthDate = birth,
            activityFactor = activity,
            muscleBaselineKg = muscle,
            goalStartDate = start
        )
    }

    private fun writeBackupProfile(profile: UserProfile) {
        syncPrefs.edit()
            .putString("sex", profile.sex.name)
            .putString("height_cm", profile.heightCm.toString())
            .putString("birth_date", profile.birthDate.toString())
            .putString("activity_factor", profile.activityFactor.toString())
            .putString("muscle_baseline_kg", profile.muscleBaselineKg.toString())
            .putString("goal_start_date", profile.goalStartDate.toString())
            .commit()
    }

    val profileFlow: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        val backup = readBackupProfile()
        val sex = prefs[Keys.SEX]?.let { runCatching { Sex.valueOf(it) }.getOrNull() } ?: backup.sex
        val height = prefs[Keys.HEIGHT_CM] ?: backup.heightCm
        val birth = prefs[Keys.BIRTH_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: backup.birthDate
        val activity = prefs[Keys.ACTIVITY_FACTOR] ?: backup.activityFactor
        val muscle = prefs[Keys.MUSCLE_BASELINE] ?: backup.muscleBaselineKg
        val start = prefs[Keys.GOAL_START_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: backup.goalStartDate

        val resolved = UserProfile(
            sex = sex,
            heightCm = height,
            birthDate = birth,
            activityFactor = activity,
            muscleBaselineKg = muscle,
            goalStartDate = start
        )
        writeBackupProfile(resolved)
        resolved
    }

    suspend fun saveProfile(profile: UserProfile) {
        // 1. Synchronous flush to SharedPreferences so it cannot be lost even if process exits immediately
        writeBackupProfile(profile)
        // 2. Persist to DataStore for reactive Flow updates
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
