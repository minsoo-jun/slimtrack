package com.minsoo.slimtrack.domain

import java.time.LocalDate

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
            "체중 5kg가 줄어드는 과정에서 대부분의 감량분이 체지방에서 소모되도록 유도하면, 내장지방 레벨도 자연스럽게 두 단계 낮아져 '표준' 경계선(10 Level)에 안착할 수 있습니다."
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
            "유산소 운동(Zone 2 저강도 유산소 등) 비중을 꾸준히 유지하면서, 근력 운동을 통해 소중한 골격근량(38.5kg)이 손실되지 않도록 방어하는 것이 핵심입니다."
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

data class StageEvaluationResult(
    val activeStage: GoalStage,
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

    fun evaluate(
        currentWeightKg: Double,
        visceralFatLevel: Int,
        skeletalMuscleKg: Double,
        startWeightKg: Double = DEFAULT_START_WEIGHT_KG,
        muscleBaselineKg: Double = DEFAULT_MUSCLE_BASELINE_KG,
        startDate: LocalDate = LocalDate.of(2026, 11, 9)
    ): StageEvaluationResult {
        val stage1Done = currentWeightKg <= GoalStage.STAGE_1_SHORT.targetWeightMaxKg &&
            visceralFatLevel <= GoalStage.STAGE_1_SHORT.targetVisceralFatMax
        val stage2Done = currentWeightKg <= GoalStage.STAGE_2_MID.targetWeightMaxKg &&
            visceralFatLevel <= GoalStage.STAGE_2_MID.targetVisceralFatMax
        val stage3Done = currentWeightKg <= GoalStage.STAGE_3_LONG.targetWeightMaxKg &&
            visceralFatLevel <= GoalStage.STAGE_3_LONG.targetVisceralFatMax

        val activeStage = when {
            !stage1Done -> GoalStage.STAGE_1_SHORT
            !stage2Done -> GoalStage.STAGE_2_MID
            else -> GoalStage.STAGE_3_LONG
        }

        val stageStartWeight = when (activeStage) {
            GoalStage.STAGE_1_SHORT -> startWeightKg
            GoalStage.STAGE_2_MID -> GoalStage.STAGE_1_SHORT.targetWeightMidKg
            GoalStage.STAGE_3_LONG -> GoalStage.STAGE_2_MID.targetWeightMidKg
        }

        val totalSpan = (stageStartWeight - activeStage.targetWeightMidKg).coerceAtLeast(0.5)
        val lostInStage = (stageStartWeight - currentWeightKg).coerceAtLeast(0.0)
        val progress = (lostInStage / totalSpan).toFloat().coerceIn(0f, 1f)

        val remainingKg = (currentWeightKg - activeStage.targetWeightMidKg).coerceAtLeast(0.0)
        val visceralDelta = (visceralFatLevel - activeStage.targetVisceralFatMax).coerceAtLeast(0)

        val muscleDelta = skeletalMuscleKg - muscleBaselineKg
        val muscleWarning = muscleDelta < -MUSCLE_LOSS_THRESHOLD_KG

        val deadline = when (activeStage) {
            GoalStage.STAGE_1_SHORT -> startDate.plusMonths(2) // 2027-01-09
            GoalStage.STAGE_2_MID -> startDate.plusMonths(6)   // 2027-05-09
            GoalStage.STAGE_3_LONG -> startDate.plusMonths(12) // 2027-11-09
        }

        return StageEvaluationResult(
            activeStage = activeStage,
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
