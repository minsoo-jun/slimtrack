package com.minsoo.slimtrack.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AdviceStrictness(val labelKo: String, val descriptionKo: String) {
    VERY_STRICT("매우 엄격", "변명 불허, 냉정한 수치 지적과 강도 높은 행동 지시"),
    STRICT("엄격 (기본)", "칭찬은 사실일 때 1문장만 허용, 수치 중심의 직설적 피드백"),
    NORMAL("보통", "수치 기반 피드백과 현실적인 격려 병행")
}

@Serializable
enum class ViolationSeverity(val weight: Int, val badgeKo: String) {
    HIGH(3, "🔴 심각"),
    MEDIUM(2, "🟠 경고"),
    LOW(1, "🟡 주의")
}

@Serializable
data class AdviceViolation(
    val code: String,
    val titleKo: String,
    val severity: ViolationSeverity,
    val detailKo: String
)

@Serializable
data class AdviceVerdict(
    val dateIso: String,
    val strictness: String,
    val grade: String,
    val stageNumber: Int,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val visceralFatLevel: Int,
    val skeletalMuscleKg: Double,
    val skeletalMuscleDeltaKg: Double,
    val bmrKcal: Int,
    val dailyBalanceKcal: Int,
    val weeklyCumulativeKcal: Int,
    val monthlyCumulativeKcal: Int,
    val recoveryPerDayKcal: Int,
    val actualPaceKgPerWeek: Double,
    val requiredPaceKgPerWeek: Double,
    val weeklyAlcoholKcal: Int,
    val weeklyAlcoholDays: Int,
    val avgProteinGrams: Int,
    val targetProteinGrams: Int,
    val avgExerciseKcal: Int,
    val targetExerciseKcal: Int,
    val consecutiveRedDays: Int,
    val missingMealDaysLast7: Int,
    val daysSinceLastWeightLog: Int,
    val daysSinceLastInBody: Int,
    val bmi: Double,
    val age: Int,
    val zone2HrMin: Int,
    val zone2HrMax: Int,
    val violations: List<AdviceViolation>,
    val headlineKo: String,
    val bodyLinesKo: List<String>,
    val actionsTomorrowKo: List<String>
)

data class AdviceInputSnapshot(
    val today: LocalDate,
    val strictness: AdviceStrictness,
    val stageResult: StageEvaluationResult,
    val currentWeightKg: Double,
    val weight7DaysAgoKg: Double?,
    val visceralFatLevel: Int,
    val skeletalMuscleKg: Double,
    val leanBodyMassKg: Double,
    val bmrKcal: Int,
    val dailyPlan: DailyPlan,
    val last7DayEvaluations: List<DayEvaluation>,
    val weeklyRollup: PeriodRollup,
    val monthlyRollup: PeriodRollup,
    val weeklyAlcoholKcal: Int,
    val weeklyAlcoholDays: Int,
    val avgProteinGramsLast7: Int,
    val avgExerciseKcalLast7: Int,
    val lastWeightLogDate: LocalDate?,
    val lastInBodyDate: LocalDate?,
    val bmi: Double,
    val age: Int,
    val zone2HrRange: IntRange
)

object AdviceRuleEngine {

    fun evaluate(snapshot: AdviceInputSnapshot): AdviceVerdict {
        val violations = mutableListOf<AdviceViolation>()
        val stage = snapshot.stageResult.activeStage
        val daysToDeadline = ChronoUnit.DAYS.between(snapshot.today, snapshot.stageResult.deadlineForActiveStage)
            .toInt().coerceAtLeast(1)
        val requiredPaceKgPerWeek = ((snapshot.currentWeightKg - stage.targetWeightMidKg).coerceAtLeast(0.0) / daysToDeadline) * 7.0
        val actualPaceKgPerWeek = snapshot.weight7DaysAgoKg?.let { (it - snapshot.currentWeightKg) } ?: 0.0

        // 1. Pace check
        if (snapshot.weight7DaysAgoKg != null && requiredPaceKgPerWeek > 0.1 &&
            actualPaceKgPerWeek < requiredPaceKgPerWeek * 0.7
        ) {
            violations += AdviceViolation(
                code = "PACE_BEHIND",
                titleKo = "감량 페이스 미달",
                severity = ViolationSeverity.HIGH,
                detailKo = String.format(
                    "필요 감량 속도는 주 %.2fkg인데 최근 7일 실제 감량은 %.2fkg입니다. 이 속도면 %d단계 마감을 지키지 못합니다.",
                    requiredPaceKgPerWeek,
                    actualPaceKgPerWeek,
                    stage.number
                )
            )
        }

        // 2. Consecutive RED days (sorted ascending by date)
        val sortedDays = snapshot.last7DayEvaluations.sortedBy { it.date }
        var consecutiveRed = 0
        for (d in sortedDays.reversed()) {
            if (d.statusColor == DayStatusColor.RED) consecutiveRed++ else break
        }
        if (consecutiveRed >= 2) {
            violations += AdviceViolation(
                code = "CONSECUTIVE_RED",
                titleKo = "${consecutiveRed}일 연속 목표 실패",
                severity = ViolationSeverity.HIGH,
                detailKo = "${consecutiveRed}일 연속 빨간불(칼로리 초과 또는 기록 누락)입니다. 내일은 권장 섭취(${snapshot.dailyPlan.recommendedIntake}kcal)와 만회 운동(${snapshot.weeklyRollup.adjustedExerciseTargetToday}kcal)을 타협 없이 수행하십시오."
            )
        }

        // 3. Missing meal logs
        val missingMealDays = sortedDays.count { it.isMissingMealFailure }
        if (missingMealDays > 0) {
            violations += AdviceViolation(
                code = "MISSING_MEAL_LOG",
                titleKo = "식사 기록 누락 (${missingMealDays}일)",
                severity = if (missingMealDays >= 2) ViolationSeverity.HIGH else ViolationSeverity.MEDIUM,
                detailKo = "최근 7일 중 ${missingMealDays}일의 식사 기록이 없습니다. 기록하지 않은 날은 무조건 실패(빨강)로 처리됩니다."
            )
        }

        // 4. Weekly cumulative negative
        if (snapshot.weeklyRollup.cumulativeBalanceKcal < 0) {
            violations += AdviceViolation(
                code = "WEEKLY_DEFICIT_SHORTFALL",
                titleKo = "주간 누적 칼로리 적자 미달 (${snapshot.weeklyRollup.cumulativeBalanceKcal}kcal)",
                severity = ViolationSeverity.MEDIUM,
                detailKo = "이번 주 누적 수지가 ${snapshot.weeklyRollup.cumulativeBalanceKcal}kcal입니다. 남은 ${snapshot.weeklyRollup.remainingDaysInPeriod}일 동안 하루 ${snapshot.weeklyRollup.recoveryPerDayKcal}kcal씩 만회해야 주간 목표를 맞춥니다."
            )
        }

        // 5. Exercise target shortfall
        val lowExerciseDays = sortedDays.count {
            it.hasMealLogged && snapshot.dailyPlan.exerciseTarget > 0 &&
                it.exerciseKcal < (snapshot.dailyPlan.exerciseTarget * 0.5)
        }
        if (lowExerciseDays >= 3) {
            violations += AdviceViolation(
                code = "LOW_EXERCISE",
                titleKo = "운동 소모량 절대 부족 (${lowExerciseDays}일 미달)",
                severity = ViolationSeverity.MEDIUM,
                detailKo = "기초대사량(${snapshot.bmrKcal}kcal) 하한 때문에 식단만으로는 목표 적자(${snapshot.dailyPlan.deficitTarget}kcal)를 채울 수 없습니다. 하루 최소 ${snapshot.dailyPlan.exerciseTarget}kcal 이상 운동하십시오."
            )
        }

        // 6. Alcohol frequency / calories
        if (snapshot.weeklyAlcoholDays >= 2 || snapshot.weeklyAlcoholKcal >= 600) {
            violations += AdviceViolation(
                code = "ALCOHOL_EXCESS",
                titleKo = "주간 음주 과다 (${snapshot.weeklyAlcoholDays}회, ${snapshot.weeklyAlcoholKcal}kcal)",
                severity = if (snapshot.weeklyAlcoholKcal >= 1000) ViolationSeverity.HIGH else ViolationSeverity.MEDIUM,
                detailKo = "이번 주 술로만 ${snapshot.weeklyAlcoholKcal}kcal를 섭취했습니다. 내장지방 레벨(${snapshot.visceralFatLevel})을 낮추려면 음주 빈도를 주 1회 이하로 제한하십시오."
            )
        }

        // 7. Protein deficiency
        val targetProteinGrams = (snapshot.leanBodyMassKg * 1.6).roundToInt().coerceAtLeast(90)
        val loggedMealDays = sortedDays.count { it.hasMealLogged }
        if (loggedMealDays >= 2 && snapshot.avgProteinGramsLast7 < (targetProteinGrams * 0.8)) {
            violations += AdviceViolation(
                code = "LOW_PROTEIN",
                titleKo = "단백질 섭취 부족 (평균 ${snapshot.avgProteinGramsLast7}g / 권장 ${targetProteinGrams}g)",
                severity = ViolationSeverity.MEDIUM,
                detailKo = "골격근량(기준 38.5kg) 방어를 위해 제지방량 기준 하루 ${targetProteinGrams}g의 단백질이 필요하지만 현재 평균 ${snapshot.avgProteinGramsLast7}g에 그칩니다."
            )
        }

        // 8. Skeletal muscle loss
        if (snapshot.stageResult.skeletalMuscleLossWarning) {
            val deltaStr = String.format("%.1f", snapshot.stageResult.skeletalMuscleDeltaKg)
            violations += AdviceViolation(
                code = "MUSCLE_LOSS",
                titleKo = "골격근 손실 경고 (${deltaStr}kg)",
                severity = ViolationSeverity.HIGH,
                detailKo = "현재 골격근량이 ${snapshot.skeletalMuscleKg}kg으로 기준(38.5kg) 대비 ${deltaStr}kg 감소했습니다. 근력 운동을 주 3회 이상 실시하고 단백질 섭취를 즉시 늘리십시오."
            )
        }

        // 9. Weight log missing >= 3 days
        val daysSinceWeight = snapshot.lastWeightLogDate?.let {
            ChronoUnit.DAYS.between(it, snapshot.today).toInt().coerceAtLeast(0)
        } ?: 99
        if (daysSinceWeight >= 3) {
            violations += AdviceViolation(
                code = "WEIGHT_LOG_STALE",
                titleKo = "일일 체중 ${daysSinceWeight}일째 미측정",
                severity = ViolationSeverity.MEDIUM,
                detailKo = "최근 ${daysSinceWeight}일 동안 체중 기록이 없습니다. 매일 아침 공복 체중을 입력하거나 Health Connect에서 동기화하십시오."
            )
        }

        // 10. InBody stale >= 8 days
        val daysSinceInBody = snapshot.lastInBodyDate?.let {
            ChronoUnit.DAYS.between(it, snapshot.today).toInt().coerceAtLeast(0)
        } ?: 99
        if (daysSinceInBody >= 8) {
            violations += AdviceViolation(
                code = "INBODY_STALE",
                titleKo = "InBody 주간 갱신 경과 (${daysSinceInBody}일 경과)",
                severity = ViolationSeverity.LOW,
                detailKo = "마지막 InBody 측정 후 ${daysSinceInBody}일이 지났습니다. 최신 체지방량과 내장지방 레벨을 반영하도록 InBody를 측정하고 스크린샷을 등록하십시오."
            )
        }

        // 11. Undereating below BMR >= 3 days
        val underBmrDays = sortedDays.count { it.hasMealLogged && it.totalIntakeKcal in 1 until snapshot.bmrKcal }
        if (underBmrDays >= 3) {
            violations += AdviceViolation(
                code = "UNDEREATING_BELOW_BMR",
                titleKo = "기초대사량 미만 과도한 절식 (${underBmrDays}일)",
                severity = ViolationSeverity.MEDIUM,
                detailKo = "기초대사량(${snapshot.bmrKcal}kcal) 미만 섭취가 ${underBmrDays}일 발생했습니다. 과도한 절식은 골격근(38.5kg) 손실과 대사 저하를 부릅니다. 최소 ${snapshot.bmrKcal}kcal는 섭취하고 운동으로 적자를 만드십시오."
            )
        }

        val totalPenalty = violations.sumOf { it.severity.weight }
        val grade = when {
            totalPenalty == 0 -> "A"
            totalPenalty <= 2 -> "B"
            totalPenalty <= 4 -> "C"
            totalPenalty <= 7 -> "D"
            else -> "F"
        }

        val todayEval = sortedDays.lastOrNull { it.date == snapshot.today }
        val dailyBalance = todayEval?.netBalanceKcal ?: 0

        val headline = buildHeadline(grade, violations, snapshot)
        val bodyLines = buildBodyLines(grade, violations, snapshot, requiredPaceKgPerWeek, actualPaceKgPerWeek)
        val actions = buildTomorrowActions(violations, snapshot, targetProteinGrams)

        return AdviceVerdict(
            dateIso = snapshot.today.toString(),
            strictness = snapshot.strictness.name,
            grade = grade,
            stageNumber = stage.number,
            currentWeightKg = snapshot.currentWeightKg,
            targetWeightKg = stage.targetWeightMidKg,
            visceralFatLevel = snapshot.visceralFatLevel,
            skeletalMuscleKg = snapshot.skeletalMuscleKg,
            skeletalMuscleDeltaKg = snapshot.stageResult.skeletalMuscleDeltaKg,
            bmrKcal = snapshot.bmrKcal,
            dailyBalanceKcal = dailyBalance,
            weeklyCumulativeKcal = snapshot.weeklyRollup.cumulativeBalanceKcal,
            monthlyCumulativeKcal = snapshot.monthlyRollup.cumulativeBalanceKcal,
            recoveryPerDayKcal = snapshot.weeklyRollup.recoveryPerDayKcal,
            actualPaceKgPerWeek = actualPaceKgPerWeek,
            requiredPaceKgPerWeek = requiredPaceKgPerWeek,
            weeklyAlcoholKcal = snapshot.weeklyAlcoholKcal,
            weeklyAlcoholDays = snapshot.weeklyAlcoholDays,
            avgProteinGrams = snapshot.avgProteinGramsLast7,
            targetProteinGrams = targetProteinGrams,
            avgExerciseKcal = snapshot.avgExerciseKcalLast7,
            targetExerciseKcal = snapshot.dailyPlan.exerciseTarget,
            consecutiveRedDays = consecutiveRed,
            missingMealDaysLast7 = missingMealDays,
            daysSinceLastWeightLog = daysSinceWeight,
            daysSinceLastInBody = daysSinceInBody,
            bmi = snapshot.bmi,
            age = snapshot.age,
            zone2HrMin = snapshot.zone2HrRange.first,
            zone2HrMax = snapshot.zone2HrRange.last,
            violations = violations,
            headlineKo = headline,
            bodyLinesKo = bodyLines,
            actionsTomorrowKo = actions
        )
    }

    private fun buildHeadline(
        grade: String,
        violations: List<AdviceViolation>,
        snapshot: AdviceInputSnapshot
    ): String {
        val top = violations.maxByOrNull { it.severity.weight }
        return when (snapshot.strictness) {
            AdviceStrictness.VERY_STRICT -> if (top != null) {
                "[등급 $grade] 변명 불가: ${top.titleKo} — 즉시 교정 필요"
            } else {
                "[등급 A] 기준 충족. 방심하지 말고 현재 페이스를 유지하십시오."
            }
            AdviceStrictness.STRICT -> if (top != null) {
                "[등급 $grade] 경고: ${top.titleKo}"
            } else {
                "[등급 A] 오늘과 주간 목표 수치를 정확히 지켰습니다."
            }
            AdviceStrictness.NORMAL -> if (top != null) {
                "[등급 $grade] 점검 필요: ${top.titleKo}"
            } else {
                "[등급 A] 훌륭합니다! 목표 페이스를 잘 유지하고 있습니다."
            }
        }
    }

    private fun buildBodyLines(
        grade: String,
        violations: List<AdviceViolation>,
        snapshot: AdviceInputSnapshot,
        requiredPace: Double,
        actualPace: Double
    ): List<String> {
        val lines = mutableListOf<String>()
        val stage = snapshot.stageResult.activeStage
        lines += String.format(
            "현재 체중 %.1fkg (BMI %.1f, 내장지방 %d레벨, 골격근 %.1fkg) · %s 목표 %.1fkg까지 %.1fkg 남았습니다.",
            snapshot.currentWeightKg,
            snapshot.bmi,
            snapshot.visceralFatLevel,
            snapshot.skeletalMuscleKg,
            "${stage.number}단계",
            stage.targetWeightMidKg,
            snapshot.stageResult.remainingKgToActiveStage
        )
        lines += String.format(
            "주간 누적 수지 %+dkcal, 월간 누적 수지 %+dkcal (기초대사량 %dkcal, 일일 권장 섭취 %dkcal, 일일 운동 목표 %dkcal).",
            snapshot.weeklyRollup.cumulativeBalanceKcal,
            snapshot.monthlyRollup.cumulativeBalanceKcal,
            snapshot.bmrKcal,
            snapshot.dailyPlan.recommendedIntake,
            snapshot.dailyPlan.exerciseTarget
        )
        if (violations.isEmpty()) {
            lines += "위반 항목이 없습니다. Zone 2 심박수(${snapshot.zone2HrRange.first}~${snapshot.zone2HrRange.last}bpm) 유산소와 근력 운동으로 골격근 38.5kg을 계속 방어하십시오."
        } else {
            violations.take(4).forEach { v ->
                lines += "${v.severity.badgeKo} ${v.detailKo}"
            }
        }
        return lines
    }

    private fun buildTomorrowActions(
        violations: List<AdviceViolation>,
        snapshot: AdviceInputSnapshot,
        targetProteinGrams: Int
    ): List<String> {
        val actions = mutableListOf<String>()
        val codes = violations.map { it.code }.toSet()

        if ("WEEKLY_DEFICIT_SHORTFALL" in codes || "CONSECUTIVE_RED" in codes) {
            actions += "내일 섭취 상한 ${snapshot.weeklyRollup.adjustedRecommendedIntakeToday}kcal 엄수 + 만회 포함 운동 ${snapshot.weeklyRollup.adjustedExerciseTargetToday}kcal 달성"
        } else {
            actions += "내일 권장 섭취 ${snapshot.dailyPlan.recommendedIntake}kcal 및 운동 ${snapshot.dailyPlan.exerciseTarget}kcal 달성"
        }

        if ("MISSING_MEAL_LOG" in codes) {
            actions += "아침·점심·저녁 3식 모두 사진 촬영 후 즉시 칼로리 기록 (21:00 전 완료 필수)"
        }

        if ("MUSCLE_LOSS" in codes || "LOW_PROTEIN" in codes) {
            actions += "단백질 최소 ${targetProteinGrams}g 섭취 및 대근육 근력 운동 40분 실시 (골격근 38.5kg 방어)"
        } else {
            actions += "Zone 2 유산소(${snapshot.zone2HrRange.first}~${snapshot.zone2HrRange.last}bpm) 40분 이상 + 단백질 ${targetProteinGrams}g 섭취"
        }

        if ("ALCOHOL_EXCESS" in codes) {
            actions += "이번 주 남은 기간 금주 (술 0kcal 유지)"
        } else if ("WEIGHT_LOG_STALE" in codes) {
            actions += "내일 기상 직후 공복 체중 측정 및 입력 (또는 Health Connect 수동 동기화)"
        } else if ("INBODY_STALE" in codes) {
            actions += "이번 주 내 InBody 측정 후 결과 화면 스크린샷 업로드"
        } else if (actions.size < 3) {
            actions += "기상 직후 공복 체중 기록 및 3식 사진 빠짐없이 등록"
        }

        return actions.take(3)
    }
}
