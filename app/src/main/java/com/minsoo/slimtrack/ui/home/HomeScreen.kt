package com.minsoo.slimtrack.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.R
import com.minsoo.slimtrack.data.db.DailyBeveragePreset
import com.minsoo.slimtrack.data.db.DrinkEntity
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.MealEntity
import com.minsoo.slimtrack.data.db.MealType
import com.minsoo.slimtrack.data.repo.DashboardState
import com.minsoo.slimtrack.domain.DayEvaluation
import com.minsoo.slimtrack.domain.DayStatusColor
import com.minsoo.slimtrack.domain.GoalMetricComparisonSeries
import com.minsoo.slimtrack.domain.GoalMetricType
import com.minsoo.slimtrack.domain.GoalTrajectoryCalculator
import com.minsoo.slimtrack.domain.PeriodRollup
import com.minsoo.slimtrack.ui.meal.ExpandableMealAccordionCard
import com.minsoo.slimtrack.ui.theme.StatusGrayBg
import com.minsoo.slimtrack.ui.theme.StatusGrayBorder
import com.minsoo.slimtrack.ui.theme.StatusGrayText
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenBorder
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import com.minsoo.slimtrack.ui.theme.StatusRedBg
import com.minsoo.slimtrack.ui.theme.StatusRedBorder
import com.minsoo.slimtrack.ui.theme.StatusRedText
import com.minsoo.slimtrack.ui.theme.StatusYellowBg
import com.minsoo.slimtrack.ui.theme.StatusYellowBorder
import com.minsoo.slimtrack.ui.theme.StatusYellowText
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    dashboard: DashboardState,
    currentYearMonth: YearMonth,
    selectedDate: LocalDate,
    selectedDateMeals: List<MealEntity>,
    selectedDateDrinks: List<DrinkEntity>,
    selectedDateExercises: List<ExerciseEntity>,
    syncMessage: String?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSyncHealthConnect: () -> Unit,
    onNavigateToMealTab: () -> Unit,
    onNavigateToCoachTab: () -> Unit,
    onDeleteMeal: (MealEntity) -> Unit,
    onDeleteDrink: (DrinkEntity) -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit = {},
    onChangeMealType: (MealEntity, MealType) -> Unit = { _, _ -> },
    onSaveManualWeight: (Float) -> Unit = {},
    initialExpandedMetric: GoalMetricType? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Stage & Current Body Summary Banner ("1단계 목표")
        StageHeaderCard(
            dashboard = dashboard,
            onSyncClick = onSyncHealthConnect,
            initialExpandedMetric = initialExpandedMetric
        )

        if (!syncMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = syncMessage,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        // 2. Daily Calorie Card placed IMMEDIATELY below "1단계 목표"!
        val selectedEval = dashboard.monthEvaluations[selectedDate] ?: dashboard.todayEvaluation
        SelectedDateDetailCard(
            date = selectedDate,
            effectiveStartDate = dashboard.profile.goalStartDate,
            evaluation = selectedEval,
            meals = selectedDateMeals,
            drinks = selectedDateDrinks,
            exercises = selectedDateExercises,
            isToday = selectedDate == dashboard.today,
            onAddMealClick = onNavigateToMealTab,
            onChangeMealType = onChangeMealType,
            onSaveManualWeight = onSaveManualWeight,
            onDeleteMeal = onDeleteMeal,
            onDeleteDrink = onDeleteDrink,
            onDeleteExercise = onDeleteExercise
        )

        // Skeletal Muscle Loss Warning Banner
        if (dashboard.bodyStatus.stageResult.skeletalMuscleLossWarning) {
            Surface(
                color = StatusRedBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StatusRedBorder, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRedBorder, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format(
                            "골격근 경고: 현재 %.1fkg (기준 %.1fkg 대비 %.1fkg) · 근력운동 필수",
                            dashboard.bodyStatus.latestInBody.skeletalMuscleKg,
                            dashboard.profile.muscleBaselineKg,
                            dashboard.bodyStatus.stageResult.skeletalMuscleDeltaKg
                        ),
                        fontSize = 11.5.sp,
                        color = StatusRedText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. Weekly & Monthly Cumulative Rollup & Recovery Card
        RollupSummaryRow(
            weekly = dashboard.weeklyRollup,
            monthly = dashboard.monthlyRollup,
            baseExerciseTarget = dashboard.bodyStatus.dailyPlan.exerciseTarget
        )

        // 4. Monthly Goal Calendar
        CalendarCard(
            yearMonth = currentYearMonth,
            today = dashboard.today,
            selectedDate = selectedDate,
            effectiveStartDate = dashboard.profile.goalStartDate,
            evaluations = dashboard.monthEvaluations,
            weightLoggedDates = dashboard.weightloggedDates,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onSelectDate = onSelectDate
        )

        // 5. Strict Coach Advice Card
        StrictCoachPreviewCard(
            verdict = dashboard.liveVerdict,
            onOpenCoachTab = onNavigateToCoachTab
        )
    }
}

@Composable
private fun StageHeaderCard(
    dashboard: DashboardState,
    onSyncClick: () -> Unit,
    initialExpandedMetric: GoalMetricType? = null
) {
    val body = dashboard.bodyStatus
    val stage = body.stageResult.activeStageSpec
    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
        dashboard.today,
        body.stageResult.deadlineForActiveStage
    ).toInt().coerceAtLeast(0)

    var expandedMetric by remember { mutableStateOf(initialExpandedMetric) }
    val targetBmi = dashboard.profile.bmi(stage.targetWeightMidKg)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sticker_oh_yeah),
                    contentDescription = "오예 목표 캐릭터",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎯 ${stage.number}단계 목표 (${stage.targetWeightMidFormatted}kg · ${stage.targetVisceralFatMax}Lv)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable(onClick = onSyncClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Health 동기화",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "동기화",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Full-width D-Day & remaining weight row using midpoint target so it never truncates!
            Text(
                text = "🔥 마감 D-${daysLeft} (${body.stageResult.deadlineForActiveStage}) · 목표(${stage.targetWeightMidFormatted}kg)까지 -${String.format("%.1fkg", body.stageResult.remainingKgToActiveStage)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StatusRedText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            LinearProgressIndicator(
                progress = { body.stageResult.weightProgressPct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            // Clickable Metric Boxes: clicking any box opens/toggles the Goal vs Daily Achievement comparison accordion below!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactMetricBox(
                    label = "체중(kg)",
                    value = String.format("%.1f", body.currentWeightKg),
                    targetSubText = "목표 ${stage.targetWeightMidFormatted}",
                    isSelected = expandedMetric == GoalMetricType.WEIGHT,
                    isPrimaryGoal = true,
                    onClick = {
                        expandedMetric = if (expandedMetric == GoalMetricType.WEIGHT) null else GoalMetricType.WEIGHT
                    },
                    modifier = Modifier.weight(1.05f)
                )
                CompactMetricBox(
                    label = "내장지방",
                    value = "${body.latestInBody.visceralFatLevel}Lv",
                    targetSubText = "목표 ${stage.targetVisceralFatMax}Lv",
                    isSelected = expandedMetric == GoalMetricType.VISCERAL_FAT,
                    isPrimaryGoal = true,
                    onClick = {
                        expandedMetric = if (expandedMetric == GoalMetricType.VISCERAL_FAT) null else GoalMetricType.VISCERAL_FAT
                    },
                    modifier = Modifier.weight(1f)
                )
                CompactMetricBox(
                    label = "골격근(kg)",
                    value = String.format("%.1f", body.latestInBody.skeletalMuscleKg),
                    targetSubText = "목표 ${String.format("%.1f", dashboard.profile.muscleBaselineKg)}",
                    isSelected = expandedMetric == GoalMetricType.SKELETAL_MUSCLE,
                    isPrimaryGoal = true,
                    onClick = {
                        expandedMetric = if (expandedMetric == GoalMetricType.SKELETAL_MUSCLE) null else GoalMetricType.SKELETAL_MUSCLE
                    },
                    modifier = Modifier.weight(1.05f)
                )
                CompactMetricBox(
                    label = "공식BMR",
                    value = "${body.bmrKcal}",
                    targetSubText = "대사유지",
                    isSelected = expandedMetric == GoalMetricType.BMR,
                    isPrimaryGoal = false,
                    onClick = {
                        expandedMetric = if (expandedMetric == GoalMetricType.BMR) null else GoalMetricType.BMR
                    },
                    modifier = Modifier.weight(0.95f)
                )
                CompactMetricBox(
                    label = "BMI",
                    value = String.format("%.1f", body.bmi),
                    targetSubText = "목표 ${String.format("%.1f", targetBmi)}",
                    isSelected = expandedMetric == GoalMetricType.BMI,
                    isPrimaryGoal = false,
                    onClick = {
                        expandedMetric = if (expandedMetric == GoalMetricType.BMI) null else GoalMetricType.BMI
                    },
                    modifier = Modifier.weight(0.9f)
                )
            }

            if (expandedMetric == null) {
                Text(
                    text = "👆 위 항목(체중·내장지방·골격근 등)을 클릭하면 목표 그래프 vs 매일 달성 비교 그래프가 열립니다.",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = expandedMetric != null) {
                val activeMetric = expandedMetric ?: GoalMetricType.WEIGHT
                val series = remember(
                    activeMetric,
                    dashboard.today,
                    dashboard.profile,
                    body,
                    dashboard.weightLogs,
                    dashboard.inBodyHistory
                ) {
                    GoalTrajectoryCalculator.buildSeries(
                        metric = activeMetric,
                        today = dashboard.today,
                        profile = dashboard.profile,
                        stageResult = body.stageResult,
                        currentWeightKg = body.currentWeightKg,
                        currentBmrKcal = body.bmrKcal,
                        currentBmi = body.bmi,
                        latestInBody = body.latestInBody,
                        weightLogs = dashboard.weightLogs,
                        inBodyHistory = dashboard.inBodyHistory
                    )
                }
                GoalMetricComparisonAccordion(
                    series = series,
                    onSelectMetric = { expandedMetric = it },
                    onClose = { expandedMetric = null }
                )
            }
        }
    }
}

@Composable
private fun CompactMetricBox(
    label: String,
    value: String,
    targetSubText: String,
    isSelected: Boolean = false,
    isPrimaryGoal: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isPrimaryGoal -> Color(0xFFF0FDFA)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isPrimaryGoal -> Color(0xFF99F6E4)
        else -> Color(0xFFE2E8F0)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "$label${if (isSelected) "▴" else "▾"}",
                fontSize = 9.sp,
                fontWeight = if (isSelected || isPrimaryGoal) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF475569),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF0F172A),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = targetSubText,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF0F766E),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun GoalMetricComparisonAccordion(
    series: GoalMetricComparisonSeries,
    onSelectMetric: (GoalMetricType) -> Unit,
    onClose: () -> Unit
) {
    val metric = series.metric
    val isOnTrack = series.isTodayOnTrack
    val gap = series.todayGapFromTarget
    val gapSign = if (gap > 0f) "+" else ""
    val mmDdFmt = remember { DateTimeFormatter.ofPattern("MM/dd") }

    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Accordion Header + Collapse Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 [${metric.titleKo}] 중간값 목표(${metric.formatValue(series.targetMidValue)}) vs 매일 달성 그래프",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable(onClick = onClose)
                ) {
                    Text(
                        text = "접기 ▲",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 2. Metric Quick-Switch Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GoalMetricType.entries.forEach { m ->
                    val selected = m == metric
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectMetric(m) }
                    ) {
                        Text(
                            text = m.shortLabelKo,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (selected) Color.White else Color(0xFF334155),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }
                }
            }

            // 3. 4-Column Key Numbers Strip (출발 기준 / 오늘 목표치 / 오늘 달성치 / 단계 목표 중간값)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GoalMetricStatPill(
                    title = "시작일(${series.startDate.format(mmDdFmt)})",
                    value = metric.formatValue(series.startValue),
                    valueColor = Color(0xFF334155),
                    modifier = Modifier.weight(1f)
                )
                GoalMetricStatPill(
                    title = "오늘 목표치",
                    value = metric.formatValue(series.todayTargetValue),
                    valueColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )
                GoalMetricStatPill(
                    title = "오늘 달성치",
                    value = metric.formatValue(series.currentValue),
                    valueColor = if (isOnTrack) StatusGreenText else StatusRedText,
                    modifier = Modifier.weight(1f)
                )
                GoalMetricStatPill(
                    title = "목표(중간값)",
                    value = metric.formatValue(series.targetMidValue),
                    valueColor = Color(0xFF0F766E),
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Achievement Pace Status Banner
            val paceSign = if (series.requiredDailyDeltaFromToday > 0f) "+" else ""
            val wkSign = if (series.requiredWeeklyDeltaFromToday > 0f) "+" else ""
            val statusText = if (isOnTrack) {
                "🟢 오늘 목표치(${metric.formatValue(series.todayTargetValue)}) 대비 ${gapSign}${metric.formatValue(gap)} 순항 달성 중! · 지금(${metric.formatValue(series.currentValue)})부터 마감(${series.deadline.format(mmDdFmt)})까지 주간 ${wkSign}${metric.formatValue(series.requiredWeeklyDeltaFromToday)} 페이스"
            } else {
                "🔴 오늘 목표치(${metric.formatValue(series.todayTargetValue)}) 대비 ${gapSign}${metric.formatValue(gap)} 차이 · 지금(${metric.formatValue(series.currentValue)})부터 마감(${series.deadline.format(mmDdFmt)})까지 하루 ${paceSign}${metric.formatValue(series.requiredDailyDeltaFromToday)}(주 ${wkSign}${metric.formatValue(series.requiredWeeklyDeltaFromToday)}) 필요"
            }
            Surface(
                color = if (isOnTrack) StatusGreenBg else StatusRedBg,
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isOnTrack) StatusGreenBorder else StatusRedBorder, RoundedCornerShape(7.dp))
            ) {
                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOnTrack) StatusGreenText else StatusRedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }

            // 5. Graph 1: Daily Target Trajectory vs Daily Actual Achievement (시작일 ~ 오늘 일별 비교)
            DailyTargetVsActualChartCard(series = series)

            // 6. Graph 2: Full Stage Trajectory (시작일 & 현재 수치 기준 → 마감일 중간값 목표 달성 궤적)
            FullStageGoalTrajectoryChartCard(series = series)

            // 7. Recent Day-by-Day Target vs Actual Comparison Table
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "📋 최근 일별 목표치 vs 실제 달성 수치 비교",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    softWrap = false
                )
                series.dailyPoints.reversed().take(5).forEach { pt ->
                    val isTodayRow = pt.date == series.today
                    val diffStr = (if (pt.diffFromPlanTarget > 0f) "+" else "") + metric.formatValue(pt.diffFromPlanTarget)
                    val badgeText = if (pt.isAchievingTargetPace) {
                        "🟢 $diffStr 달성"
                    } else {
                        "🔴 $diffStr 미달"
                    }
                    Surface(
                        color = if (isTodayRow) Color(0xFFF0FDFA) else Color.White,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isTodayRow) 1.dp else 0.5.dp,
                                color = if (isTodayRow) Color(0xFF14B8A6) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(6.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${pt.date.format(mmDdFmt)}(${pt.dayOfWeekKo})${if (isTodayRow) " 오늘" else ""}",
                                fontSize = 10.sp,
                                fontWeight = if (isTodayRow) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.95f)
                            )
                            Text(
                                text = "목표 ${metric.formatValue(pt.planTargetValue)} → 실제 ${metric.formatValue(pt.actualValue)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1.45f)
                            )
                            Text(
                                text = badgeText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (pt.isAchievingTargetPace) StatusGreenText else StatusRedText,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalMetricStatPill(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 8.5.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DailyTargetVsActualChartCard(series: GoalMetricComparisonSeries) {
    val metric = series.metric
    val pts = series.dailyPoints
    val mmDdFmt = remember { DateTimeFormatter.ofPattern("MM/dd") }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "📈 1. 일별 목표 궤적(점선) vs 매일의 달성 그래프(실선)",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                softWrap = false
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "━●━ 매일 실제 달성(${metric.shortLabelKo})",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D9488),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "- - - 시작일 기준 일별 목표선",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                if (pts.isEmpty()) return@Canvas
                val allVals = pts.flatMap { listOf(it.planTargetValue, it.actualValue) }
                val rawMin = allVals.minOrNull() ?: 0f
                val rawMax = allVals.maxOrNull() ?: 100f
                val pad = ((rawMax - rawMin) * 0.25f).coerceAtLeast(0.6f)
                val minV = rawMin - pad
                val maxV = rawMax + pad
                val span = (maxV - minV).coerceAtLeast(0.5f)

                val padX = 12.dp.toPx()
                val padY = 10.dp.toPx()
                val plotW = (size.width - padX * 2f).coerceAtLeast(10f)
                val plotH = (size.height - padY * 2f).coerceAtLeast(10f)

                fun xFor(idx: Int): Float {
                    if (pts.size <= 1) return size.width / 2f
                    return padX + (idx.toFloat() / (pts.size - 1).toFloat()) * plotW
                }

                fun yFor(v: Float): Float {
                    val ratio = ((v - minV) / span).coerceIn(0f, 1f)
                    return padY + plotH * (1f - ratio)
                }

                // Subtle horizontal grid lines
                listOf(0.2f, 0.5f, 0.8f).forEach { r ->
                    val gy = padY + plotH * r
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(padX, gy),
                        end = Offset(size.width - padX, gy),
                        strokeWidth = 1f
                    )
                }

                // Shaded comparison fill between Daily Target line and Daily Actual line
                if (pts.size >= 2) {
                    val areaPath = Path().apply {
                        moveTo(xFor(0), yFor(pts[0].planTargetValue))
                        for (i in 1 until pts.size) {
                            lineTo(xFor(i), yFor(pts[i].planTargetValue))
                        }
                        for (i in pts.size - 1 downTo 0) {
                            lineTo(xFor(i), yFor(pts[i].actualValue))
                        }
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        color = if (series.isTodayOnTrack) {
                            Color(0xFF10B981).copy(alpha = 0.14f)
                        } else {
                            Color(0xFFF59E0B).copy(alpha = 0.14f)
                        }
                    )
                }

                // 1) Dashed Blue Line: Daily Target Trajectory
                if (pts.size >= 2) {
                    for (i in 0 until pts.size - 1) {
                        drawLine(
                            color = Color(0xFF2563EB),
                            start = Offset(xFor(i), yFor(pts[i].planTargetValue)),
                            end = Offset(xFor(i + 1), yFor(pts[i + 1].planTargetValue)),
                            strokeWidth = 3.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                        )
                    }
                }
                pts.forEachIndexed { idx, p ->
                    drawCircle(
                        color = Color(0xFF2563EB),
                        radius = 3.5f,
                        center = Offset(xFor(idx), yFor(p.planTargetValue))
                    )
                }

                // 2) Solid Teal Line: Daily Actual Achievement
                if (pts.size >= 2) {
                    for (i in 0 until pts.size - 1) {
                        drawLine(
                            color = Color(0xFF0D9488),
                            start = Offset(xFor(i), yFor(pts[i].actualValue)),
                            end = Offset(xFor(i + 1), yFor(pts[i + 1].actualValue)),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                pts.forEachIndexed { idx, p ->
                    val dotColor = if (p.isAchievingTargetPace) Color(0xFF0D9488) else Color(0xFFDC2626)
                    val radius = if (idx == pts.lastIndex || p.isMeasuredOnDay) 6f else 4f
                    drawCircle(
                        color = dotColor,
                        radius = radius,
                        center = Offset(xFor(idx), yFor(p.actualValue))
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radius * 0.45f,
                        center = Offset(xFor(idx), yFor(p.actualValue))
                    )
                }
            }

            if (pts.isNotEmpty()) {
                val first = pts.first()
                val last = pts.last()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "시작(${first.date.format(mmDdFmt)}) ${metric.formatValue(first.actualValue)}",
                        fontSize = 9.sp,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "오늘(${last.date.format(mmDdFmt)}) 목표 ${metric.formatValue(last.planTargetValue, false)} vs 실제 ${metric.formatValue(last.actualValue)}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun FullStageGoalTrajectoryChartCard(series: GoalMetricComparisonSeries) {
    val metric = series.metric
    val pts = series.dailyPoints
    val mmDdFmt = remember { DateTimeFormatter.ofPattern("MM/dd") }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "🎯 2. 시작일·지금 기준 → 마감일(${series.deadline}) 중간값(${metric.formatValue(series.targetMidValue)}) 달성 목표 그래프",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "- - 시작일 목표선",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "━▶ 지금(${metric.formatValue(series.currentValue, false)})→목표선",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "━● 매일 달성",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D9488),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val allValues = pts.map { it.actualValue } + listOf(
                    series.startValue,
                    series.currentValue,
                    series.targetMidValue
                )
                val rawMin = allValues.minOrNull() ?: 0f
                val rawMax = allValues.maxOrNull() ?: 100f
                val pad = ((rawMax - rawMin) * 0.22f).coerceAtLeast(0.6f)
                val minV = rawMin - pad
                val maxV = rawMax + pad
                val span = (maxV - minV).coerceAtLeast(0.5f)

                val padX = 12.dp.toPx()
                val padY = 10.dp.toPx()
                val plotW = (size.width - padX * 2f).coerceAtLeast(10f)
                val plotH = (size.height - padY * 2f).coerceAtLeast(10f)

                val totalDays = series.totalStageDays.toFloat().coerceAtLeast(1f)
                // Keep today marker at least 18% from left so daily progress is always visible even early in the stage
                val rawTodayRatio = (series.elapsedDays.toFloat() / totalDays).coerceIn(0f, 0.92f)
                val todayRatio = rawTodayRatio.coerceAtLeast(0.22f)

                fun xForStageRatio(r: Float): Float = padX + r.coerceIn(0f, 1f) * plotW
                fun yFor(v: Float): Float {
                    val ratio = ((v - minV) / span).coerceIn(0f, 1f)
                    return padY + plotH * (1f - ratio)
                }

                val startX = xForStageRatio(0f)
                val todayX = xForStageRatio(todayRatio)
                val endX = xForStageRatio(1f)

                val startY = yFor(series.startValue)
                val currentY = yFor(series.currentValue)
                val targetY = yFor(series.targetMidValue)

                // Horizontal target reference line at targetMidValue
                drawLine(
                    color = Color(0xFF16A34A).copy(alpha = 0.4f),
                    start = Offset(startX, targetY),
                    end = Offset(endX, targetY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Vertical Today divider line
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(todayX, padY),
                    end = Offset(todayX, size.height - padY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Line 1 (Blue Dashed): Start Date -> Deadline Midpoint Target Trajectory
                drawLine(
                    color = Color(0xFF2563EB),
                    start = Offset(startX, startY),
                    end = Offset(endX, targetY),
                    strokeWidth = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )

                // Line 2 (Orange/Amber Dashed): Current Today Value -> Deadline Midpoint Target Trajectory
                drawLine(
                    color = Color(0xFFD97706),
                    start = Offset(todayX, currentY),
                    end = Offset(endX, targetY),
                    strokeWidth = 4.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )

                // Line 3 (Teal Solid): Daily Actual Progress from Start Date up to Today
                if (pts.size >= 2) {
                    for (i in 0 until pts.size - 1) {
                        val r1 = (i.toFloat() / (pts.size - 1).toFloat()) * todayRatio
                        val r2 = ((i + 1).toFloat() / (pts.size - 1).toFloat()) * todayRatio
                        drawLine(
                            color = Color(0xFF0D9488),
                            start = Offset(xForStageRatio(r1), yFor(pts[i].actualValue)),
                            end = Offset(xForStageRatio(r2), yFor(pts[i + 1].actualValue)),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    drawLine(
                        color = Color(0xFF0D9488),
                        start = Offset(startX, startY),
                        end = Offset(todayX, currentY),
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                // Key Milestone Dots: Start, Today Current, Deadline Midpoint Target
                drawCircle(color = Color(0xFF2563EB), radius = 6f, center = Offset(startX, startY))
                drawCircle(color = Color(0xFF0D9488), radius = 7.5f, center = Offset(todayX, currentY))
                drawCircle(color = Color.White, radius = 3.5f, center = Offset(todayX, currentY))
                drawCircle(color = Color(0xFF16A34A), radius = 7.5f, center = Offset(endX, targetY))
                drawCircle(color = Color.White, radius = 3.5f, center = Offset(endX, targetY))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "시작(${series.startDate.format(mmDdFmt)}) ${metric.formatValue(series.startValue)}",
                    fontSize = 9.sp,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "지금(오늘) ${metric.formatValue(series.currentValue)}",
                    fontSize = 9.sp,
                    color = Color(0xFF0D9488),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "목표(${series.deadline.format(mmDdFmt)}) ${metric.formatValue(series.targetMidValue)}",
                    fontSize = 9.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun RollupSummaryRow(
    weekly: PeriodRollup,
    monthly: PeriodRollup,
    baseExerciseTarget: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RollupCard(
            title = "주간 누적 (유효일~일)",
            cumulative = weekly.cumulativeBalanceKcal,
            remainingDays = weekly.remainingDaysInPeriod,
            recoveryPerDay = weekly.recoveryPerDayKcal,
            modifier = Modifier.weight(1f)
        )
        RollupCard(
            title = "월간 누적 (유효일~말일)",
            cumulative = monthly.cumulativeBalanceKcal,
            remainingDays = monthly.remainingDaysInPeriod,
            recoveryPerDay = monthly.recoveryPerDayKcal,
            modifier = Modifier.weight(1f)
        )
    }

    if (weekly.recoveryPerDayKcal > 0) {
        Surface(
            color = StatusYellowBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StatusYellowBorder, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "⚡ 주간 만회: 권장섭취 ${weekly.adjustedRecommendedIntakeToday}kcal · 운동목표 ${weekly.adjustedExerciseTargetToday}kcal (기본 ${baseExerciseTarget}+만회 ${weekly.adjustedExerciseTargetToday - baseExerciseTarget})",
                fontSize = 11.sp,
                color = StatusYellowText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun RollupCard(
    title: String,
    cumulative: Int,
    remainingDays: Int,
    recoveryPerDay: Int,
    modifier: Modifier = Modifier
) {
    val isPositive = cumulative >= 0
    val bg = if (isPositive) StatusGreenBg else StatusRedBg
    val border = if (isPositive) StatusGreenBorder else StatusRedBorder
    val fg = if (isPositive) StatusGreenText else StatusRedText

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, border, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = if (cumulative >= 0) "${cumulative} kcal 여유" else "${cumulative} kcal 초과",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = fg,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = if (recoveryPerDay > 0) {
                    "만회 -${recoveryPerDay}kcal/일 (${remainingDays}일 남음)"
                } else {
                    "목표 순항 중 (${remainingDays}일 남음)"
                },
                fontSize = 10.sp,
                color = fg,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CalendarCard(
    yearMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    effectiveStartDate: LocalDate,
    evaluations: Map<LocalDate, DayEvaluation>,
    weightLoggedDates: Set<LocalDate>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val monthlyDrinkDaysCount = evaluations.values.count { it.drinkIntakeKcal > 0 }
    val monthlyDrinkTotalKcal = evaluations.values.sumOf { it.drinkIntakeKcal }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Top Header Row: Month Navigation on Left + Monthly Drink Count Badge on Right Corner!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
                    }
                    Text(
                        text = "${yearMonth.year}년 ${yearMonth.monthValue}월 캘린더",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onNextMonth, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
                    }
                }

                // Top-Right Corner Monthly Alcohol Count Badge
                val drinkBadgeBg = if (monthlyDrinkDaysCount > 0) StatusRedBg else StatusGreenBg
                val drinkBadgeBorder = if (monthlyDrinkDaysCount > 0) StatusRedBorder else StatusGreenBorder
                val drinkBadgeFg = if (monthlyDrinkDaysCount > 0) StatusRedText else StatusGreenText
                Surface(
                    color = drinkBadgeBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, drinkBadgeBorder, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = if (monthlyDrinkTotalKcal > 0) {
                            "🍺 월 ${monthlyDrinkDaysCount}회 (${monthlyDrinkTotalKcal}kcal)"
                        } else {
                            "🍺 월 ${monthlyDrinkDaysCount}회 음주"
                        },
                        color = drinkBadgeFg,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Legend (compact single line including 🍺 음주일)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendChip("🟢 달성", StatusGreenBg, StatusGreenText)
                LegendChip("🟡 허용", StatusYellowBg, StatusYellowText)
                LegendChip("🔴 초과/✕미기록", StatusRedBg, StatusRedText)
                LegendChip("🍺 음주일", StatusRedBg, StatusRedText)
                LegendChip("⚪ 시작전/미래", StatusGrayBg, StatusGrayText)
            }

            val weekDays = listOf("월", "화", "수", "목", "금", "토", "일")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            val firstDayOfMonth = yearMonth.atDay(1)
            val leadingEmpty = firstDayOfMonth.dayOfWeek.value - 1
            val daysInMonth = yearMonth.lengthOfMonth()
            val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7

            for (row in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - leadingEmpty + 1
                        if (dayNum in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNum)
                            val eval = evaluations[date]
                            val hasWeight = date in weightLoggedDates
                            CalendarDayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                evaluation = eval,
                                hasWeightDot = hasWeight,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).height(46.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendChip(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(5.dp)) {
        Text(
            text = text,
            color = fg,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

/**
 * Calendar Day Cell:
 * - Positive remaining intake is displayed WITHOUT a '+' sign (e.g. "713").
 * - Exceeded intake is displayed with '-' (e.g. "-240").
 * - Missing meal on valid dates (>= effectiveStartDate) draws a Canvas '✕' icon.
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    evaluation: DayEvaluation?,
    hasWeightDot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = evaluation?.statusColor ?: DayStatusColor.GRAY
    val bg = when (color) {
        DayStatusColor.GREEN -> StatusGreenBg
        DayStatusColor.YELLOW -> StatusYellowBg
        DayStatusColor.RED -> StatusRedBg
        DayStatusColor.GRAY -> StatusGrayBg
    }
    val fg = when (color) {
        DayStatusColor.GREEN -> StatusGreenText
        DayStatusColor.YELLOW -> StatusYellowText
        DayStatusColor.RED -> StatusRedText
        DayStatusColor.GRAY -> StatusGrayText
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> Color(0xFF0284C7)
        color == DayStatusColor.GREEN -> StatusGreenBorder
        color == DayStatusColor.YELLOW -> StatusYellowBorder
        color == DayStatusColor.RED -> StatusRedBorder
        else -> StatusGrayBorder
    }
    val borderWidth = if (isSelected || isToday) 2.dp else 1.dp

    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    val dynamicCellHeight = (46f * maxOf(1f, fontScale * 0.92f)).dp

    Box(
        modifier = modifier
            .height(dynamicCellHeight)
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val hasDrink = (evaluation?.drinkIntakeKcal ?: 0) > 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = fg,
                    maxLines = 1,
                    softWrap = false
                )
                if (hasDrink) {
                    Spacer(Modifier.width(1.dp))
                    Text(
                        text = "🍺",
                        fontSize = 8.5.sp,
                        lineHeight = 10.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                } else if (hasWeightDot) {
                    Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            if (evaluation != null && color != DayStatusColor.GRAY) {
                if (evaluation.isMissingMealFailure) {
                    Canvas(modifier = Modifier.size(11.dp)) {
                        val s = size.minDimension
                        drawCircle(color = StatusRedBorder.copy(alpha = 0.2f), radius = s / 2f)
                        val pad = s * 0.26f
                        drawLine(
                            color = StatusRedText,
                            start = Offset(pad, pad),
                            end = Offset(s - pad, s - pad),
                            strokeWidth = 2.2f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = StatusRedText,
                            start = Offset(s - pad, pad),
                            end = Offset(pad, s - pad),
                            strokeWidth = 2.2f,
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    // No '+' prefix for positive remaining calories!
                    val rem = evaluation.remainingIntakeKcal
                    Text(
                        text = "$rem",
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = fg,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SelectedDateDetailCard(
    date: LocalDate,
    effectiveStartDate: LocalDate,
    evaluation: DayEvaluation,
    meals: List<MealEntity>,
    drinks: List<DrinkEntity>,
    exercises: List<ExerciseEntity>,
    isToday: Boolean = false,
    onAddMealClick: () -> Unit,
    onChangeMealType: (MealEntity, MealType) -> Unit = { _, _ -> },
    onSaveManualWeight: (Float) -> Unit = {},
    onDeleteMeal: (MealEntity) -> Unit,
    onDeleteDrink: (DrinkEntity) -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit
) {
    val rem = evaluation.remainingIntakeKcal
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dailyStickerRes = if (rem >= 0 && !evaluation.isMissingMealFailure) {
                    R.drawable.sticker_good_job
                } else {
                    R.drawable.sticker_sorry
                }
                Image(
                    painter = painterResource(id = dailyStickerRes),
                    contentDescription = "오늘의 상태 스티커",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "📅 $date 일일 칼로리 수치",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.clickable(onClick = onAddMealClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "식단 입력",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Full-width highlighted banner for remaining intakeable calories so it NEVER truncates!
            val subtitleText = when {
                date.isBefore(effectiveStartDate) -> "⚪ 기록 유효 시작일($effectiveStartDate) 이전 날짜"
                evaluation.isMissingMealFailure -> "🔴 식사 미기록 (실패 · 오늘 섭취 가능 ${evaluation.plan.recommendedIntake} kcal)"
                rem >= 0 -> "🟢 오늘 앞으로 ${rem} kcal 더 섭취 가능"
                else -> "🔴 오늘 허용 칼로리 ${-rem} kcal 초과 섭취"
            }
            val pillBg = when {
                date.isBefore(effectiveStartDate) -> StatusGrayBg
                evaluation.statusColor == DayStatusColor.RED -> StatusRedBg
                evaluation.statusColor == DayStatusColor.YELLOW -> StatusYellowBg
                else -> StatusGreenBg
            }
            val pillBorder = when {
                date.isBefore(effectiveStartDate) -> StatusGrayBorder
                evaluation.statusColor == DayStatusColor.RED -> StatusRedBorder
                evaluation.statusColor == DayStatusColor.YELLOW -> StatusYellowBorder
                else -> StatusGreenBorder
            }
            val subtitleColor = when {
                date.isBefore(effectiveStartDate) -> StatusGrayText
                evaluation.statusColor == DayStatusColor.RED -> StatusRedText
                evaluation.statusColor == DayStatusColor.YELLOW -> StatusYellowText
                else -> StatusGreenText
            }
            Surface(
                color = pillBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, pillBorder, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = subtitleText,
                    fontSize = 12.sp,
                    color = subtitleColor,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            // Visual Canvas Calorie Gauge + 4-Column Metric Grid
            CalorieBalanceGraphicBox(evaluation = evaluation)

            // 🥩 Daily Protein Status Bar
            val consumedProtein = meals.sumOf { it.proteinGrams.toDouble() }.toFloat()
            DailyProteinStatusBar(
                consumedProteinGrams = consumedProtein,
                targetProteinGrams = evaluation.targetProteinGrams
            )

            // Quick Today Weight direct input bar
            if (isToday) {
                var quickWeightStr by remember { mutableStateOf("") }
                Surface(
                    color = Color(0xFFF0FDFA),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF5EEAD4), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickWeightStr,
                            onValueChange = { quickWeightStr = it },
                            placeholder = {
                                Text(
                                    text = "오늘 체중 즉시반영 (예: 97.5)",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val w = quickWeightStr.trim().toFloatOrNull()
                                if (w != null && w in 30f..250f) {
                                    onSaveManualWeight(w)
                                    quickWeightStr = ""
                                }
                            },
                            enabled = quickWeightStr.trim().toFloatOrNull()?.let { it in 30f..250f } == true,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "체중 즉시반영",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            if (meals.isEmpty() && drinks.isEmpty() && exercises.isEmpty()) {
                Text(
                    text = "등록된 식사·술·운동 기록이 없습니다. 우측 상단 [식단 입력]을 눌러 추가하세요.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                if (meals.isNotEmpty()) {
                    Text(
                        text = "🍽 식사 & 안주 (${meals.size}건 · 총 ${meals.sumOf { it.totalKcal }}kcal · 터치 시 상세 펼침)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    meals.forEach { m ->
                        ExpandableMealAccordionCard(
                            meal = m,
                            initiallyExpanded = false,
                            onDeleteMeal = { onDeleteMeal(m) },
                            onChangeMealType = { newType -> onChangeMealType(m, newType) }
                        )
                    }
                }

                if (drinks.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "🍺 음주 기록 (${drinks.sumOf { it.kcal }}kcal)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    drinks.forEach { d ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.LocalBar, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusRedBorder)
                                    Spacer(Modifier.width(5.dp))
                                    val drinkText = if (d.displayLabel.contains("*")) {
                                        "${d.displayLabel} — ${d.kcal}kcal"
                                    } else {
                                        "${d.displayLabel}: ${d.volumeMl}ml * ${d.abvPct}% * 1회 — ${d.kcal}kcal"
                                    }
                                    Text(
                                        text = drinkText,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                DeletePillButton(onClick = { onDeleteDrink(d) })
                            }
                        }
                    }
                }

                if (exercises.isNotEmpty()) {
                    HorizontalDivider()
                    val stepEx = exercises.firstOrNull { it.source == "HC_STEPS" }
                    val stepSuffix = if (stepEx != null) " · 걸음 ${stepEx.steps}보(4천보 초과 +${stepEx.activeKcal}kcal)" else ""
                    Text(
                        text = "🏃 운동 & 도보 소모 (${exercises.sumOf { it.activeKcal }}kcal$stepSuffix)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    exercises.forEach { ex ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusGreenBorder)
                                    Spacer(Modifier.width(5.dp))
                                    val exDisplay = if (ex.source == "HC_STEPS") {
                                        val bonus = (ex.steps - 4000).coerceAtLeast(0)
                                        "👟 +${ex.activeKcal}kcal · 도보 ${ex.steps}보(초과 ${bonus}보)"
                                    } else {
                                        "+${ex.activeKcal}kcal · [${ex.source}] ${ex.title}(${ex.durationMinutes}분)"
                                    }
                                    Text(
                                        text = exDisplay,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                DeletePillButton(onClick = { onDeleteExercise(ex) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletePillButton(onClick: () -> Unit) {
    Surface(
        color = StatusRedBg,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .border(1.dp, StatusRedBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "삭제",
                tint = StatusRedText,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "삭제",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = StatusRedText,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun CalorieBalanceGraphicBox(evaluation: DayEvaluation) {
    val plan = evaluation.plan
    val allowedBudget = (plan.recommendedIntake + evaluation.exerciseKcal).coerceAtLeast(plan.bmr)
    val intakeRatio = (evaluation.totalIntakeKcal.toFloat() / allowedBudget.coerceAtLeast(1)).coerceIn(0f, 1.25f)
    val rem = evaluation.remainingIntakeKcal
    val barColor = when {
        evaluation.statusColor == DayStatusColor.RED && !evaluation.isMissingMealFailure -> StatusRedBorder
        evaluation.statusColor == DayStatusColor.YELLOW -> StatusYellowBorder
        rem >= 0 -> StatusGreenBorder
        else -> StatusRedBorder
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Visual Canvas Gauge Bar
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "섭취 ${evaluation.totalIntakeKcal} / 허용 ${allowedBudget}kcal",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = if (rem >= 0) "잔여 ${rem}kcal" else "${-rem}kcal 초과",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rem >= 0) StatusGreenText else StatusRedText,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    drawRoundRect(
                        color = Color(0xFFE2E8F0),
                        size = size,
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    val fillW = (size.width * (intakeRatio / 1.25f)).coerceAtMost(size.width)
                    drawRoundRect(
                        color = barColor,
                        size = Size(fillW, size.height),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    // 100% target marker line
                    val markerX = size.width * (1.0f / 1.25f)
                    drawLine(
                        color = Color(0xFF0F172A),
                        start = Offset(markerX, 0f),
                        end = Offset(markerX, size.height),
                        strokeWidth = 3f
                    )
                }
            }

            // 4-column single-line metric tiles: 4th tile ("섭취 가능") uses high-contrast solid badge styling (same 13.sp font size, maximum visual pop!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BalanceTile("권장 섭취", "${plan.recommendedIntake}", "kcal", Color(0xFF0F172A), Modifier.weight(1f))
                BalanceTile("실제 섭취", "${evaluation.totalIntakeKcal}", "kcal", Color(0xFF0F172A), Modifier.weight(1f))
                BalanceTile("운동·도보", "${evaluation.exerciseKcal}", "kcal", StatusGreenText, Modifier.weight(1f))
                HighlightedRemainingTile(
                    label = if (rem >= 0) "✨ 섭취 가능" else "⚠️ 초과 칼로리",
                    numberText = "$rem",
                    unit = "kcal",
                    isPositive = rem >= 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BalanceTile(
    label: String,
    numberText: String,
    unit: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = numberText,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = unit,
                fontSize = 8.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DailyProteinStatusBar(
    consumedProteinGrams: Float,
    targetProteinGrams: Int
) {
    val consumed = DailyBeveragePreset.formatMacroGrams(consumedProteinGrams)
    val target = targetProteinGrams.coerceAtLeast(1)
    val pct = ((consumedProteinGrams / target.toFloat()) * 100f).roundToInt()
    val remaining = (target.toFloat() - consumedProteinGrams).coerceAtLeast(0f)
    val remainingStr = DailyBeveragePreset.formatMacroGrams(remaining)
    val isAchieved = consumedProteinGrams >= target.toFloat()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isAchieved) StatusGreenBorder else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🥩 일일 단백질 섭취량",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = if (isAchieved) StatusGreenBg else Color(0xFFE0E7FF),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(
                            1.dp,
                            if (isAchieved) StatusGreenBorder else Color(0xFFA5B4FC),
                            RoundedCornerShape(4.dp)
                        )
                    ) {
                        Text(
                            text = if (isAchieved) "달성 $pct%" else "$pct%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAchieved) StatusGreenText else Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
                Text(
                    text = "${consumed}g / ${target}g",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAchieved) StatusGreenText else Color(0xFF0F172A)
                )
            }

            // Visual Progress Gauge Bar
            val ratio = (consumedProteinGrams / target.toFloat()).coerceIn(0f, 1.25f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                drawRoundRect(
                    color = Color(0xFFE2E8F0),
                    size = size,
                    cornerRadius = CornerRadius(4f, 4f)
                )
                val fillW = (size.width * (ratio / 1.25f)).coerceAtMost(size.width)
                val barColor = when {
                    isAchieved -> Color(0xFF10B981) // Emerald Green
                    pct >= 70 -> Color(0xFF0D9488)  // Teal
                    pct >= 40 -> Color(0xFF6366F1)  // Indigo
                    else -> Color(0xFFF59E0B)       // Amber
                }
                drawRoundRect(
                    color = barColor,
                    size = Size(fillW, size.height),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // 100% target marker line
                val markerX = size.width * (1.0f / 1.25f)
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(markerX, 0f),
                    end = Offset(markerX, size.height),
                    strokeWidth = 2.5f
                )
            }

            // Subtext summary
            val statusText = if (isAchieved) {
                val overG = DailyBeveragePreset.formatMacroGrams(consumedProteinGrams - target.toFloat())
                "🎉 일일 권장량 완벽 달성! (+${overG}g 초과 · 골격근 방어)"
            } else {
                "⚡ 목표까지 ${remainingStr}g 남음 (하루 권장: ${target}g)"
            }
            Text(
                text = statusText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAchieved) StatusGreenText else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * High-contrast solid badge tile for "섭취 가능" (Remaining Intakeable Calories):
 * Stacks label, number, and unit vertically so even at 140%~155% presbyopia font scale
 * the 4-digit calorie number is large, bold, and never clipped horizontally.
 */
@Composable
private fun HighlightedRemainingTile(
    label: String,
    numberText: String,
    unit: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isPositive) Color(0xFF047857) else Color(0xFFDC2626)
    val border = if (isPositive) Color(0xFF10B981) else Color(0xFFF87171)
    val labelColor = if (isPositive) Color(0xFFA7F3D0) else Color(0xFFFECACA)

    Surface(
        color = bg,
        shape = RoundedCornerShape(7.dp),
        modifier = modifier.border(2.dp, border, RoundedCornerShape(7.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = numberText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = unit,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun StrictCoachPreviewCard(
    verdict: com.minsoo.slimtrack.domain.AdviceVerdict,
    onOpenCoachTab: () -> Unit
) {
    val isBad = verdict.grade in listOf("C", "D", "F")
    val bg = if (isBad) StatusRedBg else StatusGreenBg
    val border = if (isBad) StatusRedBorder else StatusGreenBorder
    val fg = if (isBad) StatusRedText else StatusGreenText

    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenCoachTab)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        id = if (isBad) R.drawable.sticker_cheer_up else R.drawable.sticker_best
                    ),
                    contentDescription = "코치 스티커",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(border),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = verdict.grade,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = verdict.headlineKo,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = fg,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "상세 보기",
                    tint = fg
                )
            }
            if (verdict.actionsTomorrowKo.isNotEmpty()) {
                Text(
                    text = "내일 필수: ${verdict.actionsTomorrowKo.first()}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
