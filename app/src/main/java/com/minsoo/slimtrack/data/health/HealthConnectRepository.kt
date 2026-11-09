package com.minsoo.slimtrack.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.WeightLogEntity
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class HealthConnectStatus {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

data class HealthSyncSummary(
    val exercisesSynced: Int,
    val weightRecordsSynced: Int,
    val latestWeightKg: Float?,
    val latestBodyFatPct: Float?,
    val messageKo: String
)

class HealthConnectRepository(private val context: Context) {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
    )

    fun checkAvailability(): HealthConnectStatus {
        return runCatching {
            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.UPDATE_REQUIRED
                else -> HealthConnectStatus.UNAVAILABLE
            }
        }.getOrDefault(HealthConnectStatus.UNAVAILABLE)
    }

    private fun clientOrNull(): HealthConnectClient? {
        if (checkAvailability() != HealthConnectStatus.AVAILABLE) return null
        return runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        return runCatching {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        }.getOrDefault(false)
    }

    suspend fun readExercises(
        startDate: LocalDate,
        endDate: LocalDate,
        currentWeightKg: Double = 98.4
    ): List<ExerciseEntity> {
        val client = clientOrNull() ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val startInstant = startDate.atStartOfDay(zone).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(zone).toInstant()

        return runCatching {
            val sessions = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                )
            ).records

            val activeCalories = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                )
            ).records

            val stepRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                )
            ).records

            val result = mutableListOf<ExerciseEntity>()

            for (session in sessions) {
                val dateIso = session.startTime.atZone(zone).toLocalDate().toString()
                val durationMin = Duration.between(session.startTime, session.endTime).toMinutes()
                    .toInt().coerceAtLeast(1)

                val overlappingKcal = activeCalories
                    .filter { it.startTime < session.endTime && it.endTime > session.startTime }
                    .sumOf { it.energy.inKilocalories }
                    .roundToInt()

                val overlappingSteps = stepRecords
                    .filter { it.startTime < session.endTime && it.endTime > session.startTime }
                    .sumOf { it.count }
                    .toInt()

                val estimatedKcal = if (overlappingKcal > 0) {
                    overlappingKcal
                } else {
                    (durationMin * 6.5).roundToInt()
                }

                result += ExerciseEntity(
                    hcId = session.metadata.id,
                    dateIso = dateIso,
                    exerciseType = mapExerciseTypeKo(session.exerciseType),
                    title = session.title?.takeIf { it.isNotBlank() } ?: mapExerciseTypeKo(session.exerciseType),
                    durationMinutes = durationMin,
                    activeKcal = estimatedKcal,
                    steps = overlappingSteps,
                    source = "HC"
                )
            }

            // Method A (방식 A): Aggregate daily steps from Health Connect StepsRecord,
            // excluding steps already inside an explicit ExerciseSessionRecord to avoid double counting,
            // and award bonus exercise kcal for steps above 4,000 steps/day.
            val stepsByDate = stepRecords.groupBy { it.startTime.atZone(zone).toLocalDate().toString() }
            for ((dateIso, dayStepList) in stepsByDate) {
                val totalSteps = dayStepList.sumOf { it.count }.toInt()
                val nonSessionSteps = dayStepList
                    .filter { sr ->
                        sessions.none { sess -> sr.startTime < sess.endTime && sr.endTime > sess.startTime }
                    }
                    .sumOf { it.count }
                    .toInt()

                if (totalSteps > 0) {
                    val bonusSteps = (nonSessionSteps - com.minsoo.slimtrack.domain.CalorieEngine.BASELINE_STEPS_INCLUDED_IN_TDEE)
                        .coerceAtLeast(0)
                    val bonusKcal = com.minsoo.slimtrack.domain.CalorieEngine.computeBonusStepsKcal(
                        totalSteps = nonSessionSteps,
                        weightKg = currentWeightKg
                    )
                    result += ExerciseEntity(
                        hcId = "hc-steps-$dateIso",
                        dateIso = dateIso,
                        exerciseType = "일일 걸음수(방식A)",
                        title = "도보 ${totalSteps}보 (4,000보 초과 ${bonusSteps}보)",
                        durationMinutes = (totalSteps / 100).coerceAtLeast(1),
                        activeKcal = bonusKcal,
                        steps = totalSteps,
                        source = "HC_STEPS"
                    )
                }
            }

            // Fallback: if a day has neither ExerciseSessionRecord nor StepsRecord, check ActiveCaloriesBurnedRecord
            val coveredDates = result.map { it.dateIso }.toSet()
            val byDate = activeCalories.groupBy { it.startTime.atZone(zone).toLocalDate().toString() }
            for ((dateIso, records) in byDate) {
                if (dateIso !in coveredDates) {
                    val kcal = records.sumOf { it.energy.inKilocalories }.roundToInt()
                    if (kcal >= 30) {
                        result += ExerciseEntity(
                            hcId = "active-kcal-$dateIso",
                            dateIso = dateIso,
                            exerciseType = "일상 활동 소모",
                            title = "Health Connect 활동 칼로리",
                            durationMinutes = 30,
                            activeKcal = kcal,
                            steps = 0,
                            source = "HC"
                        )
                    }
                }
            }

            result
        }.getOrDefault(emptyList())
    }

    /**
     * Reads WeightRecord and BodyFatRecord from Health Connect for manual or scheduled sync.
     */
    suspend fun readWeightLogs(startDate: LocalDate, endDate: LocalDate): List<WeightLogEntity> {
        val client = clientOrNull() ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val startInstant = startDate.atStartOfDay(zone).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(zone).toInstant()
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        return runCatching {
            val weights = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                )
            ).records

            val bodyFats = client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                )
            ).records

            val fatByDate = bodyFats.groupBy { it.time.atZone(zone).toLocalDate().toString() }
                .mapValues { (_, list) -> list.maxByOrNull { it.time }?.percentage?.value?.toFloat() }

            weights.map { w ->
                val zdt = w.time.atZone(zone)
                val dateIso = zdt.toLocalDate().toString()
                WeightLogEntity(
                    measuredAtIso = zdt.format(fmt),
                    dateIso = dateIso,
                    weightKg = ((w.weight.inKilograms * 10.0).roundToInt() / 10.0f),
                    bodyFatPct = fatByDate[dateIso],
                    source = "HC",
                    hcRecordId = w.metadata.id
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun mapExerciseTypeKo(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "러닝"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "걷기"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "사이클"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "수영"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "근력 운동"
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "등산"
        else -> "운동"
    }
}
