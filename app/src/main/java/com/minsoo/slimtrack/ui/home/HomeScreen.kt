package com.minsoo.slimtrack.ui.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.data.db.DrinkEntity
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.MealEntity
import com.minsoo.slimtrack.data.db.MealType
import com.minsoo.slimtrack.data.repo.DashboardState
import com.minsoo.slimtrack.domain.DayEvaluation
import com.minsoo.slimtrack.domain.DayStatusColor
import com.minsoo.slimtrack.domain.PeriodRollup
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
import com.minsoo.slimtrack.ui.theme.TealPrimary
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
    onQuickAddWeight: (LocalDate, Float) -> Unit,
    onSyncHealthConnect: () -> Unit,
    onNavigateToMealTab: () -> Unit,
    onNavigateToCoachTab: () -> Unit,
    onDeleteMeal: (MealEntity) -> Unit,
    onDeleteDrink: (DrinkEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWeightDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Stage & Current Body Summary Banner
        StageHeaderCard(
            dashboard = dashboard,
            onQuickWeightClick = { showWeightDialog = true },
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

        // 2. Weekly & Monthly Cumulative +/- Rollup & Recovery Card
        RollupSummaryRow(
            weekly = dashboard.weeklyRollup,
            monthly = dashboard.monthlyRollup,
            baseExerciseTarget = dashboard.bodyStatus.dailyPlan.exerciseTarget
        )

        // 3. Monthly Goal Calendar
        CalendarCard(
            yearMonth = currentYearMonth,
            today = dashboard.today,
            selectedDate = selectedDate,
            evaluations = dashboard.monthEvaluations,
            weightLoggedDates = dashboard.weightloggedDates,
            onPrevMonth = onPrevMonth,
            onNextMonth = onNextMonth,
            onSelectDate = onSelectDate
        )

        // 4. Selected Date Balance Breakdown & Records
        val selectedEval = dashboard.monthEvaluations[selectedDate] ?: dashboard.todayEvaluation
        SelectedDateDetailCard(
            date = selectedDate,
            evaluation = selectedEval,
            meals = selectedDateMeals,
            drinks = selectedDateDrinks,
            exercises = selectedDateExercises,
            onAddMealClick = onNavigateToMealTab,
            onDeleteMeal = onDeleteMeal,
            onDeleteDrink = onDeleteDrink
        )

        // 5. Strict Coach Advice Card
        StrictCoachPreviewCard(
            verdict = dashboard.liveVerdict,
            onOpenCoachTab = onNavigateToCoachTab
        )
    }

    if (showWeightDialog) {
        QuickWeightInputDialog(
            date = selectedDate,
            initialWeight = dashboard.bodyStatus.currentWeightKg.toFloat(),
            onDismiss = { showWeightDialog = false },
            onSave = { w ->
                onQuickAddWeight(selectedDate, w)
                showWeightDialog = false
            }
        )
    }
}

@Composable
private fun StageHeaderCard(
    dashboard: DashboardState,
    onQuickWeightClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val body = dashboard.bodyStatus
    val stage = body.stageResult.activeStage
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
                Column(modifier = Modifier.weight(1f)) {
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
                        text = "남은 감량 ${String.format("%.1fkg", body.stageResult.remainingKgToActiveStage)} · 마감 ${body.stageResult.deadlineForActiveStage}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable(onClick = onQuickWeightClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MonitorWeight,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "체중입력",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(onClick = onSyncClick)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "동기화",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
            title = "주간 누적 (월~일)",
            cumulative = weekly.cumulativeBalanceKcal,
            remainingDays = weekly.remainingDaysInPeriod,
            recoveryPerDay = weekly.recoveryPerDayKcal,
            modifier = Modifier.weight(1f)
        )
        RollupCard(
            title = "월간 누적",
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
                text = String.format("%+d kcal", cumulative),
                fontSize = 16.sp,
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
    evaluations: Map<LocalDate, DayEvaluation>,
    weightLoggedDates: Set<LocalDate>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
                }
                Text(
                    text = "${yearMonth.year}년 ${yearMonth.monthValue}월 목표 캘린더",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
                }
            }

            // Legend (compact single line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendChip("🟢 달성", StatusGreenBg, StatusGreenText)
                LegendChip("🟡 허용(-150)", StatusYellowBg, StatusYellowText)
                LegendChip("🔴 초과/✕미기록", StatusRedBg, StatusRedText)
                LegendChip("⚪ 미래", StatusGrayBg, StatusGrayText)
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
 * Calendar Day Cell rendered with Canvas status indicator so text is never clipped vertically.
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
                if (hasWeightDot) {
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
                    // Graphic X icon drawn via Canvas so Korean text "미기록" never clips
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
                    Text(
                        text = String.format("%+d", evaluation.netBalanceKcal),
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
    evaluation: DayEvaluation,
    meals: List<MealEntity>,
    drinks: List<DrinkEntity>,
    exercises: List<ExerciseEntity>,
    onAddMealClick: () -> Unit,
    onDeleteMeal: (MealEntity) -> Unit,
    onDeleteDrink: (DrinkEntity) -> Unit
) {
    val plan = evaluation.plan
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row: Left title (weight 1f) + Right compact pill button (never squished!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📅 $date 일일 칼로리 수지",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (evaluation.isMissingMealFailure) {
                            "🔴 식사 미기록 (실패 · 21:00 경고 알람)"
                        } else {
                            String.format("일일 수지 %+d kcal (목표 적자 -%dkcal)", evaluation.netBalanceKcal, plan.deficitTarget)
                        },
                        fontSize = 11.5.sp,
                        color = if (evaluation.statusColor == DayStatusColor.RED) StatusRedText else StatusGreenText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
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
                            text = "기록 추가",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Visual Canvas Calorie Gauge + 2x2 Grid (Zero line-wrap!)
            CalorieBalanceGraphicBox(evaluation = evaluation)

            if (meals.isEmpty() && drinks.isEmpty() && exercises.isEmpty()) {
                Text(
                    text = "등록된 식사·술·운동 기록이 없습니다.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                if (meals.isNotEmpty()) {
                    Text(
                        text = "🍽 식사 & 안주 (${meals.sumOf { it.totalKcal }}kcal)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    meals.forEach { m ->
                        val typeLabel = runCatching { MealType.valueOf(m.mealType).labelKo }.getOrDefault(m.mealType)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "[$typeLabel] ${m.totalKcal}kcal (단백질 ${m.proteinGrams.toInt()}g) ${m.notes}",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDeleteMeal(m) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", modifier = Modifier.size(15.dp))
                            }
                        }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocalBar, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusRedBorder)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "${d.displayLabel} ${d.volumeMl}ml (${d.abvPct}%) — ${d.kcal}kcal",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDeleteDrink(d) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                if (exercises.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "🏃 운동 소모 (+${exercises.sumOf { it.activeKcal }}kcal)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    exercises.forEach { ex ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusGreenBorder)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "[${ex.source}] ${ex.title} (${ex.durationMinutes}분) — +${ex.activeKcal}kcal",
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalorieBalanceGraphicBox(evaluation: DayEvaluation) {
    val plan = evaluation.plan
    val allowedBudget = (plan.recommendedIntake + evaluation.exerciseKcal - plan.exerciseTarget).coerceAtLeast(plan.bmr)
    val intakeRatio = (evaluation.totalIntakeKcal.toFloat() / allowedBudget.coerceAtLeast(1)).coerceIn(0f, 1.25f)
    val barColor = when (evaluation.statusColor) {
        DayStatusColor.GREEN -> StatusGreenBorder
        DayStatusColor.YELLOW -> StatusYellowBorder
        DayStatusColor.RED -> StatusRedBorder
        DayStatusColor.GRAY -> StatusGrayBorder
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
                        text = "섭취 ${evaluation.totalIntakeKcal} / 허용예산 ${allowedBudget} kcal",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = String.format("오늘 수지 %+d kcal", evaluation.netBalanceKcal),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = barColor,
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

            // 4-column single-line metric tiles (unit separated so "kcal" NEVER wraps!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BalanceTile("권장 섭취", "${plan.recommendedIntake}", "kcal", Color(0xFF0F172A), Modifier.weight(1f))
                BalanceTile("실제 섭취", "${evaluation.totalIntakeKcal}", "kcal", Color(0xFF0F172A), Modifier.weight(1f))
                BalanceTile("운동(목표${plan.exerciseTarget})", "+${evaluation.exerciseKcal}", "kcal", StatusGreenText, Modifier.weight(1f))
                BalanceTile("오늘 ±", String.format("%+d", evaluation.netBalanceKcal), "kcal", barColor, Modifier.weight(1f))
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
                // Graphic Grade Badge Circle
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

@Composable
private fun QuickWeightInputDialog(
    date: LocalDate,
    initialWeight: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var weightText by remember { mutableStateOf(String.format("%.1f", initialWeight)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚖ $date 체중 수동 입력", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("아침 기상 직후 공복 측정을 권장합니다.", fontSize = 12.sp)
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("체중 (kg)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightText.toFloatOrNull()
                    if (w != null && w in 35f..250f) {
                        onSave(w)
                    }
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
