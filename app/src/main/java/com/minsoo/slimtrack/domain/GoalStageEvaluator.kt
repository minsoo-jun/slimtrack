package com.minsoo.slimtrack.domain

import com.minsoo.slimtrack.data.db.InBodyEntity
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.data.prefs.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

enum class GoalStage(
    val number: Int,
    val titleKo: String,
    val periodDescriptionKo: String,
    val targetWeightMinKg: Double,
    val targetWeightMaxKg: Double,
    val targetWeightMidKg: Double,
    val targetVisceralFatMin: Int,
    val targetVisceralFatMax: Int,
    val strategyPointsKo: List<String>
) {
    STAGE_1_SHORT(
        number = 1,
        titleKo = "1단계: 단기 목표 (1~2개월 내)",
        periodDescriptionKo = "2026-11-09 ~ 2027-01-09 (약 2개월)",
        targetWeightMinKg = 93.0,
        targetWeightMaxKg = 94.0,
        targetWeightMidKg = 93.5,
        targetVisceralFatMin = 10,
        targetVisceralFatMax = 10,
        strategyPointsKo = listOf(
            "단기간에 무리하게 빼기보다 한 달에 2~3kg 정도씩 안전하게 감량하는 페이스가 좋습니다.",
            "체중 감량분이 체지방에서 소모되도록 유도하면, 내장지방 레벨도 자연스럽게 낮아져 '표준' 경계선에 안착할 수 있습니다."
        )
    ),
    STAGE_2_MID(
        number = 2,
        titleKo = "2단계: 중기 목표 (3~6개월 내)",
        periodDescriptionKo = "3~6개월 집중 감량 구간",
        targetWeightMinKg = 85.0,
        targetWeightMaxKg = 88.0,
        targetWeightMidKg = 86.5,
        targetVisceralFatMin = 8,
        targetVisceralFatMax = 9,
        strategyPointsKo = listOf(
            "이 시점부터는 BMI 수치도 정상 범위나 과체중 경계선으로 내려오기 시작합니다.",
            "유산소 운동(Zone 2 저강도 유산소 등) 비중을 꾸준히 유지하면서, 근력 운동을 통해 골격근량이 손실되지 않도록 방어하는 것이 핵심입니다."
        )
    ),
    STAGE_3_LONG(
        number = 3,
        titleKo = "3단계: 최종 목표 (장기 관리)",
        periodDescriptionKo = "장기 유지 및 대사 건강 완성",
        targetWeightMinKg = 78.0,
        targetWeightMaxKg = 82.0,
        targetWeightMidKg = 80.0,
        targetVisceralFatMin = 5,
        targetVisceralFatMax = 7,
        strategyPointsKo = listOf(
            "체지방률을 10대 후반~20% 초반대로 낮추어 대사 질환 위험을 완전히 낮추고, 가벼워진 체중으로 러닝이나 일상 활동에서 관절 부담을 최소화하는 단계입니다."
        )
    );

    companion object {
        fun fromNumber(number: Int): GoalStage = entries.firstOrNull { it.number == number } ?: STAGE_1_SHORT
    }
}

data class StageSpec(
    val stage: GoalStage,
    val number: Int,
    val titleKo: String,
    val targetWeightMinKg: Double,
    val targetWeightMaxKg: Double,
    val targetWeightMidKg: Double,
    val targetVisceralFatMax: Int,
    val monthsFromStart: Int,
    val strategyPointsKo: List<String>
) {
    val targetWeightMidFormatted: String
        get() = String.format(java.util.Locale.US, "%.1f", targetWeightMidKg)
}

data class StageEvaluationResult(
    val activeStage: GoalStage,
    val activeStageSpec: StageSpec = StageSpec(
        stage = activeStage,
        number = activeStage.number,
        titleKo = activeStage.titleKo,
        targetWeightMinKg = activeStage.targetWeightMinKg,
        targetWeightMaxKg = activeStage.targetWeightMaxKg,
        targetWeightMidKg = activeStage.targetWeightMidKg,
        targetVisceralFatMax = activeStage.targetVisceralFatMax,
        monthsFromStart = when (activeStage) {
            GoalStage.STAGE_1_SHORT -> 2
            GoalStage.STAGE_2_MID -> 6
            GoalStage.STAGE_3_LONG -> 12
        },
        strategyPointsKo = activeStage.strategyPointsKo
    ),
    val allStageSpecs: List<StageSpec> = GoalStage.entries.map {
        StageSpec(
            stage = it,
            number = it.number,
            titleKo = it.titleKo,
            targetWeightMinKg = it.targetWeightMinKg,
            targetWeightMaxKg = it.targetWeightMaxKg,
            targetWeightMidKg = it.targetWeightMidKg,
            targetVisceralFatMax = it.targetVisceralFatMax,
            monthsFromStart = when (it) {
                GoalStage.STAGE_1_SHORT -> 2
                GoalStage.STAGE_2_MID -> 6
                GoalStage.STAGE_3_LONG -> 12
            },
            strategyPointsKo = it.strategyPointsKo
        )
    },
    val stage1Completed: Boolean,
    val stage2Completed: Boolean,
    val stage3Completed: Boolean,
    val weightProgressPct: Float,
    val remainingKgToActiveStage: Double,
    val visceralFatDeltaToStage: Int,
    val skeletalMuscleLossWarning: Boolean,
    val skeletalMuscleDeltaKg: Double,
    val deadlineForActiveStage: LocalDate
)

object GoalStageEvaluator {
    const val DEFAULT_START_WEIGHT_KG = 98.9
    const val DEFAULT_MUSCLE_BASELINE_KG = 38.5
    const val MUSCLE_LOSS_THRESHOLD_KG = 0.5

    fun buildStageSpecs(profile: UserProfile? = null): List<StageSpec> {
        if (profile == null) {
            return GoalStage.entries.map {
                StageSpec(
                    stage = it,
                    number = it.number,
                    titleKo = it.titleKo,
                    targetWeightMinKg = it.targetWeightMinKg,
                    targetWeightMaxKg = it.targetWeightMaxKg,
                    targetWeightMidKg = it.targetWeightMidKg,
                    targetVisceralFatMax = it.targetVisceralFatMax,
                    monthsFromStart = when (it) {
                        GoalStage.STAGE_1_SHORT -> 2
                        GoalStage.STAGE_2_MID -> 6
                        GoalStage.STAGE_3_LONG -> 12
                    },
                    strategyPointsKo = it.strategyPointsKo
                )
            }
        }
        val s1Mid = (profile.stage1WeightMinKg + profile.stage1WeightMaxKg) / 2.0
        val s2Mid = (profile.stage2WeightMinKg + profile.stage2WeightMaxKg) / 2.0
        val s3Mid = (profile.stage3WeightMinKg + profile.stage3WeightMaxKg) / 2.0
        return listOf(
            StageSpec(
                stage = GoalStage.STAGE_1_SHORT,
                number = 1,
                titleKo = "1단계: 단기 목표 (${profile.stage1Months}개월 내)",
                targetWeightMinKg = profile.stage1WeightMinKg,
                targetWeightMaxKg = profile.stage1WeightMaxKg,
                targetWeightMidKg = s1Mid,
                targetVisceralFatMax = profile.stage1VisceralFatMax,
                monthsFromStart = profile.stage1Months,
                strategyPointsKo = GoalStage.STAGE_1_SHORT.strategyPointsKo
            ),
            StageSpec(
                stage = GoalStage.STAGE_2_MID,
                number = 2,
                titleKo = "2단계: 중기 목표 (${profile.stage2Months}개월 내)",
                targetWeightMinKg = profile.stage2WeightMinKg,
                targetWeightMaxKg = profile.stage2WeightMaxKg,
                targetWeightMidKg = s2Mid,
                targetVisceralFatMax = profile.stage2VisceralFatMax,
                monthsFromStart = profile.stage2Months,
                strategyPointsKo = GoalStage.STAGE_2_MID.strategyPointsKo
            ),
            StageSpec(
                stage = GoalStage.STAGE_3_LONG,
                number = 3,
                titleKo = "3단계: 최종 목표 (${profile.stage3Months}개월 내)",
                targetWeightMinKg = profile.stage3WeightMinKg,
                targetWeightMaxKg = profile.stage3WeightMaxKg,
                targetWeightMidKg = s3Mid,
                targetVisceralFatMax = profile.stage3VisceralFatMax,
                monthsFromStart = profile.stage3Months,
                strategyPointsKo = GoalStage.STAGE_3_LONG.strategyPointsKo
            )
        )
    }

    fun evaluate(
        currentWeightKg: Double,
        visceralFatLevel: Int,
        skeletalMuscleKg: Double,
        startWeightKg: Double = DEFAULT_START_WEIGHT_KG,
        muscleBaselineKg: Double = DEFAULT_MUSCLE_BASELINE_KG,
        startDate: LocalDate = LocalDate.of(2026, 11, 9),
        profile: UserProfile? = null
    ): StageEvaluationResult {
        val specs = buildStageSpecs(profile)
        val s1 = specs[0]
        val s2 = specs[1]
        val s3 = specs[2]

        val stage1Done = currentWeightKg <= s1.targetWeightMidKg &&
            visceralFatLevel <= s1.targetVisceralFatMax
        val stage2Done = currentWeightKg <= s2.targetWeightMidKg &&
            visceralFatLevel <= s2.targetVisceralFatMax
        val stage3Done = currentWeightKg <= s3.targetWeightMidKg &&
            visceralFatLevel <= s3.targetVisceralFatMax

        val activeSpec = when {
            !stage1Done -> s1
            !stage2Done -> s2
            else -> s3
        }

        val effectiveStartWeight = profile?.startWeightKg ?: startWeightKg
        val stageStartWeight = when (activeSpec.stage) {
            GoalStage.STAGE_1_SHORT -> effectiveStartWeight
            GoalStage.STAGE_2_MID -> s1.targetWeightMidKg
            GoalStage.STAGE_3_LONG -> s2.targetWeightMidKg
        }

        val totalSpan = (stageStartWeight - activeSpec.targetWeightMidKg).coerceAtLeast(0.5)
        val lostInStage = (stageStartWeight - currentWeightKg).coerceAtLeast(0.0)
        val progress = (lostInStage / totalSpan).toFloat().coerceIn(0f, 1f)

        val remainingKg = (currentWeightKg - activeSpec.targetWeightMidKg).coerceAtLeast(0.0)
        val visceralDelta = (visceralFatLevel - activeSpec.targetVisceralFatMax).coerceAtLeast(0)

        val muscleDelta = skeletalMuscleKg - muscleBaselineKg
        val muscleWarning = muscleDelta < -MUSCLE_LOSS_THRESHOLD_KG

        val deadline = startDate.plusMonths(activeSpec.monthsFromStart.toLong().coerceAtLeast(1L))

        return StageEvaluationResult(
            activeStage = activeSpec.stage,
            activeStageSpec = activeSpec,
            allStageSpecs = specs,
            stage1Completed = stage1Done,
            stage2Completed = stage2Done,
            stage3Completed = stage3Done,
            weightProgressPct = progress,
            remainingKgToActiveStage = remainingKg,
            visceralFatDeltaToStage = visceralDelta,
            skeletalMuscleLossWarning = muscleWarning,
            skeletalMuscleDeltaKg = muscleDelta,
            deadlineForActiveStage = deadline
        )
    }
}

enum class GoalMetricType(
    val titleKo: String,
    val shortLabelKo: String,
    val unit: String,
    val lowerIsBetter: Boolean,
    val decimals: Int
) {
    WEIGHT("체중(kg)", "체중", "kg", lowerIsBetter = true, decimals = 1),
    VISCERAL_FAT("내장지방(Lv)", "내장지방", "Lv", lowerIsBetter = true, decimals = 1),
    SKELETAL_MUSCLE("골격근(kg)", "골격근", "kg", lowerIsBetter = false, decimals = 1),
    BMR("공식BMR(kcal)", "공식BMR", "kcal", lowerIsBetter = false, decimals = 0),
    BMI("BMI(체질량)", "BMI", "", lowerIsBetter = true, decimals = 1);

    fun formatValue(v: Float, includeUnit: Boolean = true): String {
        val num = if (decimals == 0) {
            String.format(Locale.US, "%d", v.toInt())
        } else {
            String.format(Locale.US, "%.${decimals}f", v)
        }
        return if (includeUnit && unit.isNotEmpty()) "$num$unit" else num
    }
}

data class DailyGoalComparisonPoint(
    val date: LocalDate,
    val dayOfWeekKo: String,
    val planTargetValue: Float,
    val actualValue: Float,
    val isMeasuredOnDay: Boolean,
    val diffFromPlanTarget: Float,
    val isAchievingTargetPace: Boolean
)

data class GoalMetricComparisonSeries(
    val metric: GoalMetricType,
    val stageNumber: Int,
    val startDate: LocalDate,
    val today: LocalDate,
    val deadline: LocalDate,
    val totalStageDays: Int,
    val elapsedDays: Int,
    val remainingDays: Int,
    val startValue: Float,
    val todayTargetValue: Float,
    val currentValue: Float,
    val targetMidValue: Float,
    val requiredDailyDeltaFromToday: Float,
    val requiredWeeklyDeltaFromToday: Float,
    val dailyPoints: List<DailyGoalComparisonPoint>
) {
    val isTodayOnTrack: Boolean
        get() = if (metric.lowerIsBetter) {
            currentValue <= todayTargetValue + 0.05f
        } else {
            currentValue >= todayTargetValue - 0.05f
        }

    val todayGapFromTarget: Float
        get() = currentValue - todayTargetValue
}

object GoalTrajectoryCalculator {
    private fun dayOfWeekKo(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }

    fun buildSeries(
        metric: GoalMetricType,
        today: LocalDate,
        profile: UserProfile,
        stageResult: StageEvaluationResult,
        currentWeightKg: Double,
        currentBmrKcal: Int,
        currentBmi: Double,
        latestInBody: InBodyEntity,
        weightLogs: List<WeightLogEntity>,
        inBodyHistory: List<InBodyEntity>
    ): GoalMetricComparisonSeries {
        val activeSpec = stageResult.activeStageSpec
        val startDate = profile.goalStartDate
        val deadline = stageResult.deadlineForActiveStage
        val totalStageDays = ChronoUnit.DAYS.between(startDate, deadline).toInt().coerceAtLeast(1)
        val elapsedDays = ChronoUnit.DAYS.between(startDate, today).toInt().coerceIn(0, totalStageDays)
        val remainingDays = ChronoUnit.DAYS.between(today, deadline).toInt().coerceAtLeast(1)

        val sortedWeights = weightLogs.sortedBy { it.measuredAtIso }
        val sortedInBodies = (inBodyHistory + latestInBody)
            .distinctBy { "${it.dateIso}_${it.measuredAtIso}" }
            .sortedBy { it.measuredAtIso }

        val weightsByDate: Map<LocalDate, Float> = sortedWeights
            .mapNotNull { log ->
                runCatching { LocalDate.parse(log.dateIso) to log.weightKg }.getOrNull()
            }
            .toMap()

        val inBodyByDate: Map<LocalDate, InBodyEntity> = sortedInBodies
            .mapNotNull { ib ->
                runCatching { LocalDate.parse(ib.dateIso) to ib }.getOrNull()
            }
            .toMap()

        // Determine Start Value, Current Value, and Midpoint Target Value for the selected metric
        val startWeight = if (startDate.isBefore(today)) {
            weightsByDate[startDate]
                ?: sortedWeights.firstOrNull { it.dateIso >= startDate.toString() && it.dateIso < today.toString() }?.weightKg
                ?: profile.startWeightKg.toFloat()
        } else {
            profile.startWeightKg.toFloat()
        }

        val earliestInBody = sortedInBodies.firstOrNull() ?: latestInBody

        val (startValue, currentValue, targetMidValue) = when (metric) {
            GoalMetricType.WEIGHT -> Triple(
                startWeight,
                currentWeightKg.toFloat(),
                activeSpec.targetWeightMidKg.toFloat()
            )
            GoalMetricType.VISCERAL_FAT -> {
                val startVf = maxOf(
                    earliestInBody.visceralFatLevel.toFloat(),
                    latestInBody.visceralFatLevel.toFloat()
                )
                val targetVf = activeSpec.targetVisceralFatMax.toFloat()
                Triple(startVf, latestInBody.visceralFatLevel.toFloat(), targetVf)
            }
            GoalMetricType.SKELETAL_MUSCLE -> {
                val startSmm = earliestInBody.skeletalMuscleKg.takeIf { it > 10f }
                    ?: profile.muscleBaselineKg.toFloat()
                val targetSmm = profile.muscleBaselineKg.toFloat()
                Triple(startSmm, latestInBody.skeletalMuscleKg, targetSmm)
            }
            GoalMetricType.BMR -> {
                val startBmr = BmrCalculator.inbodyBmr(
                    earliestInBody.weightKg.toDouble(),
                    earliestInBody.bodyFatMassKg.toDouble()
                ).toFloat()
                val targetBmr = maxOf(startBmr, currentBmrKcal.toFloat())
                Triple(startBmr, currentBmrKcal.toFloat(), targetBmr)
            }
            GoalMetricType.BMI -> {
                val startBmi = profile.bmi(startWeight.toDouble()).toFloat()
                val targetBmi = profile.bmi(activeSpec.targetWeightMidKg).toFloat()
                Triple(startBmi, currentBmi.toFloat(), targetBmi)
            }
        }

        fun planTargetAt(date: LocalDate): Float {
            val d = ChronoUnit.DAYS.between(startDate, date).toFloat().coerceIn(0f, totalStageDays.toFloat())
            val ratio = d / totalStageDays.toFloat()
            return startValue + (targetMidValue - startValue) * ratio
        }

        val todayTargetValue = planTargetAt(today)
        val requiredDailyDelta = (targetMidValue - currentValue) / remainingDays.toFloat()
        val requiredWeeklyDelta = requiredDailyDelta * 7f

        // Build daily comparison window: from startDate (or at least 6 days before today if startDate is very recent) to today
        val rawWindowStart = if (ChronoUnit.DAYS.between(startDate, today) >= 4) {
            startDate
        } else {
            minOf(startDate, today.minusDays(6))
        }
        // Cap daily series to at most last 21 days so the daily chart stays crisp and readable
        val windowStart = maxOf(rawWindowStart, today.minusDays(20))

        // Build sparse measured map for the metric
        val measuredByDate = mutableMapOf<LocalDate, Float>()
        when (metric) {
            GoalMetricType.WEIGHT -> {
                inBodyByDate.forEach { (d, ib) -> measuredByDate[d] = ib.weightKg }
                weightsByDate.forEach { (d, w) -> measuredByDate[d] = w }
            }
            GoalMetricType.VISCERAL_FAT -> {
                inBodyByDate.forEach { (d, ib) -> measuredByDate[d] = ib.visceralFatLevel.toFloat() }
            }
            GoalMetricType.SKELETAL_MUSCLE -> {
                inBodyByDate.forEach { (d, ib) -> measuredByDate[d] = ib.skeletalMuscleKg }
            }
            GoalMetricType.BMR -> {
                inBodyByDate.forEach { (d, ib) ->
                    measuredByDate[d] = BmrCalculator.inbodyBmr(
                        ib.weightKg.toDouble(),
                        ib.bodyFatMassKg.toDouble()
                    ).toFloat()
                }
            }
            GoalMetricType.BMI -> {
                inBodyByDate.forEach { (d, ib) -> measuredByDate[d] = profile.bmi(ib.weightKg.toDouble()).toFloat() }
                weightsByDate.forEach { (d, w) -> measuredByDate[d] = profile.bmi(w.toDouble()).toFloat() }
            }
        }
        measuredByDate[startDate] = measuredByDate[startDate] ?: startValue
        measuredByDate[today] = currentValue

        val totalWindowDays = ChronoUnit.DAYS.between(windowStart, today).toInt().coerceAtLeast(0)
        val dailyPoints = mutableListOf<DailyGoalComparisonPoint>()
        var lastKnownVal = measuredByDate.entries
            .filter { !it.key.isAfter(windowStart) }
            .maxByOrNull { it.key }
            ?.value ?: startValue

        for (offset in 0..totalWindowDays) {
            val d = windowStart.plusDays(offset.toLong())
            val direct = measuredByDate[d]
            val isMeasured = direct != null
            val actualVal = if (direct != null) {
                lastKnownVal = direct
                direct
            } else {
                // Linearly interpolate if there is a next known point, else carry forward
                val prevEntry = measuredByDate.entries.filter { it.key.isBefore(d) }.maxByOrNull { it.key }
                val nextEntry = measuredByDate.entries.filter { it.key.isAfter(d) }.minByOrNull { it.key }
                if (prevEntry != null && nextEntry != null) {
                    val span = ChronoUnit.DAYS.between(prevEntry.key, nextEntry.key).toFloat().coerceAtLeast(1f)
                    val step = ChronoUnit.DAYS.between(prevEntry.key, d).toFloat()
                    prevEntry.value + (nextEntry.value - prevEntry.value) * (step / span)
                } else {
                    lastKnownVal
                }
            }

            val planTarget = planTargetAt(d)
            val diff = actualVal - planTarget
            val achieving = if (metric.lowerIsBetter) {
                actualVal <= planTarget + 0.05f
            } else {
                actualVal >= planTarget - 0.05f
            }
            dailyPoints += DailyGoalComparisonPoint(
                date = d,
                dayOfWeekKo = dayOfWeekKo(d),
                planTargetValue = planTarget,
                actualValue = actualVal,
                isMeasuredOnDay = isMeasured,
                diffFromPlanTarget = diff,
                isAchievingTargetPace = achieving
            )
        }

        return GoalMetricComparisonSeries(
            metric = metric,
            stageNumber = activeSpec.number,
            startDate = startDate,
            today = today,
            deadline = deadline,
            totalStageDays = totalStageDays,
            elapsedDays = elapsedDays,
            remainingDays = remainingDays,
            startValue = startValue,
            todayTargetValue = todayTargetValue,
            currentValue = currentValue,
            targetMidValue = targetMidValue,
            requiredDailyDeltaFromToday = requiredDailyDelta,
            requiredWeeklyDeltaFromToday = requiredWeeklyDelta,
            dailyPoints = dailyPoints
        )
    }
}
