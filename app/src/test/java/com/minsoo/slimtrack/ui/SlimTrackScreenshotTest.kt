package com.minsoo.slimtrack.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.minsoo.slimtrack.MainTab
import com.minsoo.slimtrack.data.db.AppDatabase
import com.minsoo.slimtrack.data.db.DrinkEntity
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.FoodItemEntry
import com.minsoo.slimtrack.data.db.MealEntity
import com.minsoo.slimtrack.data.db.MealType
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.data.health.HealthConnectStatus
import com.minsoo.slimtrack.data.prefs.AppSettings
import com.minsoo.slimtrack.data.prefs.UserProfile
import com.minsoo.slimtrack.data.repo.CurrentBodyStatus
import com.minsoo.slimtrack.data.repo.DashboardState
import com.minsoo.slimtrack.domain.AdviceInputSnapshot
import com.minsoo.slimtrack.domain.AdviceRuleEngine
import com.minsoo.slimtrack.domain.AdviceStrictness
import com.minsoo.slimtrack.domain.BmrCalculator
import com.minsoo.slimtrack.domain.CalorieEngine
import com.minsoo.slimtrack.domain.DayRecordInput
import com.minsoo.slimtrack.domain.GoalStageEvaluator
import com.minsoo.slimtrack.ui.advice.CoachAdviceScreen
import com.minsoo.slimtrack.ui.body.BodyAndStageScreen
import com.minsoo.slimtrack.ui.home.HomeScreen
import com.minsoo.slimtrack.ui.meal.MealAndDrinkScreen
import com.minsoo.slimtrack.ui.settings.SettingsAndProfileScreen
import com.minsoo.slimtrack.ui.theme.SlimTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h914dp-xxhdpi")
class SlimTrackScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val outDir = File("build/outputs/roborazzi").apply { mkdirs() }
    private val roborazziOptions = RoborazziOptions(
        recordOptions = RoborazziOptions.RecordOptions(resizeScale = 1.0)
    )

    private val today = LocalDate.of(2026, 11, 9)
    private val ym = YearMonth.of(2026, 11)
    private val profile = UserProfile()
    private val settings = AppSettings()
    private val seedInBody = AppDatabase.defaultSeedInBody()
    private val bmr = BmrCalculator.inbodyBmr(98.9, 31.1)
    private val stageResult = GoalStageEvaluator.evaluate(98.4, 12, 38.5)
    private val dailyPlan = CalorieEngine.dailyPlan(bmr, 1.2, 682)

    private val bodyStatus = CurrentBodyStatus(
        currentWeightKg = 98.4,
        currentWeightSource = "일일 체중(MANUAL)",
        currentWeightDateIso = "2026-11-09",
        latestInBody = seedInBody,
        bmrKcal = bmr,
        leanBodyMassKg = 67.8,
        bmi = profile.bmi(98.4),
        stageResult = stageResult,
        dailyPlan = dailyPlan
    )

    private fun buildSampleDashboard(): DashboardState {
        val evals = mutableMapOf<LocalDate, com.minsoo.slimtrack.domain.DayEvaluation>()
        for (d in 1..ym.lengthOfMonth()) {
            val date = ym.atDay(d)
            val input = when (d) {
                7 -> DayRecordInput(date, 1750, 0, 350, 3)   // Green (+99 kcal)
                8 -> DayRecordInput(date, 1950, 328, 250, 3) // Red (-509 kcal, with Soju)
                9 -> DayRecordInput(date, 1800, 0, 320, 3)   // Today Green (+19 kcal)
                else -> DayRecordInput(date, 0, 0, 0, if (d < 7) 3 else 0)
            }
            val fixedInput = if (d in 1..6) DayRecordInput(date, 1820, 0, 310, 3) else input
            evals[date] = CalorieEngine.evaluateDay(fixedInput, dailyPlan, today, 150, true)
        }
        val list = evals.values.toList()
        val weekly = CalorieEngine.computeWeeklyRollup(today, list, dailyPlan)
        val monthly = CalorieEngine.computeMonthlyRollup(today, list, dailyPlan)
        val snapshot = AdviceInputSnapshot(
            today = today,
            strictness = AdviceStrictness.STRICT,
            stageResult = stageResult,
            currentWeightKg = 98.4,
            weight7DaysAgoKg = 98.9,
            visceralFatLevel = 12,
            skeletalMuscleKg = 38.5,
            leanBodyMassKg = 67.8,
            bmrKcal = bmr,
            dailyPlan = dailyPlan,
            last7DayEvaluations = list.filter { !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today) },
            weeklyRollup = weekly,
            monthlyRollup = monthly,
            weeklyAlcoholKcal = 328,
            weeklyAlcoholDays = 1,
            avgProteinGramsLast7 = 102,
            avgExerciseKcalLast7 = 310,
            lastWeightLogDate = today,
            lastInBodyDate = LocalDate.of(2026, 11, 7),
            bmi = profile.bmi(98.4),
            age = profile.ageOn(today),
            zone2HrRange = profile.zone2HrRange(today)
        )
        val verdict = AdviceRuleEngine.evaluate(snapshot)
        return DashboardState(
            today = today,
            profile = profile,
            settings = settings,
            bodyStatus = bodyStatus,
            monthEvaluations = evals,
            todayEvaluation = evals[today]!!,
            weeklyRollup = weekly,
            monthlyRollup = monthly,
            weightloggedDates = setOf(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8), today),
            latestAdvice = null,
            liveVerdict = verdict
        )
    }

    @Test
    fun capture01HomeCalendar() {
        val dash = buildSampleDashboard()
        composeRule.setContent {
            SlimTrackTheme {
                HomeScreen(
                    dashboard = dash,
                    currentYearMonth = ym,
                    selectedDate = today,
                    selectedDateMeals = listOf(
                        MealEntity(1, "2026-11-09", "BREAKFAST", "", "[]", 520, 32f, 45f, 18f, 0.9f, "닭가슴살 샐러드"),
                        MealEntity(2, "2026-11-09", "LUNCH", "", "[]", 680, 38f, 65f, 22f, 0.9f, "김치찌개 정식"),
                        MealEntity(3, "2026-11-09", "DINNER", "", "[]", 600, 40f, 50f, 16f, 0.9f, "연어 포케")
                    ),
                    selectedDateDrinks = listOf(
                        DrinkEntity(1, "2026-11-09", "SOJU", "소주", 360, 16.5f, 328)
                    ),
                    selectedDateExercises = listOf(
                        ExerciseEntity("hc-1", "2026-11-09", "러닝", "Zone 2 러닝", 45, 320, 5200, "HC")
                    ),
                    syncMessage = "Health Connect 운동 1건, 체중 3건 동기화 완료",
                    onPrevMonth = {},
                    onNextMonth = {},
                    onSelectDate = {},
                    onQuickAddWeight = { _, _ -> },
                    onSyncHealthConnect = {},
                    onNavigateToMealTab = {},
                    onNavigateToCoachTab = {},
                    onDeleteMeal = {},
                    onDeleteDrink = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage(File(outDir, "01_home_calendar.png"), roborazziOptions)
    }

    @Test
    fun capture02MealDrinkExercise() {
        composeRule.setContent {
            SlimTrackTheme {
                MealAndDrinkScreen(
                    selectedDate = today,
                    selectedMealType = MealType.ANJU,
                    stagedItems = listOf(
                        FoodItemEntry("삼겹살 구이", "1인분(200g)", 660, 34f, 2f, 56f),
                        FoodItemEntry("된장찌개", "1뚝배기", 180, 12f, 14f, 8f)
                    ),
                    stagedNotes = "회식 안주 사진 분석 결과",
                    stagedConfidence = 0.88f,
                    isAnalyzingPhoto = false,
                    analysisStatusMessage = "🤖 Gemini 분석 완료: 2개 품목, 총 840kcal",
                    todayDrinks = listOf(DrinkEntity(1, "2026-11-09", "SOJU", "소주", 360, 16.5f, 328)),
                    todayExercises = listOf(ExerciseEntity("hc-1", "2026-11-09", "러닝", "Zone 2 러닝", 45, 320, 5200, "HC")),
                    onSelectMealType = {},
                    onTakePhotoClick = {},
                    onPickPhotoClick = {},
                    onUpdateStagedItem = { _, _ -> },
                    onRemoveStagedItem = {},
                    onAddManualFoodItem = {},
                    onSaveMeal = {},
                    onAddDrink = { _, _, _ -> },
                    onDeleteDrink = {},
                    onSyncHealthConnectExercises = {},
                    onAddManualExercise = { _, _, _ -> }
                )
            }
        }
        composeRule.onRoot().captureRoboImage(File(outDir, "02_meal_drink_exercise.png"), roborazziOptions)
    }

    @Test
    fun capture03BodyInBodyStages() {
        composeRule.setContent {
            SlimTrackTheme {
                BodyAndStageScreen(
                    today = today,
                    bodyStatus = bodyStatus,
                    weightLogs = listOf(
                        WeightLogEntity(1, "2026-11-07T08:00:00", "2026-11-07", 98.9f, 31.4f, "HC", "hc1"),
                        WeightLogEntity(2, "2026-11-08T08:00:00", "2026-11-08", 98.6f, 31.3f, "MANUAL", null),
                        WeightLogEntity(3, "2026-11-09T08:00:00", "2026-11-09", 98.4f, 31.2f, "MANUAL", null)
                    ),
                    inBodyHistory = listOf(seedInBody),
                    isAnalyzingInBody = false,
                    syncStatusMessage = "✅ InBody 570 최신 기록 반영됨 (공식 BMR 1,834kcal)",
                    onAddManualWeight = { _, _, _ -> },
                    onManualSyncHealthConnectWeight = {},
                    onPickInBodyScreenshot = {},
                    onSaveInBodyManualOrOcr = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage(File(outDir, "03_body_inbody_stages.png"), roborazziOptions)
    }

    @Test
    fun capture04StrictCoachAdvice() {
        val dash = buildSampleDashboard()
        composeRule.setContent {
            SlimTrackTheme {
                CoachAdviceScreen(
                    liveVerdict = dash.liveVerdict,
                    adviceHistory = emptyList(),
                    isGeneratingAiAdvice = false,
                    onGenerateAiAdviceNow = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage(File(outDir, "04_strict_coach_advice.png"), roborazziOptions)
    }

    @Test
    fun capture05ProfileSettings() {
        composeRule.setContent {
            SlimTrackTheme {
                SettingsAndProfileScreen(
                    today = today,
                    profile = profile,
                    settings = settings,
                    currentApiKey = "AIzaSy***************",
                    availableGemini3Models = com.minsoo.slimtrack.data.prefs.Gemini3Catalog.presetModels,
                    isFetchingModels = false,
                    modelFetchMessage = "✅ 사용 가능한 Gemini 3.x 모델 5개 확인됨",
                    healthConnectStatus = HealthConnectStatus.AVAILABLE,
                    onSaveProfile = {},
                    onSaveSettings = {},
                    onSaveApiKey = {},
                    onFetchAvailableGemini3Models = {},
                    onRequestHealthPermissions = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage(File(outDir, "05_profile_settings.png"), roborazziOptions)
    }

    @Test
    fun capture06HomeDetailAndBottomNav() {
        val dash = buildSampleDashboard()
        // Set day 2..7 as missing meals (0 meals) to verify Canvas graphic X icon in calendar cells
        val modifiedEvals = dash.monthEvaluations.toMutableMap()
        for (d in 2..6) {
            val date = ym.atDay(d)
            modifiedEvals[date] = CalorieEngine.evaluateDay(DayRecordInput(date, 0, 0, 0, 0), dailyPlan, today, 150, true)
        }
        val dashWithMissing = dash.copy(monthEvaluations = modifiedEvals)
        composeRule.setContent {
            SlimTrackTheme {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            MainTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = tab == MainTab.HOME,
                                    onClick = {},
                                    icon = { Icon(tab.icon, contentDescription = tab.labelKo) },
                                    label = {
                                        Text(
                                            text = tab.labelKo,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { pad ->
                    HomeScreen(
                        dashboard = dashWithMissing,
                        currentYearMonth = ym,
                        selectedDate = today,
                        selectedDateMeals = listOf(
                            MealEntity(1, "2026-11-09", "LUNCH", "", "[]", 1055, 55f, 65f, 22f, 0.9f, "다양한 단백질원 정식")
                        ),
                        selectedDateDrinks = emptyList(),
                        selectedDateExercises = emptyList(),
                        syncMessage = null,
                        onPrevMonth = {},
                        onNextMonth = {},
                        onSelectDate = {},
                        onQuickAddWeight = { _, _ -> },
                        onSyncHealthConnect = {},
                        onNavigateToMealTab = {},
                        onNavigateToCoachTab = {},
                        onDeleteMeal = {},
                        onDeleteDrink = {},
                        modifier = Modifier.padding(pad)
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage(File(outDir, "06_home_with_bottom_nav.png"), roborazziOptions)
    }
}
