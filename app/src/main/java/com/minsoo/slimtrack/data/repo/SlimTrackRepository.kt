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

data class CurrentBodyStatus(
    val currentWeightKg: Double,
    val currentWeightSource: String,
    val currentWeightDateIso: String,
    val latestInBody: InBodyEntity,
    val bmrKcal: Int,
    val leanBodyMassKg: Double,
    val bmi: Double,
    val stageResult: StageEvaluationResult,
    val dailyPlan: DailyPlan
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
    val liveVerdict: AdviceVerdict
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
        today: LocalDate
    ): CurrentBodyStatus {
        val inBody = latestInBody ?: AppDatabase.defaultSeedInBody()
        val useWeightLog = latestWeightLog != null &&
            latestWeightLog.measuredAtIso >= inBody.measuredAtIso

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
            startWeightKg = GoalStageEvaluator.DEFAULT_START_WEIGHT_KG,
            muscleBaselineKg = profile.muscleBaselineKg,
            startDate = profile.goalStartDate
        )

        val deficitTarget = CalorieEngine.dailyDeficitTarget(
            currentWeightKg = currentWeight,
            targetWeightKg = stageResult.activeStage.targetWeightMidKg,
            today = today,
            deadline = stageResult.deadlineForActiveStage
        )
        val dailyPlan = CalorieEngine.dailyPlan(
            bmr = bmr,
            activityFactor = profile.activityFactor,
            deficitTarget = deficitTarget
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
            dailyPlan = dailyPlan
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
            dao.observeLatestInBody()
        ) { meals, drinks, exercises, weights, inBody ->
            FiveTuple(meals, drinks, exercises, weights, inBody)
        }

        return combine(
            recordsFlow,
            profileStore.profileFlow,
            settingsStore.settingsFlow,
            dao.observeLatestAdvice()
        ) { records, profile, settings, latestAdvice ->
            val effectiveStart = profile.goalStartDate
            val validWeights = records.weights.filter { it.dateIso >= effectiveStart.toString() }
            val validMeals = records.meals.filter { it.dateIso >= effectiveStart.toString() }
            val validDrinks = records.drinks.filter { it.dateIso >= effectiveStart.toString() }
            val validExercises = records.exercises.filter { it.dateIso >= effectiveStart.toString() }

            val bodyStatus = resolveCurrentBodyStatus(
                latestInBody = records.inBody,
                latestWeightLog = validWeights.firstOrNull(),
                profile = profile,
                today = today
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
                    mealCount = dayMeals.size
                )
                val eval = CalorieEngine.evaluateDay(
                    input = input,
                    plan = bodyStatus.dailyPlan,
                    today = today,
                    toleranceKcal = settings.toleranceKcal,
                    missingMealAsFailure = true,
                    effectiveStartDate = effectiveStart
                )
                allEvaluations[cursor] = eval
                cursor = cursor.plusDays(1)
            }

            val todayEval = allEvaluations[today] ?: CalorieEngine.evaluateDay(
                input = DayRecordInput(today, 0, 0, 0, 0),
                plan = bodyStatus.dailyPlan,
                today = today,
                toleranceKcal = settings.toleranceKcal,
                missingMealAsFailure = true,
                effectiveStartDate = effectiveStart
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
                liveVerdict = liveVerdict
            )
        }
    }

    suspend fun addManualWeight(
        date: LocalDate,
        weightKg: Float,
        bodyFatPct: Float? = null,
        now: LocalDateTime = LocalDateTime.now()
    ): WeightLogEntity {
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
     * Purges any daily logs strictly before the user's configured effective start date (goalStartDate).
     */
    suspend fun purgeRecordsBeforeStartDate(startDate: LocalDate) {
        val iso = startDate.toString()
        dao.deleteMealsBefore(iso)
        dao.deleteDrinksBefore(iso)
        dao.deleteExercisesBefore(iso)
        dao.deleteWeightLogsBefore(iso)
        dao.deleteAdviceBefore(iso)
    }

    /**
     * Manual & background sync from Health Connect (exercises + weights + body fat).
     * Respects effective start date (goalStartDate) and deduplicates weight records by hcRecordId.
     */
    suspend fun syncFromHealthConnect(
        today: LocalDate = LocalDate.now(),
        daysBack: Long = 30
    ): HealthSyncSummary {
        val profile = profileStore.readBackupProfile()
        val rawStart = today.minusDays(daysBack)
        val startDate = if (profile.goalStartDate.isAfter(rawStart)) profile.goalStartDate else rawStart
        val exercises = healthConnectRepository.readExercises(startDate, today)
        if (exercises.isNotEmpty()) {
            dao.upsertExercises(exercises)
        }

        val hcWeights = healthConnectRepository.readWeightLogs(startDate, today)
        var weightInserted = 0
        for (w in hcWeights) {
            val existing = w.hcRecordId?.let { dao.findWeightLogByHcId(it) }
            if (existing == null) {
                dao.upsertWeightLog(w)
                weightInserted++
            } else {
                dao.upsertWeightLog(w.copy(id = existing.id))
            }
        }

        val latestW = dao.getLatestWeightLog()
        val msg = if (exercises.isEmpty() && hcWeights.isEmpty()) {
            "Health Connect에서 가져올 새 운동/체중/체지방 기록이 없거나 권한이 필요합니다."
        } else {
            "Health Connect 동기화 완료: 운동 ${exercises.size}건, 체중·체지방 ${hcWeights.size}건(신규 ${weightInserted}건)"
        }
        return HealthSyncSummary(
            exercisesSynced = exercises.size,
            weightRecordsSynced = hcWeights.size,
            latestWeightKg = latestW?.weightKg,
            latestBodyFatPct = latestW?.bodyFatPct,
            messageKo = msg
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
    ): MealEntity {
        val itemsJson = json.encodeToString(items)
        val totalKcal = items.sumOf { it.kcal }
        val protein = items.sumOf { it.proteinGrams.toDouble() }.toFloat()
        val carbs = items.sumOf { it.carbsGrams.toDouble() }.toFloat()
        val fat = items.sumOf { it.fatGrams.toDouble() }.toFloat()

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
        val inBody: InBodyEntity?
    )
}
