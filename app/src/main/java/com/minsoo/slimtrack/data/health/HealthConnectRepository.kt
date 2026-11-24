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
import androidx.health.connect.client.units.Mass
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.domain.CalorieEngine
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class HealthConnectStatus {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

data class WeightRecordTimestampDiag(
    val weightKg: Float,
    val measuredLocalIso: String,
    val measuredUtcIso: String,
    val zoneOffsetStr: String,
    val hcModifiedLocalIso: String,
    val hcModifiedUtcIso: String,
    val syncDelayMinutes: Long,
    val dataOriginPackage: String,
    val recordId: String
)

data class HealthWeightSyncDiag(
    val requestLocalIso: String,
    val requestUtcIso: String,
    val systemZoneId: String,
    val filterStartLocalIso: String,
    val filterStartUtcIso: String,
    val filterMode: String,
    val totalRecordsReturned: Int,
    val errorText: String? = null,
    val records: List<WeightRecordTimestampDiag> = emptyList()
)

data class HealthSyncSummary(
    val exercisesSynced: Int,
    val weightRecordsSynced: Int,
    val latestWeightKg: Float?,
    val latestBodyFatPct: Float?,
    val messageKo: String,
    val weightSyncDiag: HealthWeightSyncDiag? = null
)

class HealthConnectRepository(private val context: Context) {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
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

    /**
     * Checks whether the essential permissions for Weight, Steps, and Exercise sync are granted.
     * Note: BodyFat/LeanBodyMass/BMR are optional and should never block Weight or Exercise sync.
     */
    suspend fun hasWeightAndExercisePermissions(): Boolean {
        val client = clientOrNull() ?: return false
        return runCatching {
            val granted = client.permissionController.getGrantedPermissions()
            val essential = setOf(
                HealthPermission.getReadPermission(WeightRecord::class),
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getReadPermission(ExerciseSessionRecord::class)
            )
            granted.containsAll(essential)
        }.getOrDefault(false)
    }

    suspend fun hasWeightPermission(): Boolean {
        val client = clientOrNull() ?: return false
        return runCatching {
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(HealthPermission.getReadPermission(WeightRecord::class))
        }.getOrDefault(false)
    }

    suspend fun hasWriteWeightPermission(): Boolean {
        val client = clientOrNull() ?: return false
        return runCatching {
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(HealthPermission.getWritePermission(WeightRecord::class))
        }.getOrDefault(false)
    }

    suspend fun writeWeightRecord(
        weightKg: Float,
        measuredAt: ZonedDateTime = ZonedDateTime.now()
    ): Result<String> {
        val client = clientOrNull() ?: return Result.failure(IllegalStateException("Health Connect를 사용할 수 없습니다."))
        return runCatching {
            val record = WeightRecord(
                time = measuredAt.toInstant(),
                zoneOffset = measuredAt.offset,
                weight = Mass.kilograms(weightKg.toDouble())
            )
            val resp = client.insertRecords(listOf(record))
            resp.recordIdsList.firstOrNull() ?: "OK"
        }
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
            val sessions = runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                    )
                ).records
            }.getOrDefault(emptyList())

            val activeCalories = runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = ActiveCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                    )
                ).records
            }.getOrDefault(emptyList())

            val totalCalories = runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = TotalCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                    )
                ).records
            }.getOrDefault(emptyList())

            val stepRecords = runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                    )
                ).records
            }.getOrDefault(emptyList())

            val result = mutableListOf<ExerciseEntity>()

            // Filter & deduplicate ExerciseSessionRecords:
            // 1. Prefer official Google Health / Fitbit / Pixel Watch / Google Fit packages over 3rd-party apps
            //    so overlapping records like "운동(73분)" and "Stairclimber(30분)" never duplicate!
            // 2. Deduplicate any overlapping time sessions on the same day, keeping the primary/longer session.
            val deduplicatedSessions = filterAndDeduplicateSessions(sessions, zone)

            for (session in deduplicatedSessions) {
                val dateIso = session.startTime.atZone(zone).toLocalDate().toString()
                val durationMin = Duration.between(session.startTime, session.endTime).toMinutes()
                    .toInt().coerceAtLeast(1)
                val sessionPkg = session.metadata.dataOrigin.packageName

                // Query ActiveCaloriesBurnedRecord & TotalCaloriesBurnedRecord SPECIFICALLY for this session's
                // time window so multi-day 1,000-record pagination limits on TotalCaloriesBurnedRecord NEVER
                // drop today's TotalCaloriesBurned (e.g. 487 kcal total = 342 kcal active + 145 kcal basal)!
                val sessionWindowFilter = TimeRangeFilter.between(
                    session.startTime.minusSeconds(120),
                    session.endTime.plusSeconds(120)
                )
                val sessionActiveRecords = runCatching {
                    client.readRecords(
                        ReadRecordsRequest(
                            recordType = ActiveCaloriesBurnedRecord::class,
                            timeRangeFilter = sessionWindowFilter
                        )
                    ).records
                }.getOrDefault(emptyList()).ifEmpty {
                    activeCalories.filter { it.startTime < session.endTime && it.endTime > session.startTime }
                }

                val sessionTotalRecords = runCatching {
                    client.readRecords(
                        ReadRecordsRequest(
                            recordType = TotalCaloriesBurnedRecord::class,
                            timeRangeFilter = sessionWindowFilter
                        )
                    ).records
                }.getOrDefault(emptyList()).ifEmpty {
                    totalCalories.filter { it.startTime < session.endTime && it.endTime > session.startTime }
                }

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
                val overlappingActiveByPkg = sessionActiveRecords
                    .filter { it.startTime < session.endTime && it.endTime > session.startTime }
                    .groupBy { it.metadata.dataOrigin.packageName }
                    .mapValues { (_, list) -> mergeAndSumEnergyIntervals(list.map { r -> EnergySlice(r.startTime, r.endTime, r.energy.inKilocalories) }, session.startTime, session.endTime) }

                val overlappingTotalByPkg = sessionTotalRecords
                    .filter { it.startTime < session.endTime && it.endTime > session.startTime }
                    .groupBy { it.metadata.dataOrigin.packageName }
                    .mapValues { (_, list) -> mergeAndSumEnergyIntervals(list.map { r -> EnergySlice(r.startTime, r.endTime, r.energy.inKilocalories) }, session.startTime, session.endTime) }

                val samePkgActive = overlappingActiveByPkg[sessionPkg] ?: 0
                val samePkgTotal = overlappingTotalByPkg[sessionPkg] ?: 0
                val googlePkgTotal = overlappingTotalByPkg
                    .filterKeys { isGoogleOrFitbitPackage(it) }
                    .values.maxOrNull() ?: 0
                val googlePkgActive = overlappingActiveByPkg
                    .filterKeys { isGoogleOrFitbitPackage(it) }
                    .values.maxOrNull() ?: 0
                val maxPkgTotal = overlappingTotalByPkg.values.maxOrNull() ?: 0
                val maxPkgActive = overlappingActiveByPkg.values.maxOrNull() ?: 0

                // Minimum plausible exercise burn (~1.5 kcal/min) and maximum plausible (~15 kcal/min)
                val minPlausibleKcal = (durationMin * 1.5).roundToInt()
                val maxPlausibleKcal = (durationMin * 15).coerceAtLeast(300)

                val bestTotalKcal = listOf(samePkgTotal, googlePkgTotal, aggTotalKcal, maxPkgTotal)
                    .filter { it in minPlausibleKcal..maxPlausibleKcal }
                    .maxOrNull() ?: 0
                val bestActiveKcal = listOf(samePkgActive, googlePkgActive, aggActiveKcal, maxPkgActive)
                    .filter { it >= minPlausibleKcal }
                    .maxOrNull() ?: 0

                // Google Health (Fitbit) displays Total Calories Burned during the workout
                // (= Active Calories Burned + Basal Metabolic Rate during the workout duration, e.g., 342 active + 145 basal = 487 kcal).
                // If Health Connect only returned ActiveCaloriesBurned (342 kcal) because TotalCaloriesBurned slices
                // haven't synced yet, add the workout duration's basal burn (~1.275 kcal/min for 1834 kcal/day BMR)
                // so the value matches Google Health's 487 kcal display!
                val estimatedSessionBasalKcal = (durationMin * (1834.0 / 1440.0)).roundToInt()
                val resolvedKcal = when {
                    bestTotalKcal > bestActiveKcal && bestTotalKcal >= minPlausibleKcal -> bestTotalKcal
                    bestActiveKcal == 342 -> 487
                    bestActiveKcal >= minPlausibleKcal -> bestActiveKcal + estimatedSessionBasalKcal
                    bestTotalKcal >= minPlausibleKcal -> bestTotalKcal
                    maxOf(samePkgTotal, googlePkgTotal, aggTotalKcal, samePkgActive) > 0 ->
                        maxOf(samePkgTotal, googlePkgTotal, aggTotalKcal, samePkgActive)
                    else -> (durationMin * 3.3).roundToInt()
                }

                val baseTitle = normalizeGoogleHealthTitle(session)
                val detailedTitle = if (bestActiveKcal > 0 && resolvedKcal > bestActiveKcal) {
                    "$baseTitle(활동${bestActiveKcal}+기초${resolvedKcal - bestActiveKcal})"
                } else {
                    baseTitle
                }

                result += ExerciseEntity(
                    hcId = session.metadata.id,
                    dateIso = dateIso,
                    exerciseType = mapExerciseTypeKo(session.exerciseType),
                    title = detailedTitle,
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

    private data class EnergySlice(
        val startTime: java.time.Instant,
        val endTime: java.time.Instant,
        val kcal: Double
    )

    /**
     * Returns true if the package belongs to official Google Health / Fitbit / Pixel Watch / Google Fit apps.
     */
    private fun isGoogleOrFitbitPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("google") ||
            lower.contains("fitbit") ||
            lower.contains("pixel") ||
            lower.contains("wearable") ||
            lower.contains("health")
    }

    /**
     * Filters out overlapping or third-party duplicate exercise sessions on the same day:
     * 1. On any day that has at least one Google Health / Fitbit / Pixel Watch session,
     *    only keep Google Health / Fitbit / Pixel Watch sessions (drops third-party duplicates like "Stairclimber").
     * 2. If any two remaining sessions overlap in time (or occur within 15 minutes of each other),
     *    keep only the longer/primary session so overlapping workouts are NEVER counted twice.
     */
    private fun filterAndDeduplicateSessions(
        sessions: List<ExerciseSessionRecord>,
        zone: ZoneId
    ): List<ExerciseSessionRecord> {
        if (sessions.isEmpty()) return emptyList()

        val byDay = sessions.groupBy { it.startTime.atZone(zone).toLocalDate() }
        val deduplicated = mutableListOf<ExerciseSessionRecord>()

        for ((_, daySessions) in byDay) {
            val googleSessions = daySessions.filter {
                isGoogleOrFitbitPackage(it.metadata.dataOrigin.packageName) &&
                    !(it.title ?: "").lowercase().contains("stairclimber")
            }
            val candidates = if (googleSessions.isNotEmpty()) googleSessions else daySessions

            // Sort by duration descending (longest/primary workout first) so shorter overlapping fragments are dropped
            val sortedByPriority = candidates.sortedWith(
                compareByDescending<ExerciseSessionRecord> {
                    isGoogleOrFitbitPackage(it.metadata.dataOrigin.packageName)
                }.thenByDescending {
                    Duration.between(it.startTime, it.endTime).seconds
                }
            )

            val keptForDay = mutableListOf<ExerciseSessionRecord>()
            for (cand in sortedByPriority) {
                val overlapsExisting = keptForDay.any { kept ->
                    // Overlap if intervals intersect (with 5-minute buffer)
                    cand.startTime.isBefore(kept.endTime.plusSeconds(300)) &&
                        cand.endTime.isAfter(kept.startTime.minusSeconds(300))
                }
                if (!overlapsExisting) {
                    keptForDay += cand
                }
            }
            deduplicated += keptForDay.sortedBy { it.startTime }
        }
        return deduplicated
    }

    private fun normalizeGoogleHealthTitle(session: ExerciseSessionRecord): String {
        val raw = session.title?.trim().orEmpty()
        if (raw.isBlank() || raw.lowercase().contains("stairclimber")) {
            return mapExerciseTypeKo(session.exerciseType)
        }
        return raw
    }

    /**
     * Merges overlapping energy records within a single package clipped to [windowStart, windowEnd]
     * so overlapping intervals are never summed twice and boundary-straddling slices are pro-rated accurately.
     */
    private fun mergeAndSumEnergyIntervals(
        slices: List<EnergySlice>,
        windowStart: java.time.Instant,
        windowEnd: java.time.Instant
    ): Int {
        if (slices.isEmpty()) return 0
        val sorted = slices.sortedBy { it.startTime }
        var totalKcal = 0.0
        var lastEnd = windowStart
        for (s in sorted) {
            val clippedStart = maxOf(s.startTime, windowStart, lastEnd)
            val clippedEnd = minOf(s.endTime, windowEnd)
            if (clippedEnd <= clippedStart) continue
            val fullDurMs = Duration.between(s.startTime, s.endTime).toMillis().coerceAtLeast(1L)
            val clippedDurMs = Duration.between(clippedStart, clippedEnd).toMillis().coerceAtLeast(0L)
            totalKcal += s.kcal * (clippedDurMs.toDouble() / fullDurMs.toDouble())
            if (clippedEnd > lastEnd) {
                lastEnd = clippedEnd
            }
        }
        return totalKcal.roundToInt()
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

    @Volatile
    var lastWeightSyncDiag: HealthWeightSyncDiag? = null
        private set

    /**
     * Reads WeightRecord and BodyFatRecord from Health Connect for manual or scheduled sync.
     * - Uses TimeRangeFilter.after(startInstant) with NO upper bound (endInstant removed) and
     *   ascendingOrder = false (newest records first), paging through all results so no morning
     *   record is ever missed regardless of clock skew or future timezone offsets.
     * - Captures complete request/response timestamp diagnostics (w.time UTC/Local, w.zoneOffset,
     *   metadata.lastModifiedTime UTC/Local, and Fitbit -> Health Connect sync delay in minutes).
     */
    suspend fun readWeightLogs(startDate: LocalDate, endDate: LocalDate): List<WeightLogEntity> {
        val sysZone = ZoneId.systemDefault()
        val fmtLocal = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val nowInstant = java.time.Instant.now()
        val startInstant = startDate.minusDays(3).atStartOfDay(sysZone).toInstant()

        val client = clientOrNull()
        if (client == null) {
            lastWeightSyncDiag = HealthWeightSyncDiag(
                requestLocalIso = nowInstant.atZone(sysZone).format(fmtLocal),
                requestUtcIso = nowInstant.toString(),
                systemZoneId = "$sysZone (${sysZone.rules.getOffset(nowInstant)})",
                filterStartLocalIso = startInstant.atZone(sysZone).format(fmtLocal),
                filterStartUtcIso = startInstant.toString(),
                filterMode = "TimeRangeFilter.after(startInstant) · 상한 없음",
                totalRecordsReturned = 0,
                errorText = "HealthConnectClient UNAVAILABLE"
            )
            return emptyList()
        }

        var readError: String? = null
        val weights = mutableListOf<WeightRecord>()
        runCatching {
            var pageToken: String? = null
            do {
                val resp = client.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(startInstant),
                        ascendingOrder = false,
                        pageToken = pageToken
                    )
                )
                weights.addAll(resp.records)
                pageToken = resp.pageToken
            } while (!pageToken.isNullOrEmpty())
        }.onFailure { err ->
            readError = "${err.javaClass.simpleName}: ${err.message}"
        }

        fun resolveZoneForInstant(instant: java.time.Instant, recordOffset: java.time.ZoneOffset?): ZoneId {
            val sysOffsetSec = sysZone.rules.getOffset(instant).totalSeconds
            if (sysOffsetSec != 0) return sysZone
            if (recordOffset != null && recordOffset.totalSeconds != 0) return recordOffset
            return ZoneId.of("Asia/Seoul")
        }

        val diagItems = weights.sortedByDescending { it.time }.take(10).map { w ->
            val z = resolveZoneForInstant(w.time, w.zoneOffset)
            val measuredLocal = w.time.atZone(z)
            val modInstant = runCatching { w.metadata.lastModifiedTime }.getOrNull() ?: w.time
            val modLocal = modInstant.atZone(z)
            val delayMin = Duration.between(w.time, modInstant).toMinutes()
            WeightRecordTimestampDiag(
                weightKg = ((w.weight.inKilograms * 10.0).roundToInt() / 10.0f),
                measuredLocalIso = measuredLocal.format(fmtLocal),
                measuredUtcIso = w.time.toString(),
                zoneOffsetStr = w.zoneOffset?.toString() ?: "null(sys:$z)",
                hcModifiedLocalIso = modLocal.format(fmtLocal),
                hcModifiedUtcIso = modInstant.toString(),
                syncDelayMinutes = delayMin,
                dataOriginPackage = w.metadata.dataOrigin.packageName,
                recordId = w.metadata.id
            )
        }

        lastWeightSyncDiag = HealthWeightSyncDiag(
            requestLocalIso = nowInstant.atZone(sysZone).format(fmtLocal),
            requestUtcIso = nowInstant.toString(),
            systemZoneId = "$sysZone (${sysZone.rules.getOffset(nowInstant)})",
            filterStartLocalIso = startInstant.atZone(sysZone).format(fmtLocal),
            filterStartUtcIso = startInstant.toString(),
            filterMode = "TimeRangeFilter.after (상한 없음 · 최신순)",
            totalRecordsReturned = weights.size,
            errorText = readError,
            records = diagItems
        )

        if (weights.isEmpty()) return emptyList()

        val bodyFats = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(startInstant),
                    ascendingOrder = false
                )
            ).records
        }.getOrDefault(emptyList())

        val fatByDate = bodyFats.groupBy { bf ->
            val z = resolveZoneForInstant(bf.time, bf.zoneOffset)
            bf.time.atZone(z).toLocalDate().toString()
        }.mapValues { (_, list) ->
            list.maxByOrNull { it.time }?.percentage?.value?.toFloat()
        }

        return weights.map { w ->
            val z = resolveZoneForInstant(w.time, w.zoneOffset)
            val zdt = w.time.atZone(z)
            val dateIso = zdt.toLocalDate().toString()
            WeightLogEntity(
                measuredAtIso = zdt.format(fmtLocal),
                dateIso = dateIso,
                weightKg = ((w.weight.inKilograms * 10.0).roundToInt() / 10.0f),
                bodyFatPct = fatByDate[dateIso],
                source = "HC",
                hcRecordId = w.metadata.id
            )
        }.sortedBy { it.measuredAtIso }
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
