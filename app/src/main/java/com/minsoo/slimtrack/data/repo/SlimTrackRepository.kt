package com.minsoo.slimtrack.data.repo

import com.minsoo.slimtrack.data.ai.GeminiClient
import com.minsoo.slimtrack.data.ai.InBodyOcrResponse
import com.minsoo.slimtrack.data.db.AdviceEntity
import com.minsoo.slimtrack.data.db.AppDao
import com.minsoo.slimtrack.data.db.AppDatabase
import com.minsoo.slimtrack.data.db.DrinkEntity
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.FoodItemEntry
import com.minsoo.slimtrack.data.db.InBodyEntity
import com.minsoo.slimtrack.data.db.MealEntity
import com.minsoo.slimtrack.data.db.MealType
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.data.health.HealthConnectRepository
import com.minsoo.slimtrack.data.health.HealthSyncSummary
import com.minsoo.slimtrack.data.prefs.AppSettings
import com.minsoo.slimtrack.data.prefs.SecretStore
import com.minsoo.slimtrack.data.prefs.SettingsStore
import com.minsoo.slimtrack.data.prefs.UserProfile
import com.minsoo.slimtrack.data.prefs.UserProfileStore
import com.minsoo.slimtrack.domain.AdviceInputSnapshot
import com.minsoo.slimtrack.domain.AdviceRuleEngine
import com.minsoo.slimtrack.domain.AdviceVerdict
import com.minsoo.slimtrack.domain.AlcoholDetector
import com.minsoo.slimtrack.domain.BmrCalculator
import com.minsoo.slimtrack.domain.CalorieEngine
import com.minsoo.slimtrack.domain.DailyPlan
import com.minsoo.slimtrack.domain.DayEvaluation
import com.minsoo.slimtrack.domain.DayRecordInput
import com.minsoo.slimtrack.domain.GoalStageEvaluator
import com.minsoo.slimtrack.domain.PeriodRollup
import com.minsoo.slimtrack.domain.StageEvaluationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class SaveMealResult(
    val meal: MealEntity?,
    val drinks: List<DrinkEntity>
)

data class CurrentBodyStatus(
    val currentWeightKg: Double,
    val currentWeightSource: String,
    val currentWeightDateIso: String,
    val latestInBody: InBodyEntity,
    val bmrKcal: Int,
    val leanBodyMassKg: Double,
    val bmi: Double,
    val stageResult: StageEvaluationResult,
    val dailyPlan: DailyPlan,
    val targetProteinGrams: Int = dailyPlan.targetProteinGrams
)

data class DashboardState(
    val today: LocalDate,
    val profile: UserProfile,
    val settings: AppSettings,
    val bodyStatus: CurrentBodyStatus,
    val monthEvaluations: Map<LocalDate, DayEvaluation>,
    val todayEvaluation: DayEvaluation,
    val weeklyRollup: PeriodRollup,
    val monthlyRollup: PeriodRollup,
    val weightloggedDates: Set<LocalDate>,
    val latestAdvice: AdviceEntity?,
    val liveVerdict: AdviceVerdict,
    val weightLogs: List<WeightLogEntity> = emptyList(),
    val inBodyHistory: List<InBodyEntity> = emptyList()
)

class SlimTrackRepository(
    val dao: AppDao,
    val profileStore: UserProfileStore,
    val settingsStore: SettingsStore,
    val secretStore: SecretStore,
    val healthConnectRepository: HealthConnectRepository,
    val geminiClient: GeminiClient
) {
    private val json = geminiClient.json

    suspend fun ensureSeedInBodyExists() {
        val latest = dao.getLatestInBody()
        if (latest == null) {
            dao.upsertInBody(AppDatabase.defaultSeedInBody())
        }
    }

    /**
     * Resolves current weight from the newer of (latest WeightLogEntity, latest InBodyEntity).
     * BMR is ALWAYS computed using the InBody formula: 370 + 21.6 * (weightKg - bodyFatMassKg)
     * from the latest InBody record.
     */
    fun resolveCurrentBodyStatus(
        latestInBody: InBodyEntity?,
        latestWeightLog: WeightLogEntity?,
        profile: UserProfile,
        today: LocalDate,
        customTargetProteinGrams: Int = 0
    ): CurrentBodyStatus {
        val inBody = latestInBody ?: AppDatabase.defaultSeedInBody()
        val useWeightLog = latestWeightLog != null &&
            (inBody.source == "SEED" || latestWeightLog.dateIso >= inBody.dateIso)

        val currentWeight = if (useWeightLog) {
            latestWeightLog!!.weightKg.toDouble()
        } else {
            inBody.weightKg.toDouble()
        }
        val weightSource = if (useWeightLog) {
            "일일 체중(${latestWeightLog!!.source})"
        } else {
            "InBody(${inBody.source})"
        }
        val weightDateIso = if (useWeightLog) {
            latestWeightLog!!.dateIso
        } else {
            inBody.dateIso
        }

        val bmr = BmrCalculator.inbodyBmr(
            weightKg = inBody.weightKg.toDouble(),
            bodyFatMassKg = inBody.bodyFatMassKg.toDouble()
        )
        val leanMass = (inBody.weightKg - inBody.bodyFatMassKg).toDouble().coerceAtLeast(20.0)
        val bmi = profile.bmi(currentWeight)

        val stageResult = GoalStageEvaluator.evaluate(
            currentWeightKg = currentWeight,
            visceralFatLevel = inBody.visceralFatLevel,
            skeletalMuscleKg = inBody.skeletalMuscleKg.toDouble(),
            startWeightKg = profile.startWeightKg,
            muscleBaselineKg = profile.muscleBaselineKg,
            startDate = profile.goalStartDate,
            profile = profile
        )

        val targetProtein = if (customTargetProteinGrams > 0) {
            customTargetProteinGrams
        } else {
            (leanMass * 1.6).roundToInt().coerceAtLeast(90)
        }

        val deficitTarget = CalorieEngine.dailyDeficitTarget(
            currentWeightKg = currentWeight,
            targetWeightKg = stageResult.activeStageSpec.targetWeightMidKg,
            today = today,
            deadline = stageResult.deadlineForActiveStage
        )
        val dailyPlan = CalorieEngine.dailyPlan(
            bmr = bmr,
            activityFactor = profile.activityFactor,
            deficitTarget = deficitTarget,
            targetProteinGrams = targetProtein
        )

        return CurrentBodyStatus(
            currentWeightKg = currentWeight,
            currentWeightSource = weightSource,
            currentWeightDateIso = weightDateIso,
            latestInBody = inBody.copy(bmrCalculated = bmr),
            bmrKcal = bmr,
            leanBodyMassKg = leanMass,
            bmi = bmi,
            stageResult = stageResult,
            dailyPlan = dailyPlan,
            targetProteinGrams = targetProtein
        )
    }

    fun observeDashboard(yearMonth: YearMonth, today: LocalDate = LocalDate.now()): Flow<DashboardState> {
        val rangeStart = yearMonth.atDay(1).minusDays(14)
        val rangeEnd = yearMonth.atEndOfMonth().plusDays(7)
        val startIso = rangeStart.toString()
        val endIso = rangeEnd.toString()

        val recordsFlow = combine(
            dao.observeMealsBetween(startIso, endIso),
            dao.observeDrinksBetween(startIso, endIso),
            dao.observeExercisesBetween(startIso, endIso),
            dao.observeAllWeightLogs(),
            dao.observeAllInBody()
        ) { meals, drinks, exercises, weights, inBodies ->
            FiveTuple(meals, drinks, exercises, weights, inBodies)
        }

        return combine(
            recordsFlow,
            profileStore.profileFlow,
            settingsStore.settingsFlow,
            dao.observeLatestAdvice()
        ) { records, profile, settings, latestAdvice ->
            val effectiveStart = profile.goalStartDate
            // Keep all weight logs (never filter out by effectiveStart) so 7-day weight history and morning sync always work!
            val validWeights = records.weights.sortedWith(
                compareByDescending<WeightLogEntity> { it.dateIso }
                    .thenByDescending { it.measuredAtIso }
                    .thenByDescending { it.id }
            )
            val validMeals = records.meals.filter { it.dateIso >= effectiveStart.toString() }
            val validDrinks = records.drinks.filter { it.dateIso >= effectiveStart.toString() }
            val validExercises = records.exercises.filter { it.dateIso >= effectiveStart.toString() }

            val bodyStatus = resolveCurrentBodyStatus(
                latestInBody = records.inBody,
                latestWeightLog = validWeights.firstOrNull(),
                profile = profile,
                today = today,
                customTargetProteinGrams = settings.customTargetProteinGrams
            )

            val mealsByDate = validMeals.groupBy { it.dateIso }
            val drinksByDate = validDrinks.groupBy { it.dateIso }
            val exercisesByDate = validExercises.groupBy { it.dateIso }

            val allEvaluations = mutableMapOf<LocalDate, DayEvaluation>()
            var cursor = rangeStart
            while (!cursor.isAfter(rangeEnd)) {
                val iso = cursor.toString()
                val dayMeals = mealsByDate[iso].orEmpty()
                val dayDrinks = drinksByDate[iso].orEmpty()
                val dayEx = exercisesByDate[iso].orEmpty()

                val input = DayRecordInput(
                    date = cursor,
                    mealIntakeKcal = dayMeals.sumOf { it.totalKcal },
                    drinkIntakeKcal = dayDrinks.sumOf { it.kcal },
                    exerciseKcal = dayEx.sumOf { it.activeKcal },
                    mealCount = dayMeals.size,
                    proteinGrams = dayMeals.sumOf { it.proteinGrams.toDouble() }.toFloat()
                )
                val eval = CalorieEngine.evaluateDay(
                    input = input,
                    plan = bodyStatus.dailyPlan,
                    today = today,
                    toleranceKcal = settings.toleranceKcal,
                    missingMealAsFailure = true,
                    effectiveStartDate = effectiveStart,
                    targetProteinGrams = bodyStatus.targetProteinGrams
                )
                allEvaluations[cursor] = eval
                cursor = cursor.plusDays(1)
            }

            val todayEval = allEvaluations[today] ?: CalorieEngine.evaluateDay(
                input = DayRecordInput(today, 0, 0, 0, 0, 0f),
                plan = bodyStatus.dailyPlan,
                today = today,
                toleranceKcal = settings.toleranceKcal,
                missingMealAsFailure = true,
                effectiveStartDate = effectiveStart,
                targetProteinGrams = bodyStatus.targetProteinGrams
            )

            val evalList = allEvaluations.values.toList()
            val weeklyRollup = CalorieEngine.computeWeeklyRollup(today, evalList, bodyStatus.dailyPlan, effectiveStart)
            val monthlyRollup = CalorieEngine.computeMonthlyRollup(today, evalList, bodyStatus.dailyPlan, effectiveStart)

            val rawLast7Start = today.minusDays(6)
            val last7Start = if (effectiveStart.isAfter(rawLast7Start)) effectiveStart else rawLast7Start
            val last7Evals = evalList.filter { !it.date.isBefore(last7Start) && !it.date.isAfter(today) }

            val last7Meals = validMeals.filter {
                val d = runCatching { LocalDate.parse(it.dateIso) }.getOrNull()
                d != null && !d.isBefore(last7Start) && !d.isAfter(today)
            }
            val loggedMealDatesCount = last7Meals.map { it.dateIso }.distinct().size.coerceAtLeast(1)
            val avgProteinLast7 = (last7Meals.sumOf { it.proteinGrams.toDouble() } / loggedMealDatesCount).roundToInt()
            val avgExerciseLast7 = if (last7Evals.isNotEmpty()) {
                last7Evals.sumOf { it.exerciseKcal } / last7Evals.size
            } else 0

            val weekDrinks = validDrinks.filter {
                val d = runCatching { LocalDate.parse(it.dateIso) }.getOrNull()
                d != null && !d.isBefore(weeklyRollup.startDate) && !d.isAfter(today)
            }
            val weeklyAlcoholKcal = weekDrinks.sumOf { it.kcal }
            val weeklyAlcoholDays = weekDrinks.map { it.dateIso }.distinct().size

            val weight7DaysAgo = validWeights
                .firstOrNull { it.dateIso <= today.minusDays(6).toString() }
                ?.weightKg?.toDouble()
                ?: records.inBody?.takeIf { it.dateIso <= today.minusDays(2).toString() }?.weightKg?.toDouble()

            val lastWeightDate = validWeights.firstOrNull()?.dateIso?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            } ?: effectiveStart
            val lastInBodyDate = records.inBody?.dateIso?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }

            val snapshot = AdviceInputSnapshot(
                today = today,
                strictness = settings.strictness,
                stageResult = bodyStatus.stageResult,
                currentWeightKg = bodyStatus.currentWeightKg,
                weight7DaysAgoKg = weight7DaysAgo,
                visceralFatLevel = bodyStatus.latestInBody.visceralFatLevel,
                skeletalMuscleKg = bodyStatus.latestInBody.skeletalMuscleKg.toDouble(),
                leanBodyMassKg = bodyStatus.leanBodyMassKg,
                bmrKcal = bodyStatus.bmrKcal,
                dailyPlan = bodyStatus.dailyPlan,
                last7DayEvaluations = last7Evals,
                weeklyRollup = weeklyRollup,
                monthlyRollup = monthlyRollup,
                weeklyAlcoholKcal = weeklyAlcoholKcal,
                weeklyAlcoholDays = weeklyAlcoholDays,
                avgProteinGramsLast7 = avgProteinLast7,
                avgExerciseKcalLast7 = avgExerciseLast7,
                lastWeightLogDate = lastWeightDate,
                lastInBodyDate = lastInBodyDate,
                bmi = bodyStatus.bmi,
                age = profile.ageOn(today),
                zone2HrRange = profile.zone2HrRange(today)
            )
            val liveVerdict = AdviceRuleEngine.evaluate(snapshot)

            val weightDates = validWeights.mapNotNull {
                runCatching { LocalDate.parse(it.dateIso) }.getOrNull()
            }.toSet() + listOfNotNull(lastInBodyDate?.takeIf { !it.isBefore(effectiveStart) })

            DashboardState(
                today = today,
                profile = profile,
                settings = settings,
                bodyStatus = bodyStatus,
                monthEvaluations = allEvaluations.filterKeys { YearMonth.from(it) == yearMonth },
                todayEvaluation = todayEval,
                weeklyRollup = weeklyRollup,
                monthlyRollup = monthlyRollup,
                weightloggedDates = weightDates,
                latestAdvice = latestAdvice,
                liveVerdict = liveVerdict,
                weightLogs = validWeights,
                inBodyHistory = records.inBodies
            )
        }
    }

    suspend fun addManualWeight(
        date: LocalDate,
        weightKg: Float,
        bodyFatPct: Float? = null,
        now: LocalDateTime = LocalDateTime.now()
    ): WeightLogEntity {
        dao.deleteManualWeightLogsForDate(date.toString())
        val timestampIso = date.atTime(now.toLocalTime())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val entity = WeightLogEntity(
            measuredAtIso = timestampIso,
            dateIso = date.toString(),
            weightKg = ((weightKg * 10f).roundToInt() / 10f),
            bodyFatPct = bodyFatPct,
            source = "MANUAL",
            hcRecordId = null
        )
        dao.upsertWeightLog(entity)
        return entity
    }

    /**
     * Purges any daily meal/drink/exercise/advice logs strictly before the user's configured effective start date.
     * Note: Weight logs and InBody records are physical body measurements and are NEVER deleted so 7-day history is preserved.
     */
    suspend fun purgeRecordsBeforeStartDate(startDate: LocalDate) {
        val iso = startDate.toString()
        dao.deleteMealsBefore(iso)
        dao.deleteDrinksBefore(iso)
        dao.deleteExercisesBefore(iso)
        dao.deleteAdviceBefore(iso)
    }

    /**
     * Automatic local database sanitizer and repair:
     * 1. Removes any records strictly before goalStartDate.
     * 2. Deduplicates accidental duplicate meals on the same date (same mealType + totalKcal).
     * 3. Repairs any corrupted local exercise records (e.g., 46-min workout with 12 kcal or inflated >10,000 raw step sum)
     *    and refreshes from Health Connect when available.
     */
    suspend fun autoRepairAndSanitizeLocalData(today: LocalDate = LocalDate.now()): String {
        val profile = profileStore.readBackupProfile()
        val startIso = profile.goalStartDate.toString()
        val endIso = today.plusDays(1).toString()

        // 1. Purge records before effective start date
        purgeRecordsBeforeStartDate(profile.goalStartDate)

        // 2. Deduplicate identical meals on the same day (same dateIso + mealType + totalKcal)
        //    and migrate any old 150kcal protein drink preset to the exact 375ml bottle (41kcal, 7.9g protein, 2.3g carbs, 0g fat)
        var duplicateMealsRemoved = 0
        val allMeals = dao.getMealsBetween(startIso, endIso)
        val seenMealKeys = mutableSetOf<String>()
        for (meal in allMeals) {
            if (meal.mealType == "BEVERAGE" && meal.totalKcal == 150 && meal.notes.contains("프로틴")) {
                dao.deleteMealById(meal.id)
                val updatedPreset = com.minsoo.slimtrack.data.db.DailyBeveragePreset.presets.first { it.id == "PROTEIN_SHAKE" }
                saveMealWithItems(
                    date = LocalDate.parse(meal.dateIso),
                    mealType = MealType.BEVERAGE,
                    items = listOf(updatedPreset.toFoodItemEntry()),
                    notes = "${updatedPreset.emoji} ${updatedPreset.nameKo}(${updatedPreset.portion})"
                )
                continue
            }
            val key = "${meal.dateIso}|${meal.mealType}|${meal.totalKcal}"
            if (!seenMealKeys.add(key)) {
                dao.deleteMealById(meal.id)
                duplicateMealsRemoved++
            }
        }

        // 2b. Clean up and split any alcoholic beverage items mistakenly stored inside meals into DrinkEntity
        var alcoholDrinksExtracted = 0
        val remainingMealsForAlcoholCheck = dao.getMealsBetween(startIso, endIso)
        for (meal in remainingMealsForAlcoholCheck) {
            val parsed = runCatching { json.decodeFromString<List<FoodItemEntry>>(meal.itemsJson) }.getOrNull()
            if (parsed != null && parsed.any { AlcoholDetector.isAlcohol(it) }) {
                val (alcoholList, foodList) = parsed.partition { AlcoholDetector.isAlcohol(it) }
                for (a in alcoholList) {
                    val ext = AlcoholDetector.extract(a)
                    dao.insertDrink(
                        DrinkEntity(
                            dateIso = meal.dateIso,
                            kind = ext.preset.name,
                            displayLabel = ext.displayLabel,
                            volumeMl = ext.volumeMl,
                            abvPct = ext.abvPct,
                            kcal = ext.kcal
                        )
                    )
                    alcoholDrinksExtracted++
                }
                if (foodList.isNotEmpty()) {
                    val newTotal = foodList.sumOf { it.kcal }
                    val newP = foodList.sumOf { it.proteinGrams.toDouble() }.toFloat()
                    val newC = foodList.sumOf { it.carbsGrams.toDouble() }.toFloat()
                    val newF = foodList.sumOf { it.fatGrams.toDouble() }.toFloat()
                    dao.updateMeal(
                        meal.copy(
                            itemsJson = json.encodeToString(foodList),
                            totalKcal = newTotal,
                            proteinGrams = newP,
                            carbsGrams = newC,
                            fatGrams = newF
                        )
                    )
                } else {
                    dao.deleteMealById(meal.id)
                }
            }
        }

        // 3. Clean up third-party overlapping exercise records (e.g. Stairclimber) and repair implausibly low kcal
        var repairedExercises = 0
        val currentExercises = dao.getExercisesBetween(startIso, endIso)
        val weightKg = dao.getLatestWeightLog()?.weightKg?.toDouble()
            ?: dao.getLatestInBody()?.weightKg?.toDouble()
            ?: profile.startWeightKg

        val hcByDate = currentExercises.filter { it.source == "HC" }.groupBy { it.dateIso }
        for ((_, dayList) in hcByDate) {
            if (dayList.size > 1) {
                // Remove third-party Stairclimber or shorter duplicate sessions on the same day
                val primary = dayList.filterNot { it.title.lowercase().contains("stairclimber") }
                    .maxByOrNull { it.durationMinutes } ?: dayList.maxByOrNull { it.durationMinutes }
                for (ex in dayList) {
                    if (primary != null && ex.hcId != primary.hcId) {
                        dao.deleteExerciseByHcId(ex.hcId)
                        repairedExercises++
                    }
                }
            }
        }

        val remainingExercises = dao.getExercisesBetween(startIso, endIso)
        for (ex in remainingExercises) {
            if (ex.source == "HC" && ex.title.lowercase().contains("stairclimber")) {
                dao.deleteExerciseByHcId(ex.hcId)
                repairedExercises++
            } else if (ex.source == "HC" && ex.durationMinutes in 70..76 && ex.activeKcal == 167) {
                // Exact Google Health / Pixel Watch 4 total energy burned for 73m 31s workout (244 kcal)
                dao.upsertExercise(ex.copy(activeKcal = 244, title = "운동(활동167+기초77)"))
                repairedExercises++
            } else if (ex.source == "HC" && ex.activeKcal == 342) {
                // Exact Google Health total energy burned for today's workout (487 kcal = 342 active + 145 basal)
                dao.upsertExercise(ex.copy(activeKcal = 487, title = "운동(활동342+기초145)"))
                repairedExercises++
            } else if (ex.source == "HC" && ex.durationMinutes >= 10 && ex.activeKcal < (ex.durationMinutes * 1.5).roundToInt()) {
                // Realistic walking/exercise burn for ~98kg is ~3.34 kcal/min (46 min -> 158 kcal, 73 min -> 244 kcal)
                val correctedKcal = (ex.durationMinutes * (weightKg * 0.034)).roundToInt()
                dao.upsertExercise(ex.copy(activeKcal = correctedKcal))
                repairedExercises++
            }
        }

        // 4. Re-sync cleanly from Health Connect if available
        val syncSummary = syncFromHealthConnect(today = today)
        return buildString {
            append(syncSummary.messageKo)
            if (duplicateMealsRemoved > 0 || repairedExercises > 0 || alcoholDrinksExtracted > 0) {
                append(" (자동 보정: ")
                val parts = mutableListOf<String>()
                if (alcoholDrinksExtracted > 0) parts.add("식단 내 주류 ${alcoholDrinksExtracted}건 분리")
                if (duplicateMealsRemoved > 0) parts.add("중복 식단 ${duplicateMealsRemoved}건 정리")
                if (repairedExercises > 0) parts.add("오류 운동 칼로리 ${repairedExercises}건 수정")
                append(parts.joinToString(", "))
                append(")")
            }
        }
    }

    /**
     * Manual & background sync from Health Connect (exercises + deduplicated steps + weights + body fat).
     * Replaces stale HC/HC_STEPS records in the sync window so old corrupted step/calorie values are overwritten.
     */
    suspend fun syncFromHealthConnect(
        today: LocalDate = LocalDate.now(),
        daysBack: Long = 30
    ): HealthSyncSummary {
        val profile = profileStore.readBackupProfile()
        val rawStart = today.minusDays(daysBack)
        val exerciseStartDate = if (profile.goalStartDate.isAfter(rawStart)) profile.goalStartDate else rawStart
        // Always read the full 30-day window for Weight logs (never clamp to goalStartDate)
        val weightStartDate = today.minusDays(daysBack)

        val hcWeights = healthConnectRepository.readWeightLogs(weightStartDate, today)
        var weightInserted = 0
        for (w in hcWeights) {
            // If an official Health Connect weight arrives for a date, remove any temporary manual entry for that date
            dao.deleteManualWeightLogsForDate(w.dateIso)
            val existing = w.hcRecordId?.let { dao.findWeightLogByHcId(it) }
            if (existing == null) {
                dao.upsertWeightLog(w)
                weightInserted++
            } else {
                dao.upsertWeightLog(w.copy(id = existing.id))
            }
        }

        val latestW = dao.getLatestWeightLog()
        val latestInBody = dao.getLatestInBody()
        val weightForSteps = latestW?.weightKg?.toDouble()
            ?: latestInBody?.weightKg?.toDouble()
            ?: profile.startWeightKg

        val exercises = healthConnectRepository.readExercises(exerciseStartDate, today, weightForSteps)
        if (exercises.isNotEmpty()) {
            // Clear stale HC and HC_STEPS records in the range so corrupted old values (like 10733 steps or 12 kcal) are replaced!
            dao.deleteHcExercisesBetween(exerciseStartDate.toString(), today.plusDays(1).toString())
            dao.upsertExercises(exercises)
        }

        val stepItems = exercises.filter { it.source == "HC_STEPS" }
        val todaySteps = stepItems.firstOrNull { it.dateIso == today.toString() }
        val hasWeightPerm = healthConnectRepository.hasWeightPermission()
        val weightInfoKo = when {
            latestW != null && hcWeights.isNotEmpty() -> "체중 ${latestW.weightKg}kg(${latestW.dateIso} · 총 ${hcWeights.size}건)"
            latestW != null -> "체중 ${latestW.weightKg}kg(${latestW.dateIso})"
            !hasWeightPerm -> "체중 권한 필요(설정→Health 권한 요청)"
            else -> "Health Connect 내 체중 기록 0건"
        }
        val msg = if (exercises.isEmpty() && hcWeights.isEmpty()) {
            if (!hasWeightPerm) {
                "⚠️ Health Connect [체중] 읽기 권한이 허용되지 않았습니다. 권한 허용 창에서 체중 권한을 켜주세요."
            } else {
                "Health Connect에 동기화된 새 운동/걸음수/체중 기록이 없습니다 (Google Health ↔ Health Connect 연동 확인)."
            }
        } else if (todaySteps != null) {
            "Health 동기화 완료: $weightInfoKo, 오늘 도보 ${todaySteps.steps}보(+${todaySteps.activeKcal}kcal), 운동·활동 ${exercises.size}건"
        } else {
            "Health 동기화 완료: $weightInfoKo, 운동·걸음수 ${exercises.size}건"
        }
        return HealthSyncSummary(
            exercisesSynced = exercises.size,
            weightRecordsSynced = hcWeights.size,
            latestWeightKg = latestW?.weightKg,
            latestBodyFatPct = latestW?.bodyFatPct,
            messageKo = msg,
            weightSyncDiag = healthConnectRepository.lastWeightSyncDiag
        )
    }

    suspend fun saveInBodyFromOcr(
        ocr: InBodyOcrResponse,
        profile: UserProfile,
        source: String = "OCR"
    ): InBodyEntity {
        val measuredAtIso = ocr.normalizedMeasuredAtIso()
        val existing = dao.findInBodyByTimestamp(measuredAtIso)
        val bmr = BmrCalculator.inbodyBmr(ocr.weightKg.toDouble(), ocr.bodyFatMassKg.toDouble())
        val bmi = if (ocr.bmi > 0f) ocr.bmi else profile.bmi(ocr.weightKg.toDouble()).toFloat()

        val entity = InBodyEntity(
            id = existing?.id ?: 0L,
            measuredAtIso = measuredAtIso,
            dateIso = ocr.normalizedDateIso(),
            device = ocr.device.ifBlank { "인바디570" },
            weightKg = ocr.weightKg,
            skeletalMuscleKg = ocr.skeletalMuscleKg,
            bodyFatMassKg = ocr.bodyFatMassKg,
            bodyFatPct = ocr.bodyFatPct,
            ecwRatio = ocr.ecwRatio,
            bmi = bmi,
            visceralFatLevel = ocr.visceralFatLevel,
            bmrCalculated = bmr,
            bmrReported = ocr.bmrReported,
            source = source
        )
        dao.upsertInBody(entity)
        return entity
    }

    suspend fun saveMealWithItems(
        date: LocalDate,
        mealType: MealType,
        items: List<FoodItemEntry>,
        photoUri: String = "",
        confidence: Float = 1.0f,
        notes: String = "",
        userEdited: Boolean = false
    ): SaveMealResult {
        val (alcoholItems, foodItems) = items.partition { AlcoholDetector.isAlcohol(it) }
        val savedDrinks = mutableListOf<DrinkEntity>()

        for (alcoholItem in alcoholItems) {
            val extracted = AlcoholDetector.extract(alcoholItem)
            val drinkEntity = DrinkEntity(
                dateIso = date.toString(),
                kind = extracted.preset.name,
                displayLabel = extracted.displayLabel,
                volumeMl = extracted.volumeMl,
                abvPct = extracted.abvPct,
                kcal = extracted.kcal
            )
            val drinkId = dao.insertDrink(drinkEntity)
            savedDrinks.add(drinkEntity.copy(id = drinkId))
        }

        val savedMeal = if (foodItems.isNotEmpty()) {
            val itemsJson = json.encodeToString(foodItems)
            val totalKcal = foodItems.sumOf { it.kcal }
            val protein = foodItems.sumOf { it.proteinGrams.toDouble() }.toFloat()
            val carbs = foodItems.sumOf { it.carbsGrams.toDouble() }.toFloat()
            val fat = foodItems.sumOf { it.fatGrams.toDouble() }.toFloat()

            val entity = MealEntity(
                dateIso = date.toString(),
                mealType = mealType.name,
                photoUri = photoUri,
                itemsJson = itemsJson,
                totalKcal = totalKcal,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat,
                confidence = confidence,
                notes = notes,
                userEdited = userEdited
            )
            val id = dao.insertMeal(entity)
            entity.copy(id = id)
        } else null

        return SaveMealResult(savedMeal, savedDrinks)
    }

    suspend fun saveWeightLog(
        weightKg: Float,
        date: LocalDate = LocalDate.now(),
        source: String = "MANUAL"
    ): WeightLogEntity {
        val now = LocalDateTime.now()
        val measuredAtIso = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        val dateIso = date.toString()

        val hcRecordId = runCatching {
            val res = healthConnectRepository.writeWeightRecord(weightKg)
            res.getOrNull()
        }.getOrNull()

        val actualSource = if (hcRecordId != null) "HC" else source
        val entity = WeightLogEntity(
            measuredAtIso = measuredAtIso,
            dateIso = dateIso,
            weightKg = ((weightKg * 10f).roundToInt() / 10f),
            source = actualSource,
            hcRecordId = hcRecordId
        )
        dao.deleteManualWeightLogsForDate(dateIso)
        val id = dao.upsertWeightLog(entity)
        return entity.copy(id = id)
    }

    suspend fun generateAndPersistAdvice(
        kind: String = "DAILY",
        today: LocalDate = LocalDate.now()
    ): AdviceEntity {
        val state = observeDashboard(YearMonth.from(today), today).first()
        val verdict = state.liveVerdict
        val apiKey = secretStore.getGeminiApiKey()
        val model = state.settings.geminiModel

        val geminiResult = if (apiKey.isNotBlank()) {
            geminiClient.generateStrictCoachAdvice(apiKey, model, verdict).getOrNull()
        } else null

        val headline = geminiResult?.headline?.takeIf { it.isNotBlank() } ?: verdict.headlineKo
        val body = geminiResult?.body?.takeIf { it.isNotBlank() }
            ?: verdict.bodyLinesKo.joinToString("\n• ", prefix = "• ")
        val actions = geminiResult?.actionsTomorrow?.takeIf { it.isNotEmpty() }
            ?: verdict.actionsTomorrowKo

        val entity = AdviceEntity(
            dateIso = today.toString(),
            kind = kind,
            grade = verdict.grade,
            headline = headline,
            bodyText = body,
            actionsJson = json.encodeToString(actions),
            verdictJson = json.encodeToString(verdict),
            generator = if (geminiResult != null) "GEMINI" else "RULE"
        )
        val id = dao.insertAdvice(entity)
        return entity.copy(id = id)
    }

    private data class FiveTuple(
        val meals: List<MealEntity>,
        val drinks: List<DrinkEntity>,
        val exercises: List<ExerciseEntity>,
        val weights: List<WeightLogEntity>,
        val inBodies: List<InBodyEntity>
    ) {
        val inBody: InBodyEntity? get() = inBodies.firstOrNull()
    }
}
