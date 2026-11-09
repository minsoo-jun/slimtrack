package com.minsoo.slimtrack.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    val goalStartDate: LocalDate = LocalDate.now(),
    val startWeightKg: Double = 98.9,
    // Stage 1 (Short-term: default 93~94kg, Visceral Fat 10Lv, 2 months)
    val stage1WeightMinKg: Double = 93.0,
    val stage1WeightMaxKg: Double = 94.0,
    val stage1VisceralFatMax: Int = 10,
    val stage1Months: Int = 2,
    // Stage 2 (Mid-term: default 85~88kg, Visceral Fat 9Lv, 6 months)
    val stage2WeightMinKg: Double = 85.0,
    val stage2WeightMaxKg: Double = 88.0,
    val stage2VisceralFatMax: Int = 9,
    val stage2Months: Int = 6,
    // Stage 3 (Long-term: default 78~82kg, Visceral Fat 7Lv, 12 months)
    val stage3WeightMinKg: Double = 78.0,
    val stage3WeightMaxKg: Double = 82.0,
    val stage3VisceralFatMax: Int = 7,
    val stage3Months: Int = 12
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

    fun withDefaultGoals(): UserProfile = copy(
        startWeightKg = 98.9,
        stage1WeightMinKg = 93.0,
        stage1WeightMaxKg = 94.0,
        stage1VisceralFatMax = 10,
        stage1Months = 2,
        stage2WeightMinKg = 85.0,
        stage2WeightMaxKg = 88.0,
        stage2VisceralFatMax = 9,
        stage2Months = 6,
        stage3WeightMinKg = 78.0,
        stage3WeightMaxKg = 82.0,
        stage3VisceralFatMax = 7,
        stage3Months = 12
    )
}

/**
 * Dual-backed persistent profile store (DataStore + synchronous SharedPreferences backup)
 * so user profile and custom 3-stage diet goals are never lost across restarts or APK updates.
 */
class UserProfileStore(private val context: Context) {
    private object Keys {
        val SEX = stringPreferencesKey("sex")
        val HEIGHT_CM = doublePreferencesKey("height_cm")
        val BIRTH_DATE = stringPreferencesKey("birth_date")
        val ACTIVITY_FACTOR = doublePreferencesKey("activity_factor")
        val MUSCLE_BASELINE = doublePreferencesKey("muscle_baseline_kg")
        val GOAL_START_DATE = stringPreferencesKey("goal_start_date")
        val START_WEIGHT_KG = doublePreferencesKey("start_weight_kg")

        val S1_MIN = doublePreferencesKey("stage1_weight_min_kg")
        val S1_MAX = doublePreferencesKey("stage1_weight_max_kg")
        val S1_VF = intPreferencesKey("stage1_visceral_fat_max")
        val S1_MONTHS = intPreferencesKey("stage1_months")

        val S2_MIN = doublePreferencesKey("stage2_weight_min_kg")
        val S2_MAX = doublePreferencesKey("stage2_weight_max_kg")
        val S2_VF = intPreferencesKey("stage2_visceral_fat_max")
        val S2_MONTHS = intPreferencesKey("stage2_months")

        val S3_MIN = doublePreferencesKey("stage3_weight_min_kg")
        val S3_MAX = doublePreferencesKey("stage3_weight_max_kg")
        val S3_VF = intPreferencesKey("stage3_visceral_fat_max")
        val S3_MONTHS = intPreferencesKey("stage3_months")
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
        val startW = syncPrefs.getString("start_weight_kg", null)?.toDoubleOrNull() ?: 98.9

        val s1Min = syncPrefs.getString("stage1_weight_min_kg", null)?.toDoubleOrNull() ?: 93.0
        val s1Max = syncPrefs.getString("stage1_weight_max_kg", null)?.toDoubleOrNull() ?: 94.0
        val s1Vf = syncPrefs.getInt("stage1_visceral_fat_max", 10)
        val s1M = syncPrefs.getInt("stage1_months", 2)

        val s2Min = syncPrefs.getString("stage2_weight_min_kg", null)?.toDoubleOrNull() ?: 85.0
        val s2Max = syncPrefs.getString("stage2_weight_max_kg", null)?.toDoubleOrNull() ?: 88.0
        val s2Vf = syncPrefs.getInt("stage2_visceral_fat_max", 9)
        val s2M = syncPrefs.getInt("stage2_months", 6)

        val s3Min = syncPrefs.getString("stage3_weight_min_kg", null)?.toDoubleOrNull() ?: 78.0
        val s3Max = syncPrefs.getString("stage3_weight_max_kg", null)?.toDoubleOrNull() ?: 82.0
        val s3Vf = syncPrefs.getInt("stage3_visceral_fat_max", 7)
        val s3M = syncPrefs.getInt("stage3_months", 12)

        return UserProfile(
            sex = sex,
            heightCm = height,
            birthDate = birth,
            activityFactor = activity,
            muscleBaselineKg = muscle,
            goalStartDate = start,
            startWeightKg = startW,
            stage1WeightMinKg = s1Min,
            stage1WeightMaxKg = s1Max,
            stage1VisceralFatMax = s1Vf,
            stage1Months = s1M,
            stage2WeightMinKg = s2Min,
            stage2WeightMaxKg = s2Max,
            stage2VisceralFatMax = s2Vf,
            stage2Months = s2M,
            stage3WeightMinKg = s3Min,
            stage3WeightMaxKg = s3Max,
            stage3VisceralFatMax = s3Vf,
            stage3Months = s3M
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
            .putString("start_weight_kg", profile.startWeightKg.toString())
            .putString("stage1_weight_min_kg", profile.stage1WeightMinKg.toString())
            .putString("stage1_weight_max_kg", profile.stage1WeightMaxKg.toString())
            .putInt("stage1_visceral_fat_max", profile.stage1VisceralFatMax)
            .putInt("stage1_months", profile.stage1Months)
            .putString("stage2_weight_min_kg", profile.stage2WeightMinKg.toString())
            .putString("stage2_weight_max_kg", profile.stage2WeightMaxKg.toString())
            .putInt("stage2_visceral_fat_max", profile.stage2VisceralFatMax)
            .putInt("stage2_months", profile.stage2Months)
            .putString("stage3_weight_min_kg", profile.stage3WeightMinKg.toString())
            .putString("stage3_weight_max_kg", profile.stage3WeightMaxKg.toString())
            .putInt("stage3_visceral_fat_max", profile.stage3VisceralFatMax)
            .putInt("stage3_months", profile.stage3Months)
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
        val startW = prefs[Keys.START_WEIGHT_KG] ?: backup.startWeightKg

        val resolved = UserProfile(
            sex = sex,
            heightCm = height,
            birthDate = birth,
            activityFactor = activity,
            muscleBaselineKg = muscle,
            goalStartDate = start,
            startWeightKg = startW,
            stage1WeightMinKg = prefs[Keys.S1_MIN] ?: backup.stage1WeightMinKg,
            stage1WeightMaxKg = prefs[Keys.S1_MAX] ?: backup.stage1WeightMaxKg,
            stage1VisceralFatMax = prefs[Keys.S1_VF] ?: backup.stage1VisceralFatMax,
            stage1Months = prefs[Keys.S1_MONTHS] ?: backup.stage1Months,
            stage2WeightMinKg = prefs[Keys.S2_MIN] ?: backup.stage2WeightMinKg,
            stage2WeightMaxKg = prefs[Keys.S2_MAX] ?: backup.stage2WeightMaxKg,
            stage2VisceralFatMax = prefs[Keys.S2_VF] ?: backup.stage2VisceralFatMax,
            stage2Months = prefs[Keys.S2_MONTHS] ?: backup.stage2Months,
            stage3WeightMinKg = prefs[Keys.S3_MIN] ?: backup.stage3WeightMinKg,
            stage3WeightMaxKg = prefs[Keys.S3_MAX] ?: backup.stage3WeightMaxKg,
            stage3VisceralFatMax = prefs[Keys.S3_VF] ?: backup.stage3VisceralFatMax,
            stage3Months = prefs[Keys.S3_MONTHS] ?: backup.stage3Months
        )
        writeBackupProfile(resolved)
        resolved
    }

    suspend fun saveProfile(profile: UserProfile) {
        writeBackupProfile(profile)
        context.profileDataStore.edit { prefs ->
            prefs[Keys.SEX] = profile.sex.name
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.BIRTH_DATE] = profile.birthDate.toString()
            prefs[Keys.ACTIVITY_FACTOR] = profile.activityFactor
            prefs[Keys.MUSCLE_BASELINE] = profile.muscleBaselineKg
            prefs[Keys.GOAL_START_DATE] = profile.goalStartDate.toString()
            prefs[Keys.START_WEIGHT_KG] = profile.startWeightKg
            prefs[Keys.S1_MIN] = profile.stage1WeightMinKg
            prefs[Keys.S1_MAX] = profile.stage1WeightMaxKg
            prefs[Keys.S1_VF] = profile.stage1VisceralFatMax
            prefs[Keys.S1_MONTHS] = profile.stage1Months
            prefs[Keys.S2_MIN] = profile.stage2WeightMinKg
            prefs[Keys.S2_MAX] = profile.stage2WeightMaxKg
            prefs[Keys.S2_VF] = profile.stage2VisceralFatMax
            prefs[Keys.S2_MONTHS] = profile.stage2Months
            prefs[Keys.S3_MIN] = profile.stage3WeightMinKg
            prefs[Keys.S3_MAX] = profile.stage3WeightMaxKg
            prefs[Keys.S3_VF] = profile.stage3VisceralFatMax
            prefs[Keys.S3_MONTHS] = profile.stage3Months
        }
    }
}
