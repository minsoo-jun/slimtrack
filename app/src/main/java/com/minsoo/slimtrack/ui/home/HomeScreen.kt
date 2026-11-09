package com.minsoo.slimtrack.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.R
import com.minsoo.slimtrack.data.db.DrinkEntity
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.MealEntity
import com.minsoo.slimtrack.data.db.MealType
import com.minsoo.slimtrack.data.repo.DashboardState
import com.minsoo.slimtrack.domain.DayEvaluation
import com.minsoo.slimtrack.domain.DayStatusColor
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
            onSyncClick = onSyncHealthConnect
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
            onAddMealClick = onNavigateToMealTab,
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
    onSyncClick: () -> Unit
) {
    val body = dashboard.bodyStatus
    val stage = body.stageResult.activeStageSpec
    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
        dashboard.today,
        body.stageResult.deadlineForActiveStage
    ).toInt().coerceAtLeast(0)

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
                Image(
                    painter = painterResource(id = R.drawable.sticker_oh_yeah),
                    contentDescription = "오예 목표 캐릭터",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "🎯 ${stage.number}단계 목표 (${stage.targetWeightMinKg.toInt()}~${stage.targetWeightMaxKg.toInt()}kg · 내장지방 ${stage.targetVisceralFatMax}Lv)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "🔥 마감 D-${daysLeft} (${daysLeft}일 남음 · ${body.stageResult.deadlineForActiveStage}) · 남은 감량 ${String.format("%.1fkg", body.stageResult.remainingKgToActiveStage)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusRedText,
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Health 동기화",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Health 동기화",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { body.stageResult.weightProgressPct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactMetricBox("현재체중", String.format("%.1f", body.currentWeightKg), "kg", Modifier.weight(1f))
                CompactMetricBox("내장지방", "${body.latestInBody.visceralFatLevel}", "Lv", Modifier.weight(1f))
                CompactMetricBox("골격근량", String.format("%.1f", body.latestInBody.skeletalMuscleKg), "kg", Modifier.weight(1f))
                CompactMetricBox("공식BMR", "${body.bmrKcal}", "kcal", Modifier.weight(1f))
                CompactMetricBox("BMI", String.format("%.1f", body.bmi), "", Modifier.weight(0.85f))
            }
        }
    }
}

@Composable
private fun CompactMetricBox(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                softWrap = false
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        fontSize = 8.5.sp,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = 1.dp, bottom = 1.dp)
                    )
                }
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

    Box(
        modifier = modifier
            .height(46.dp)
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
    onAddMealClick: () -> Unit,
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "📅 $date 일일 칼로리 수치",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(1.dp, pillBorder, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = subtitleText,
                            fontSize = 11.5.sp,
                            color = subtitleColor,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.clickable(onClick = onAddMealClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "식단 입력",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Visual Canvas Calorie Gauge + 4-Column Metric Grid
            CalorieBalanceGraphicBox(evaluation = evaluation)

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
                            onDeleteMeal = { onDeleteMeal(m) }
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
                                        "👟 [Health] 도보 ${ex.steps}보(초과 ${bonus}보) — +${ex.activeKcal}kcal"
                                    } else {
                                        "[${ex.source}] ${ex.title} (${ex.durationMinutes}분) — +${ex.activeKcal}kcal"
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
                        text = "실제 섭취 ${evaluation.totalIntakeKcal} / 허용량(권장+운동) ${allowedBudget} kcal",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = if (rem >= 0) "섭취 가능 ${rem} kcal" else "${-rem} kcal 초과",
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
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
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
            Row(verticalAlignment = Alignment.Bottom) {
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
                    fontSize = 8.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 1.dp, bottom = 1.dp)
                )
            }
        }
    }
}

/**
 * High-contrast solid badge tile for "섭취 가능" (Remaining Intakeable Calories):
 * Keeps the exact same 13.sp font size so it never overflows or wraps, while standing out
 * immediately via a vivid solid emerald/crimson background, 2.dp accent border, and crisp white text.
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
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = numberText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = unit,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 1.dp, bottom = 1.dp)
                )
            }
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
