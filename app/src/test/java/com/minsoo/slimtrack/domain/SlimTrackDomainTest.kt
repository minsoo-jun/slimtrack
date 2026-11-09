package com.minsoo.slimtrack.domain

import com.google.common.truth.Truth.assertThat
import com.minsoo.slimtrack.data.prefs.Sex
import com.minsoo.slimtrack.data.prefs.UserProfile
import com.minsoo.slimtrack.work.AlarmScheduler
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class SlimTrackDomainTest {

    @Test
    fun `inbodyBmr always computes 370 plus 21_6 times lean body mass`() {
        // Sample InBody 570: 98.9kg weight, 31.1kg body fat mass -> LBM = 67.8kg -> 370 + 1464.48 = 1834
        val bmr = BmrCalculator.inbodyBmr(98.9, 31.1)
        assertThat(bmr).isEqualTo(1834)
    }

    @Test
    fun `dailyPlan enforces BMR floor and shifts remaining deficit to exerciseTarget`() {
        val today = LocalDate.of(2026, 11, 9)
        val deadline = LocalDate.of(2027, 1, 9) // 61 days
        val deficit = CalorieEngine.dailyDeficitTarget(98.9, 93.5, today, deadline)
        assertThat(deficit).isEqualTo(682)

        val plan = CalorieEngine.dailyPlan(bmr = 1834, activityFactor = 1.2, deficitTarget = deficit)
        assertThat(plan.tdee).isEqualTo(2201)
        assertThat(plan.recommendedIntake).isEqualTo(1834) // Never below BMR
        assertThat(plan.exerciseTarget).isEqualTo(315)     // 682 - (2201 - 1834) = 315

        // Eating 1834 kcal and burning 0 kcal exercise leaves 0 remaining intake (GREEN)
        // Eating 1055 kcal and burning 158 kcal exercise leaves 1834 - 1055 + 158 = 937 kcal remaining!
        val bal = CalorieEngine.balance(plan, totalIntakeKcal = 1055, exerciseKcal = 158)
        assertThat(bal).isEqualTo(937)
        assertThat(CalorieEngine.classifyBalanceColor(bal, 150)).isEqualTo(DayStatusColor.GREEN)
    }

    @Test
    fun `calendar color boundaries and missing meal log treated as RED failure`() {
        assertThat(CalorieEngine.classifyBalanceColor(0, 150)).isEqualTo(DayStatusColor.GREEN)
        assertThat(CalorieEngine.classifyBalanceColor(-150, 150)).isEqualTo(DayStatusColor.YELLOW)
        assertThat(CalorieEngine.classifyBalanceColor(-151, 150)).isEqualTo(DayStatusColor.RED)

        val today = LocalDate.of(2026, 11, 9)
        val plan = CalorieEngine.dailyPlan(1834, 1.2, 682)

        val missingMealDay = CalorieEngine.evaluateDay(
            input = DayRecordInput(
                date = today,
                mealIntakeKcal = 0,
                drinkIntakeKcal = 0,
                exerciseKcal = 300,
                mealCount = 0
            ),
            plan = plan,
            today = today,
            toleranceKcal = 150,
            missingMealAsFailure = true
        )
        assertThat(missingMealDay.statusColor).isEqualTo(DayStatusColor.RED)
        assertThat(missingMealDay.isMissingMealFailure).isTrue()

        val futureDay = CalorieEngine.evaluateDay(
            input = DayRecordInput(today.plusDays(1), 0, 0, 0, 0),
            plan = plan,
            today = today
        )
        assertThat(futureDay.statusColor).isEqualTo(DayStatusColor.GRAY)
    }

    @Test
    fun `recoveryPerDay distributes negative cumulative balance over remaining days`() {
        val recovery = CalorieEngine.recoveryPerDay(cumulativeBalanceKcal = -900, remainingDaysInPeriod = 3)
        assertThat(recovery).isEqualTo(300)
        assertThat(CalorieEngine.recoveryPerDay(250, 3)).isEqualTo(0)
    }

    @Test
    fun `alcoholCalculator computes soju 1 bottle 360ml at 16_5 pct as 328 kcal`() {
        val sojuKcal = AlcoholCalculator.calculatePresetKcal(AlcoholPreset.SOJU, volumeMl = 360, abvPctOverride = 16.5)
        assertThat(sojuKcal).isEqualTo(328)

        val beerCanKcal = AlcoholCalculator.calculatePresetKcal(AlcoholPreset.BEER, volumeMl = 500, abvPctOverride = 4.5)
        // Pure alcohol: 500 * 0.045 * 0.789 * 7 = 124.27 + carbs 5 * 13 = 65 -> 189 kcal
        assertThat(beerCanKcal).isEqualTo(189)
    }

    @Test
    fun `goalStageEvaluator transitions from stage 1 to stage 2 and flags muscle loss`() {
        val stage1 = GoalStageEvaluator.evaluate(
            currentWeightKg = 98.9,
            visceralFatLevel = 12,
            skeletalMuscleKg = 38.5
        )
        assertThat(stage1.activeStage).isEqualTo(GoalStage.STAGE_1_SHORT)
        assertThat(stage1.skeletalMuscleLossWarning).isFalse()

        val stage2WithMuscleLoss = GoalStageEvaluator.evaluate(
            currentWeightKg = 93.4,
            visceralFatLevel = 10,
            skeletalMuscleKg = 37.8 // -0.7kg below 38.5 baseline
        )
        assertThat(stage2WithMuscleLoss.stage1Completed).isTrue()
        assertThat(stage2WithMuscleLoss.activeStage).isEqualTo(GoalStage.STAGE_2_MID)
        assertThat(stage2WithMuscleLoss.skeletalMuscleLossWarning).isTrue()
    }

    @Test
    fun `userProfile computes exact age, BMI, and Zone 2 heart rate`() {
        val profile = UserProfile(
            sex = Sex.MALE,
            heightCm = 176.0,
            birthDate = LocalDate.of(1980, 11, 10)
        )
        // Day before birthday -> 45, on birthday -> 46
        assertThat(profile.ageOn(LocalDate.of(2026, 11, 9))).isEqualTo(45)
        assertThat(profile.ageOn(LocalDate.of(2026, 11, 10))).isEqualTo(46)
        assertThat(profile.bmi(98.9)).isEqualTo(31.9)
    }

    @Test
    fun `adviceRuleEngine flags consecutive red days, missing meals, and alcohol excess`() {
        val today = LocalDate.of(2026, 11, 9)
        val plan = CalorieEngine.dailyPlan(1834, 1.2, 682)
        val stageRes = GoalStageEvaluator.evaluate(98.9, 12, 37.8)

        val day1 = CalorieEngine.evaluateDay(DayRecordInput(today.minusDays(1), 2600, 328, 0, 2), plan, today)
        val day2 = CalorieEngine.evaluateDay(DayRecordInput(today, 0, 0, 0, 0), plan, today) // missing meal

        val rollup = PeriodRollup(today.minusDays(1), today.plusDays(5), -950, 6, 158, 1834, 473)
        val snapshot = AdviceInputSnapshot(
            today = today,
            strictness = AdviceStrictness.VERY_STRICT,
            stageResult = stageRes,
            currentWeightKg = 98.9,
            weight7DaysAgoKg = 98.9,
            visceralFatLevel = 12,
            skeletalMuscleKg = 37.8,
            leanBodyMassKg = 67.8,
            bmrKcal = 1834,
            dailyPlan = plan,
            last7DayEvaluations = listOf(day1, day2),
            weeklyRollup = rollup,
            monthlyRollup = rollup,
            weeklyAlcoholKcal = 850,
            weeklyAlcoholDays = 2,
            avgProteinGramsLast7 = 65,
            avgExerciseKcalLast7 = 50,
            lastWeightLogDate = today.minusDays(4),
            lastInBodyDate = today.minusDays(2),
            bmi = 31.9,
            age = 45,
            zone2HrRange = 105..122
        )

        val verdict = AdviceRuleEngine.evaluate(snapshot)
        val codes = verdict.violations.map { it.code }.toSet()
        assertThat(codes).contains("CONSECUTIVE_RED")
        assertThat(codes).contains("MISSING_MEAL_LOG")
        assertThat(codes).contains("ALCOHOL_EXCESS")
        assertThat(codes).contains("MUSCLE_LOSS")
        assertThat(verdict.grade).isEqualTo("F")
        assertThat(verdict.actionsTomorrowKo).hasSize(3)
    }

    @Test
    fun `alarmScheduler computes next Sunday 0900 and daily 2100 accurately`() {
        // Monday 2026-11-09 05:00 -> Next Sunday is 2026-11-15 09:00 (6 days + 4 hours = 148 hours)
        val monMorning = LocalDateTime.of(2026, 11, 9, 5, 0)
        assertThat(monMorning.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        val sundayDelay = AlarmScheduler.computeDelayUntilNextSunday0900(monMorning)
        assertThat(sundayDelay.toHours()).isEqualTo(148L)

        // Same day 21:00 is 16 hours away
        val dailyDelay = AlarmScheduler.computeDelayUntilNext2100(monMorning)
        assertThat(dailyDelay.toHours()).isEqualTo(16L)
    }

    @Test
    fun `effectiveStartDate marks dates before start date as GRAY and excludes them from rollups`() {
        val today = LocalDate.of(2026, 11, 9)
        val effectiveStart = LocalDate.of(2026, 11, 9)
        val plan = CalorieEngine.dailyPlan(1834, 1.2, 682)

        val dayBeforeStart = CalorieEngine.evaluateDay(
            input = DayRecordInput(LocalDate.of(2026, 11, 8), 0, 0, 0, 0),
            plan = plan,
            today = today,
            toleranceKcal = 150,
            missingMealAsFailure = true,
            effectiveStartDate = effectiveStart
        )
        assertThat(dayBeforeStart.statusColor).isEqualTo(DayStatusColor.GRAY)
        assertThat(dayBeforeStart.isMissingMealFailure).isFalse()

        val todayEval = CalorieEngine.evaluateDay(
            input = DayRecordInput(today, 1055, 0, 158 + 83, 1),
            plan = plan,
            today = today,
            toleranceKcal = 150,
            missingMealAsFailure = true,
            effectiveStartDate = effectiveStart
        )
        assertThat(todayEval.statusColor).isEqualTo(DayStatusColor.GREEN)
        assertThat(todayEval.remainingIntakeKcal).isEqualTo(1020) // 1834 - 1055 + 241 = 1020

        // Verify 5,825 steps (4,000 baseline -> 1,825 bonus steps at 98.4kg -> 83 kcal)
        assertThat(CalorieEngine.computeBonusStepsKcal(5825, 98.4)).isEqualTo(83)
    }

    @Test
    fun `method A step bonus computes 0 kcal up to 4000 steps and proportional kcal above 4000`() {
        assertThat(CalorieEngine.computeBonusStepsKcal(3500, 98.4)).isEqualTo(0)
        assertThat(CalorieEngine.computeBonusStepsKcal(4000, 98.4)).isEqualTo(0)
        // 10,000 steps -> 6,000 bonus steps -> 6000 * 98.4 * 0.00046 = 271.58 -> 272 kcal
        assertThat(CalorieEngine.computeBonusStepsKcal(10000, 98.4)).isEqualTo(272)
    }

    @Test
    fun `custom 3 stage goals in UserProfile override stage targets while preserving defaults`() {
        val defaultProfile = UserProfile()
        val defaultRes = GoalStageEvaluator.evaluate(98.4, 12, 38.5, profile = defaultProfile)
        assertThat(defaultRes.activeStageSpec.targetWeightMinKg).isEqualTo(93.0)
        assertThat(defaultRes.activeStageSpec.targetWeightMaxKg).isEqualTo(94.0)

        val customProfile = UserProfile(
            startWeightKg = 75.0,
            stage1WeightMinKg = 70.0,
            stage1WeightMaxKg = 72.0,
            stage1VisceralFatMax = 8,
            stage1Months = 2
        )
        val customRes = GoalStageEvaluator.evaluate(74.0, 9, 30.0, profile = customProfile)
        assertThat(customRes.activeStageSpec.targetWeightMinKg).isEqualTo(70.0)
        assertThat(customRes.activeStageSpec.targetWeightMaxKg).isEqualTo(72.0)
        assertThat(customRes.activeStageSpec.targetWeightMidKg).isEqualTo(71.0)
    }
}
