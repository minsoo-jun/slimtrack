package com.minsoo.slimtrack.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

enum class DayStatusColor {
    GREEN,   // Target achieved (balance >= 0 and meal logged)
    YELLOW,  // Within tolerance (-tolerance <= balance < 0 and meal logged)
    RED,     // Exceeded target (balance < -tolerance) OR missing meal log on past/today
    GRAY     // Future date
}

data class DailyPlan(
    val bmr: Int,
    val activityFactor: Double,
    val tdee: Int,
    val deficitTarget: Int,
    val recommendedIntake: Int,
    val exerciseTarget: Int,
    val targetProteinGrams: Int = 108
)

data class DayRecordInput(
    val date: LocalDate,
    val mealIntakeKcal: Int,
    val drinkIntakeKcal: Int,
    val exerciseKcal: Int,
    val mealCount: Int,
    val proteinGrams: Float = 0f
) {
    val totalIntakeKcal: Int get() = mealIntakeKcal + drinkIntakeKcal
    val hasMealLogged: Boolean get() = mealCount > 0
}

data class DayEvaluation(
    val date: LocalDate,
    val plan: DailyPlan,
    val totalIntakeKcal: Int,
    val mealIntakeKcal: Int,
    val drinkIntakeKcal: Int,
    val exerciseKcal: Int,
    val hasMealLogged: Boolean,
    val netBalanceKcal: Int,
    val statusColor: DayStatusColor,
    val isMissingMealFailure: Boolean,
    val proteinGrams: Float = 0f,
    val targetProteinGrams: Int = plan.targetProteinGrams
) {
    /**
     * Remaining calories that can still be consumed today without exceeding the target deficit.
     * Positive value means remaining allowance (displayed WITHOUT a '+' sign, e.g., "713").
     * Negative value means calories exceeded (displayed with '-', e.g., "-240").
     */
    val remainingIntakeKcal: Int get() = netBalanceKcal

    val remainingProteinGrams: Float get() = (targetProteinGrams - proteinGrams).coerceAtLeast(0f)
    val proteinAchievementPct: Int get() = if (targetProteinGrams > 0) ((proteinGrams / targetProteinGrams) * 100f).roundToInt() else 0
}

data class PeriodRollup(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val cumulativeBalanceKcal: Int,
    val remainingDaysInPeriod: Int,
    val recoveryPerDayKcal: Int,
    val adjustedRecommendedIntakeToday: Int,
    val adjustedExerciseTargetToday: Int
)

object CalorieEngine {
    const val KCAL_PER_KG_FAT = 7700.0
    const val MIN_DAILY_DEFICIT = 300
    const val MAX_DAILY_DEFICIT = 1000
    const val DEFAULT_TOLERANCE_KCAL = 150
    const val BASELINE_STEPS_INCLUDED_IN_TDEE = 4000

    /**
     * Method A (방식 A): Basic everyday steps up to 4,000 steps are already included in TDEE (BMR * 1.2).
     * Steps above 4,000 earn bonus exercise calories proportional to body weight
     * (~45 kcal per 1,000 steps at 98kg -> 0.00046 * weightKg kcal per step).
     */
    fun computeBonusStepsKcal(
        totalSteps: Int,
        weightKg: Double,
        baselineSteps: Int = BASELINE_STEPS_INCLUDED_IN_TDEE
    ): Int {
        val bonusSteps = (totalSteps - baselineSteps).coerceAtLeast(0)
        if (bonusSteps == 0) return 0
        val kcalPerStep = (weightKg.coerceAtLeast(40.0) * 0.00046)
        return (bonusSteps * kcalPerStep).roundToInt()
    }

    /**
     * Computes daily required calorie deficit from current weight to stage target weight
     * over remaining days, clamped to safe pace (300..1000 kcal/day).
     */
    fun dailyDeficitTarget(
        currentWeightKg: Double,
        targetWeightKg: Double,
        daysLeft: Int
    ): Int {
        val kgToLose = (currentWeightKg - targetWeightKg).coerceAtLeast(0.0)
        if (kgToLose <= 0.0) return MIN_DAILY_DEFICIT
        val safeDays = daysLeft.coerceAtLeast(1)
        val raw = (kgToLose * KCAL_PER_KG_FAT / safeDays).roundToInt()
        return raw.coerceIn(MIN_DAILY_DEFICIT, MAX_DAILY_DEFICIT)
    }

    fun dailyDeficitTarget(
        currentWeightKg: Double,
        targetWeightKg: Double,
        today: LocalDate,
        deadline: LocalDate
    ): Int {
        val daysLeft = ChronoUnit.DAYS.between(today, deadline).toInt().coerceAtLeast(1)
        return dailyDeficitTarget(currentWeightKg, targetWeightKg, daysLeft)
    }

    /**
     * Computes daily recommended intake (never below BMR) and any extra exercise target
     * needed when TDEE - deficit would drop below BMR.
     */
    fun dailyPlan(
        bmr: Int,
        activityFactor: Double,
        deficitTarget: Int,
        targetProteinGrams: Int = 108
    ): DailyPlan {
        val tdee = (bmr * activityFactor).roundToInt()
        val recommendedIntake = maxOf(bmr, tdee - deficitTarget)
        val dietDeficit = (tdee - recommendedIntake).coerceAtLeast(0)
        val exerciseTarget = (deficitTarget - dietDeficit).coerceAtLeast(0)
        return DailyPlan(
            bmr = bmr,
            activityFactor = activityFactor,
            tdee = tdee,
            deficitTarget = deficitTarget,
            recommendedIntake = recommendedIntake,
            exerciseTarget = exerciseTarget,
            targetProteinGrams = targetProteinGrams
        )
    }

    /**
     * Remaining intakeable calories for the day:
     * Formula: recommendedIntake - totalIntakeKcal + exerciseKcal
     * Example: Recommended 1834 kcal - Actual Intake 1055 kcal + Exercise 158 kcal = 937 kcal remaining.
     * Positive means calories still available to eat today (displayed without '+' sign).
     * Negative means calories exceeded today (displayed with '-' sign).
     */
    fun balance(
        plan: DailyPlan,
        totalIntakeKcal: Int,
        exerciseKcal: Int
    ): Int {
        return plan.recommendedIntake - totalIntakeKcal + exerciseKcal
    }

    fun evaluateDay(
        input: DayRecordInput,
        plan: DailyPlan,
        today: LocalDate,
        toleranceKcal: Int = DEFAULT_TOLERANCE_KCAL,
        missingMealAsFailure: Boolean = true,
        effectiveStartDate: LocalDate? = null,
        targetProteinGrams: Int = plan.targetProteinGrams
    ): DayEvaluation {
        if (input.date.isAfter(today) || (effectiveStartDate != null && input.date.isBefore(effectiveStartDate))) {
            return DayEvaluation(
                date = input.date,
                plan = plan,
                totalIntakeKcal = input.drinkIntakeKcal,
                mealIntakeKcal = 0,
                drinkIntakeKcal = input.drinkIntakeKcal,
                exerciseKcal = 0,
                hasMealLogged = false,
                netBalanceKcal = 0,
                statusColor = DayStatusColor.GRAY,
                isMissingMealFailure = false,
                proteinGrams = input.proteinGrams,
                targetProteinGrams = targetProteinGrams
            )
        }

        val net = balance(plan, input.totalIntakeKcal, input.exerciseKcal)

        if (!input.hasMealLogged) {
            val isPastDayMissingMeal = missingMealAsFailure && input.date.isBefore(today)
            val color = when {
                isPastDayMissingMeal -> DayStatusColor.RED
                input.date == today -> classifyBalanceColor(net, toleranceKcal)
                else -> DayStatusColor.GRAY
            }
            return DayEvaluation(
                date = input.date,
                plan = plan,
                totalIntakeKcal = input.totalIntakeKcal,
                mealIntakeKcal = input.mealIntakeKcal,
                drinkIntakeKcal = input.drinkIntakeKcal,
                exerciseKcal = input.exerciseKcal,
                hasMealLogged = false,
                netBalanceKcal = net,
                statusColor = color,
                isMissingMealFailure = isPastDayMissingMeal,
                proteinGrams = input.proteinGrams,
                targetProteinGrams = targetProteinGrams
            )
        }

        val color = classifyBalanceColor(net, toleranceKcal)
        return DayEvaluation(
            date = input.date,
            plan = plan,
            totalIntakeKcal = input.totalIntakeKcal,
            mealIntakeKcal = input.mealIntakeKcal,
            drinkIntakeKcal = input.drinkIntakeKcal,
            exerciseKcal = input.exerciseKcal,
            hasMealLogged = true,
            netBalanceKcal = net,
            statusColor = color,
            isMissingMealFailure = false,
            proteinGrams = input.proteinGrams,
            targetProteinGrams = targetProteinGrams
        )
    }

    fun classifyBalanceColor(
        netBalanceKcal: Int,
        toleranceKcal: Int = DEFAULT_TOLERANCE_KCAL
    ): DayStatusColor = when {
        netBalanceKcal >= 0 -> DayStatusColor.GREEN
        netBalanceKcal >= -toleranceKcal -> DayStatusColor.YELLOW
        else -> DayStatusColor.RED
    }

    /**
     * When cumulative balance is negative (e.g. -900 kcal) and 3 days remain in the period,
     * returns +300 kcal/day that must be recovered per remaining day.
     */
    fun recoveryPerDay(cumulativeBalanceKcal: Int, remainingDaysInPeriod: Int): Int {
        if (cumulativeBalanceKcal >= 0 || remainingDaysInPeriod <= 0) return 0
        return ((-cumulativeBalanceKcal).toDouble() / remainingDaysInPeriod).roundToInt()
    }

    fun computeWeeklyRollup(
        today: LocalDate,
        evaluations: List<DayEvaluation>,
        todayPlan: DailyPlan,
        effectiveStartDate: LocalDate? = null
    ): PeriodRollup {
        val rawWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStart = if (effectiveStartDate != null && effectiveStartDate.isAfter(rawWeekStart)) {
            effectiveStartDate
        } else {
            rawWeekStart
        }
        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return computePeriodRollup(weekStart, weekEnd, today, evaluations, todayPlan)
    }

    fun computeMonthlyRollup(
        today: LocalDate,
        evaluations: List<DayEvaluation>,
        todayPlan: DailyPlan,
        effectiveStartDate: LocalDate? = null
    ): PeriodRollup {
        val ym = YearMonth.from(today)
        val rawMonthStart = ym.atDay(1)
        val monthStart = if (effectiveStartDate != null && effectiveStartDate.isAfter(rawMonthStart)) {
            effectiveStartDate
        } else {
            rawMonthStart
        }
        val monthEnd = ym.atEndOfMonth()
        return computePeriodRollup(monthStart, monthEnd, today, evaluations, todayPlan)
    }

    private fun computePeriodRollup(
        startDate: LocalDate,
        endDate: LocalDate,
        today: LocalDate,
        evaluations: List<DayEvaluation>,
        todayPlan: DailyPlan
    ): PeriodRollup {
        val loggedDaysUpToToday = evaluations.filter {
            !it.date.isBefore(startDate) && !it.date.isAfter(today) && (it.hasMealLogged || it.drinkIntakeKcal > 0)
        }
        val cumulative = loggedDaysUpToToday.sumOf { it.netBalanceKcal }
        val remainingDays = (ChronoUnit.DAYS.between(today, endDate).toInt() + 1).coerceAtLeast(1)
        val recovery = recoveryPerDay(cumulative, remainingDays)

        // Never drop recommended intake below BMR; shift any overflow recovery into exerciseTarget
        val idealReducedIntake = todayPlan.recommendedIntake - recovery
        val adjustedIntake = maxOf(todayPlan.bmr, idealReducedIntake)
        val intakeReduction = todayPlan.recommendedIntake - adjustedIntake
        val extraExercise = (recovery - intakeReduction).coerceAtLeast(0)
        val adjustedExercise = todayPlan.exerciseTarget + extraExercise

        return PeriodRollup(
            startDate = startDate,
            endDate = endDate,
            cumulativeBalanceKcal = cumulative,
            remainingDaysInPeriod = remainingDays,
            recoveryPerDayKcal = recovery,
            adjustedRecommendedIntakeToday = adjustedIntake,
            adjustedExerciseTargetToday = adjustedExercise
        )
    }
}
