package com.minsoo.slimtrack.ui.home

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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp)
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
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRedBorder)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format(
                            "골격근 손실 경고: 현재 %.1fkg (기준 %.1fkg 대비 %.1fkg 감소). 근력 운동과 단백질 섭취를 즉시 늘리십시오!",
                            dashboard.bodyStatus.latestInBody.skeletalMuscleKg,
                            dashboard.profile.muscleBaselineKg,
                            dashboard.bodyStatus.stageResult.skeletalMuscleDeltaKg
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRedText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Weekly & Monthly Cumulative +/- Rollup & Recovery Card
        RollupSummaryRow(
            weekly = dashboard.weeklyRollup,
            monthly = dashboard.monthlyRollup,
            baseRecommended = dashboard.bodyStatus.dailyPlan.recommendedIntake,
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎯 ${stage.titleKo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "목표 체중 ${stage.targetWeightMinKg.toInt()}~${stage.targetWeightMaxKg.toInt()}kg · 내장지방 ${stage.targetVisceralFatMax} Level",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = onQuickWeightClick) {
                        Icon(Icons.Default.MonitorWeight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("체중 입력", fontSize = 12.sp)
                    }
                    IconButton(onClick = onSyncClick) {
                        Icon(Icons.Default.Sync, contentDescription = "Health Connect 동기화")
                    }
                }
            }

            LinearProgressIndicator(
                progress = { body.stageResult.weightProgressPct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill("현재 체중", String.format("%.1fkg", body.currentWeightKg))
                MetricPill("내장지방", "${body.latestInBody.visceralFatLevel} Lv")
                MetricPill("골격근량", String.format("%.1fkg", body.latestInBody.skeletalMuscleKg))
                MetricPill("BMR(공식)", "${body.bmrKcal}kcal")
                MetricPill("BMI", String.format("%.1f", body.bmi))
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RollupSummaryRow(
    weekly: PeriodRollup,
    monthly: PeriodRollup,
    baseRecommended: Int,
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
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StatusYellowBorder, RoundedCornerShape(10.dp))
        ) {
            Text(
                text = "⚡ 주간 만회 모드 작동 중: 주간 ${weekly.cumulativeBalanceKcal}kcal 초과분을 남은 ${weekly.remainingDaysInPeriod}일 동안 만회하기 위해 오늘 권장 섭취 ${weekly.adjustedRecommendedIntakeToday}kcal(BMR 하한 보호) + 운동 목표 ${weekly.adjustedExerciseTargetToday}kcal(기본 ${baseExerciseTarget}+만회 ${weekly.adjustedExerciseTargetToday - baseExerciseTarget})로 조정되었습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = StatusYellowText,
                modifier = Modifier.padding(10.dp)
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
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, border, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 12.sp, color = fg, fontWeight = FontWeight.SemiBold)
            Text(
                text = String.format("%+d kcal", cumulative),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = fg
            )
            Text(
                text = if (recoveryPerDay > 0) {
                    "만회 필요: 일 -${recoveryPerDay}kcal (${remainingDays}일 남음)"
                } else {
                    "목표 페이스 순항 중 (${remainingDays}일 남음)"
                },
                fontSize = 11.sp,
                color = fg
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
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
                }
                Text(
                    text = "${yearMonth.year}년 ${yearMonth.monthValue}월 목표 캘린더",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendChip("🟢 달성(≥0)", StatusGreenBg, StatusGreenText)
                LegendChip("🟡 허용(-150~0)", StatusYellowBg, StatusYellowText)
                LegendChip("🔴 초과/미기록", StatusRedBg, StatusRedText)
                LegendChip("⚪ 미래", StatusGrayBg, StatusGrayText)
            }

            // Weekday header (Mon..Sun)
            val weekDays = listOf("월", "화", "수", "목", "금", "토", "일")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
            }

            val firstDayOfMonth = yearMonth.atDay(1)
            val leadingEmpty = firstDayOfMonth.dayOfWeek.value - 1 // Monday = 0 .. Sunday = 6
            val daysInMonth = yearMonth.lengthOfMonth()
            val totalCells = ((leadingEmpty + daysInMonth + 6) / 7) * 7

            for (row in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            Spacer(modifier = Modifier.weight(1f).height(52.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendChip(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

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

    val subLabel = when {
        evaluation == null || color == DayStatusColor.GRAY -> ""
        evaluation.isMissingMealFailure -> "미기록"
        else -> String.format("%+d", evaluation.netBalanceKcal)
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(3.dp)
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
                    fontSize = 12.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = fg
                )
                if (hasWeightDot) {
                    Spacer(Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📅 $date 일일 칼로리 수지",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (evaluation.isMissingMealFailure) {
                            "🔴 식사 미기록 (실패 판정 — 21:00 알람 대상)"
                        } else {
                            String.format(
                                "일일 수지: %+d kcal (목표 적자 -%dkcal 기준)",
                                evaluation.netBalanceKcal,
                                plan.deficitTarget
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (evaluation.statusColor == DayStatusColor.RED) StatusRedText else StatusGreenText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(onClick = onAddMealClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("식사/술 기록", fontSize = 12.sp)
                }
            }

            // Equation breakdown
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricPill("권장 섭취", "${plan.recommendedIntake}kcal")
                    MetricPill("실제 섭취(식+술)", "${evaluation.totalIntakeKcal}kcal")
                    MetricPill("운동 목표", "${plan.exerciseTarget}kcal")
                    MetricPill("실제 운동", "+${evaluation.exerciseKcal}kcal")
                    MetricPill("오늘 ±", String.format("%+dkcal", evaluation.netBalanceKcal))
                }
            }

            if (meals.isEmpty() && drinks.isEmpty() && exercises.isEmpty()) {
                Text(
                    text = "이 날짜에 등록된 식사·술·운동 기록이 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            } else {
                if (meals.isNotEmpty()) {
                    Text("🍽 식사 & 안주 (${meals.sumOf { it.totalKcal }}kcal)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    meals.forEach { m ->
                        val typeLabel = runCatching { MealType.valueOf(m.mealType).labelKo }.getOrDefault(m.mealType)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "[$typeLabel] ${m.totalKcal}kcal (단백질 ${m.proteinGrams.toInt()}g) ${m.notes}",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDeleteMeal(m) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (drinks.isNotEmpty()) {
                    HorizontalDivider()
                    Text("🍺 음주 기록 (${drinks.sumOf { it.kcal }}kcal)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    drinks.forEach { d ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocalBar, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusRedBorder)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${d.displayLabel} (${d.volumeMl}ml, ${d.abvPct}%) — ${d.kcal}kcal",
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { onDeleteDrink(d) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (exercises.isNotEmpty()) {
                    HorizontalDivider()
                    Text("🏃 운동 소모 (+${exercises.sumOf { it.activeKcal }}kcal)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    exercises.forEach { ex ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusGreenBorder)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "[${ex.source}] ${ex.title} (${ex.durationMinutes}분) — +${ex.activeKcal}kcal",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 엄격 코치 판정: ${verdict.headlineKo}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = fg,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onOpenCoachTab) {
                    Text("상세 보기", fontSize = 11.sp)
                }
            }
            verdict.bodyLinesKo.take(2).forEach { line ->
                Text(text = "• $line", fontSize = 12.sp, color = fg)
            }
            if (verdict.actionsTomorrowKo.isNotEmpty()) {
                Text(
                    text = "내일 필수 행동: ${verdict.actionsTomorrowKo.first()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg
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
        title = { Text("⚖ $date 체중 수동 입력") },
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
