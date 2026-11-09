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
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.domain.CalorieEngine
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

            val totalCalories = client.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
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
                val sessionPkg = session.metadata.dataOrigin.packageName

                // 1. Official Health Connect aggregate over the exact session window
                val sessionAgg = runCatching {
                    client.aggregate(
                        AggregateRequest(
                            metrics = setOf(
                                ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                                TotalCaloriesBurnedRecord.ENERGY_TOTAL
                            ),
                            timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                        )
                    )
                }.getOrNull()
                val aggActiveKcal = sessionAgg?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
                    ?.inKilocalories?.roundToInt() ?: 0
                val aggTotalKcal = sessionAgg?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                    ?.inKilocalories?.roundToInt() ?: 0

                // 2. Records overlapping the session window, grouped by source package to avoid double counting
                val overlappingActiveByPkg = activeCalories
                    .filter { it.startTime < session.endTime && it.endTime > session.startTime }
                    .groupBy { it.metadata.dataOrigin.packageName }
                    .mapValues { (_, list) -> list.sumOf { it.energy.inKilocalories }.roundToInt() }

                val overlappingTotalByPkg = totalCalories
                    .filter { it.startTime >= session.startTime.minusSeconds(60) && it.endTime <= session.endTime.plusSeconds(60) }
                    .groupBy { it.metadata.dataOrigin.packageName }
                    .mapValues { (_, list) -> list.sumOf { it.energy.inKilocalories }.roundToInt() }

                val samePkgActive = overlappingActiveByPkg[sessionPkg] ?: 0
                val samePkgTotal = overlappingTotalByPkg[sessionPkg] ?: 0
                val maxPkgActive = overlappingActiveByPkg.values.maxOrNull() ?: 0
                val maxPkgTotal = overlappingTotalByPkg.values.maxOrNull() ?: 0

                // Minimum plausible exercise burn (~1.5 kcal/min) so a stray 12 kcal sensor fragment
                // never overrides the actual 158 kcal workout record from Google Health/Fit!
                val minPlausibleKcal = (durationMin * 1.5).roundToInt()

                val resolvedKcal = when {
                    samePkgTotal >= minPlausibleKcal -> samePkgTotal
                    samePkgActive >= minPlausibleKcal -> samePkgActive
                    maxPkgTotal >= minPlausibleKcal -> maxPkgTotal
                    aggActiveKcal >= minPlausibleKcal -> aggActiveKcal
                    maxPkgActive >= minPlausibleKcal -> maxPkgActive
                    aggTotalKcal in minPlausibleKcal..(durationMin * 15) -> aggTotalKcal
                    maxOf(samePkgTotal, samePkgActive, maxPkgTotal, maxPkgActive) > 0 ->
                        maxOf(samePkgTotal, samePkgActive, maxPkgTotal, maxPkgActive)
                    else -> (durationMin * 3.5).roundToInt()
                }

                result += ExerciseEntity(
                    hcId = session.metadata.id,
                    dateIso = dateIso,
                    exerciseType = mapExerciseTypeKo(session.exerciseType),
                    title = session.title?.takeIf { it.isNotBlank() } ?: mapExerciseTypeKo(session.exerciseType),
                    durationMinutes = durationMin,
                    activeKcal = resolvedKcal,
                    steps = 0,
                    source = "HC"
                )
            }

            // Method A (방식 A): Read deduplicated daily step counts from Health Connect using
            // HealthConnectClient.aggregate(StepsRecord.COUNT_TOTAL) per day so multiple apps
            // (e.g., phone + watch/Google Fit) are NEVER summed twice (5,825 steps stays 5,825 steps!).
            var cursor = startDate
            while (!cursor.isAfter(endDate)) {
                val dateIso = cursor.toString()
                val dayStart = cursor.atStartOfDay(zone).toInstant()
                val dayEnd = cursor.plusDays(1).atStartOfDay(zone).toInstant()

                val dayStepRecords = stepRecords.filter {
                    it.startTime.atZone(zone).toLocalDate() == cursor
                }

                // Single-source deduplicated count (max across dataOrigin packages)
                val maxSingleSourceSteps = dayStepRecords
                    .groupBy { it.metadata.dataOrigin.packageName }
                    .mapValues { (_, list) -> mergeAndSumStepIntervals(list) }
                    .values
                    .maxOrNull() ?: 0

                val aggregatedSteps = runCatching {
                    client.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd)
                        )
                    )[StepsRecord.COUNT_TOTAL]?.toInt()
                }.getOrNull() ?: 0

                // If aggregate is inflated by non-overlapping duplicate app syncs, prefer maxSingleSourceSteps
                val totalSteps = when {
                    aggregatedSteps in 1..maxSingleSourceSteps -> aggregatedSteps
                    maxSingleSourceSteps > 0 && aggregatedSteps > (maxSingleSourceSteps * 1.25).toInt() -> maxSingleSourceSteps
                    aggregatedSteps > 0 -> aggregatedSteps
                    else -> maxSingleSourceSteps
                }

                if (totalSteps > 0) {
                    val bonusSteps = (totalSteps - CalorieEngine.BASELINE_STEPS_INCLUDED_IN_TDEE)
                        .coerceAtLeast(0)
                    val bonusKcal = CalorieEngine.computeBonusStepsKcal(
                        totalSteps = totalSteps,
                        weightKg = currentWeightKg
                    )
                    result += ExerciseEntity(
                        hcId = "hc-steps-$dateIso",
                        dateIso = dateIso,
                        exerciseType = "일일 걸음수(방식A)",
                        title = "도보 ${totalSteps}보 (4,000보 초과 ${bonusSteps}보)",
                        durationMinutes = 0,
                        activeKcal = bonusKcal,
                        steps = totalSteps,
                        source = "HC_STEPS"
                    )
                }

                cursor = cursor.plusDays(1)
            }

            result
        }.getOrDefault(emptyList())
    }

    /**
     * Merges overlapping StepsRecord intervals within a single data source package
     * so overlapping time windows from the same app are never counted twice.
     */
    private fun mergeAndSumStepIntervals(records: List<StepsRecord>): Int {
        if (records.isEmpty()) return 0
        val sorted = records.sortedBy { it.startTime }
        var total = 0L
        var lastEnd = java.time.Instant.MIN
        for (r in sorted) {
            if (r.endTime <= lastEnd) {
                continue
            }
            if (r.startTime < lastEnd) {
                val fullDur = Duration.between(r.startTime, r.endTime).toMillis().coerceAtLeast(1L)
                val nonOverlapDur = Duration.between(lastEnd, r.endTime).toMillis().coerceAtLeast(0L)
                total += (r.count * nonOverlapDur) / fullDur
            } else {
                total += r.count
            }
            if (r.endTime > lastEnd) {
                lastEnd = r.endTime
            }
        }
        return total.toInt()
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
