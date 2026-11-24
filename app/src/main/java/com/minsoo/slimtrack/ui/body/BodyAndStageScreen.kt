package com.minsoo.slimtrack.ui.body

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.R
import com.minsoo.slimtrack.data.ai.InBodyOcrResponse
import com.minsoo.slimtrack.data.db.InBodyEntity
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.data.health.HealthWeightSyncDiag
import com.minsoo.slimtrack.data.prefs.UserProfile
import com.minsoo.slimtrack.data.repo.CurrentBodyStatus
import com.minsoo.slimtrack.domain.GoalMetricType
import com.minsoo.slimtrack.domain.GoalStage
import com.minsoo.slimtrack.domain.GoalTrajectoryCalculator
import com.minsoo.slimtrack.ui.home.GoalMetricComparisonAccordion
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenBorder
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import com.minsoo.slimtrack.ui.theme.StatusRedBg
import com.minsoo.slimtrack.ui.theme.StatusRedBorder
import com.minsoo.slimtrack.ui.theme.StatusRedText
import com.minsoo.slimtrack.ui.theme.TealPrimary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BodyAndStageScreen(
    today: LocalDate,
    effectiveStartDate: LocalDate = LocalDate.now(),
    bodyStatus: CurrentBodyStatus,
    weightLogs: List<WeightLogEntity>,
    inBodyHistory: List<InBodyEntity>,
    isAnalyzingInBody: Boolean,
    syncStatusMessage: String?,
    weightSyncDiag: HealthWeightSyncDiag? = null,
    onManualSyncHealthConnectWeight: () -> Unit,
    onSaveQuickTodayWeight: (Float) -> Unit = {},
    onPickInBodyScreenshot: () -> Unit,
    onSaveInBodyManualOrOcr: (InBodyOcrResponse) -> Unit = {},
    initialShowWeeklyWeightHistory: Boolean = false,
    modifier: Modifier = Modifier
) {
    val latestInBody = bodyStatus.latestInBody
    var showWeeklyWeightHistory by remember { mutableStateOf(initialShowWeeklyWeightHistory) }
    var showTimestampDiag by remember { mutableStateOf(true) }

    // Keep all weight logs sorted chronologically so the last 7 days are always available!
    val filteredWeightLogs = remember(weightLogs) {
        weightLogs.sortedBy { it.measuredAtIso }
    }

    val latestSyncedFatPct = remember(filteredWeightLogs, latestInBody) {
        filteredWeightLogs.lastOrNull { it.bodyFatPct != null }?.bodyFatPct ?: latestInBody.bodyFatPct
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.sticker_check_done),
                contentDescription = "확인완료 스티커",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📊 Health 체중·체지방 그래프 & InBody · 3단계 목표",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Health Connect 동기화 그래프와 주간 InBody 스크린샷을 확인합니다.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!syncStatusMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = syncStatusMessage,
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        // Section 1: Health Connect Weight & Body Fat Trend Graphs (Manual inputs removed as requested!)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "1️⃣ Health Connect 체중 & 체지방 그래프",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "유효 시작일($effectiveStartDate) 이후 Health 동기화 데이터 기준",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable(onClick = onManualSyncHealthConnectWeight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
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

                // Summary tiles for Health Connect Weight (Clickable to toggle 7-day weight history!) & Body Fat
                val activeSpec = bodyStatus.stageResult.activeStageSpec
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthSummaryBox(
                        label = "현재 체중 (${bodyStatus.currentWeightSource})",
                        value = String.format("%.1f kg", bodyStatus.currentWeightKg),
                        subText = if (showWeeklyWeightHistory) {
                            "👆 지난 7일 체중 기록 접기 ▲"
                        } else {
                            "👆 터치: 지난 7일 체중 기록 보기 ▼"
                        },
                        color = TealPrimary,
                        isInteractive = true,
                        isExpanded = showWeeklyWeightHistory,
                        onClick = { showWeeklyWeightHistory = !showWeeklyWeightHistory },
                        modifier = Modifier.weight(1f)
                    )
                    HealthSummaryBox(
                        label = "체지방률 (Health / InBody)",
                        value = String.format("%.1f %%", latestSyncedFatPct),
                        subText = "체지방량 ${latestInBody.bodyFatMassKg}kg · 내장지방 ${latestInBody.visceralFatLevel}Lv",
                        color = Color(0xFFE11D48),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expandable 7-Day Weight History Panel when the user clicks on "현재 체중" (몸무게)!
                AnimatedVisibility(visible = showWeeklyWeightHistory) {
                    WeeklyWeightHistoryCard(
                        today = today,
                        weightLogs = filteredWeightLogs,
                        inBodyHistory = inBodyHistory,
                        latestInBody = latestInBody,
                        onSaveQuickTodayWeight = onSaveQuickTodayWeight
                    )
                }

                // Request & Response Timestamp Diagnostic Inspector for Health Connect Weight Sync
                HealthWeightTimestampDiagCard(
                    diag = weightSyncDiag,
                    expanded = showTimestampDiag,
                    onToggleExpand = { showTimestampDiag = !showTimestampDiag }
                )

                // Graph 1: Weight (kg) Trend Chart (also clickable to toggle 7-day weight history)
                Box(modifier = Modifier.clickable { showWeeklyWeightHistory = !showWeeklyWeightHistory }) {
                    WeightTrendChart(
                        weightLogs = filteredWeightLogs,
                        latestInBody = latestInBody,
                        targetWeightKg = activeSpec.targetWeightMidKg.toFloat()
                    )
                }

                HorizontalDivider()

                // Graph 2: Body Fat (%) Trend Chart
                BodyFatTrendChart(
                    weightLogs = filteredWeightLogs,
                    inBodyHistory = inBodyHistory,
                    latestInBody = latestInBody,
                    effectiveStartDate = effectiveStartDate
                )
            }
        }

        // Section 2: InBody Weekly Record (Screenshot OCR + Auto Formula BMR)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "2️⃣ InBody 정보 (주 1회 스크린샷 · 최신값 자동 반영)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "최신 측정: ${latestInBody.measuredAtIso} (${latestInBody.device})",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Button(
                    onClick = onPickInBodyScreenshot,
                    enabled = !isAnalyzingInBody,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "InBody 스크린샷 사진 선택 및 자동 인식",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (isAnalyzingInBody) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("InBody 스크린샷 수치 추출 중...", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }

                // Clean summary grid of latest InBody metrics (clickable for Goal vs Daily Achievement comparison!)
                var expandedBodyMetric by remember { mutableStateOf<GoalMetricType?>(null) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InBodyMetricChip(
                        label = "골격근량▾",
                        value = "${latestInBody.skeletalMuscleKg}kg",
                        isSelected = expandedBodyMetric == GoalMetricType.SKELETAL_MUSCLE,
                        onClick = {
                            expandedBodyMetric = if (expandedBodyMetric == GoalMetricType.SKELETAL_MUSCLE) null else GoalMetricType.SKELETAL_MUSCLE
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InBodyMetricChip(
                        label = "체지방량",
                        value = "${latestInBody.bodyFatMassKg}kg",
                        modifier = Modifier.weight(1f)
                    )
                    InBodyMetricChip(
                        label = "내장지방▾",
                        value = "${latestInBody.visceralFatLevel}Lv",
                        isSelected = expandedBodyMetric == GoalMetricType.VISCERAL_FAT,
                        onClick = {
                            expandedBodyMetric = if (expandedBodyMetric == GoalMetricType.VISCERAL_FAT) null else GoalMetricType.VISCERAL_FAT
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InBodyMetricChip(
                        label = "세포외수분",
                        value = "${latestInBody.ecwRatio}",
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedVisibility(visible = expandedBodyMetric != null) {
                    val m = expandedBodyMetric ?: GoalMetricType.SKELETAL_MUSCLE
                    val series = remember(m, today, effectiveStartDate, bodyStatus, filteredWeightLogs, inBodyHistory) {
                        GoalTrajectoryCalculator.buildSeries(
                            metric = m,
                            today = today,
                            profile = UserProfile(goalStartDate = effectiveStartDate),
                            stageResult = bodyStatus.stageResult,
                            currentWeightKg = bodyStatus.currentWeightKg,
                            currentBmrKcal = bodyStatus.bmrKcal,
                            currentBmi = bodyStatus.bmi,
                            latestInBody = latestInBody,
                            weightLogs = filteredWeightLogs,
                            inBodyHistory = inBodyHistory
                        )
                    }
                    GoalMetricComparisonAccordion(
                        series = series,
                        onSelectMetric = { expandedBodyMetric = it },
                        onClose = { expandedBodyMetric = null }
                    )
                }

                Surface(
                    color = StatusGreenBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StatusGreenBorder, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "InBody 계산식 기초대사량 (370 + 21.6 × 제지방량)",
                                fontSize = 10.5.sp,
                                color = StatusGreenText,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${bodyStatus.bmrKcal} kcal / 일",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreenText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Section 3: 3-Stage Goal Roadmap (Uses customized stage specs from UserProfile!)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🎯 3단계 체중감량 및 내장지방 목표 로드맵",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                bodyStatus.stageResult.allStageSpecs.forEach { spec ->
                    val isCurrent = bodyStatus.stageResult.activeStage == spec.stage
                    val isDone = when (spec.stage) {
                        GoalStage.STAGE_1_SHORT -> bodyStatus.stageResult.stage1Completed
                        GoalStage.STAGE_2_MID -> bodyStatus.stageResult.stage2Completed
                        GoalStage.STAGE_3_LONG -> bodyStatus.stageResult.stage3Completed
                    }
                    StageRoadmapItem(
                        spec = spec,
                        today = today,
                        effectiveStartDate = effectiveStartDate,
                        remainingKgToActiveStage = bodyStatus.stageResult.remainingKgToActiveStage,
                        isCurrent = isCurrent,
                        isDone = isDone
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthSummaryBox(
    label: String,
    value: String,
    subText: String,
    color: Color,
    isInteractive: Boolean = false,
    isExpanded: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isInteractive && isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .then(
                if (isInteractive) {
                    Modifier.border(
                        width = if (isExpanded) 2.dp else 1.5.dp,
                        color = color,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF475569),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    maxLines = 1,
                    softWrap = false
                )
                if (isInteractive) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "접기" else "지난 7일 기록 펼치기",
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = subText,
                fontSize = 9.5.sp,
                fontWeight = if (isInteractive) FontWeight.Bold else FontWeight.Normal,
                color = if (isInteractive) color else Color(0xFF64748B),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class DailyWeightRowItem(
    val date: LocalDate,
    val dayOfWeekKo: String,
    val weightKg: Float?,
    val bodyFatPct: Float?,
    val timeText: String,
    val sourceLabel: String,
    val deltaKg: Float?
)

@Composable
private fun WeeklyWeightHistoryCard(
    today: LocalDate,
    weightLogs: List<WeightLogEntity>,
    inBodyHistory: List<InBodyEntity>,
    latestInBody: InBodyEntity,
    onSaveQuickTodayWeight: (Float) -> Unit = {}
) {
    var quickWeightText by remember { mutableStateOf("") }
    val rows = remember(today, weightLogs, inBodyHistory, latestInBody) {
        val start7 = today.minusDays(6)
        val logsByDate = weightLogs.groupBy { it.dateIso }
        val inBodyByDate = (inBodyHistory + latestInBody).groupBy { it.dateIso }

        // Build chronological list over the 7-day window [today - 6 .. today]
        val rawDays = (0L..6L).map { offset ->
            val d = start7.plusDays(offset)
            val iso = d.toString()
            val dayLog = logsByDate[iso]?.maxByOrNull { it.measuredAtIso }
            val dayInBody = inBodyByDate[iso]?.maxByOrNull { it.measuredAtIso }
            Triple(d, dayLog, dayInBody)
        }

        var prevRecordedWeight: Float? = weightLogs
            .filter { it.dateIso < start7.toString() }
            .maxByOrNull { it.measuredAtIso }
            ?.weightKg

        val chronological = rawDays.map { (d, dayLog, dayInBody) ->
            val dowKo = when (d.dayOfWeek) {
                DayOfWeek.MONDAY -> "월"
                DayOfWeek.TUESDAY -> "화"
                DayOfWeek.WEDNESDAY -> "수"
                DayOfWeek.THURSDAY -> "목"
                DayOfWeek.FRIDAY -> "금"
                DayOfWeek.SATURDAY -> "토"
                DayOfWeek.SUNDAY -> "일"
            }
            val w = dayLog?.weightKg ?: dayInBody?.weightKg
            val fat = dayLog?.bodyFatPct ?: dayInBody?.bodyFatPct
            val timeStr = when {
                dayLog != null && dayLog.measuredAtIso.length >= 16 -> dayLog.measuredAtIso.substring(11, 16)
                dayInBody != null && dayInBody.measuredAtIso.length >= 16 -> dayInBody.measuredAtIso.substring(11, 16)
                else -> ""
            }
            val src = when {
                dayLog != null -> if (dayLog.source == "HC") "Health" else "수동"
                dayInBody != null -> "InBody"
                else -> "-"
            }
            val delta = if (w != null && prevRecordedWeight != null) {
                w - prevRecordedWeight!!
            } else null
            if (w != null) {
                prevRecordedWeight = w
            }
            DailyWeightRowItem(
                date = d,
                dayOfWeekKo = dowKo,
                weightKg = w,
                bodyFatPct = fat,
                timeText = timeStr,
                sourceLabel = src,
                deltaKg = delta
            )
        }
        // Display newest day first (Today -> 6 days ago)
        chronological.reversed()
    }

    val recordedWeights = rows.mapNotNull { it.weightKg }
    val oldestInWeek = recordedWeights.lastOrNull()
    val newestInWeek = recordedWeights.firstOrNull()
    val weekDelta = if (oldestInWeek != null && newestInWeek != null) newestInWeek - oldestInWeek else 0f

    Surface(
        color = Color(0xFFF0FDFA),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, TealPrimary, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 지난 일주일(7일간) 몸무게 기록 (${today.minusDays(6).format(DateTimeFormatter.ofPattern("MM/dd"))}~${today.format(DateTimeFormatter.ofPattern("MM/dd"))})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (recordedWeights.size >= 2) {
                    val isLoss = weekDelta <= 0f
                    val badgeBg = if (isLoss) StatusGreenBg else StatusRedBg
                    val badgeFg = if (isLoss) StatusGreenText else StatusRedText
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = String.format("7일 %+.1fkg", weekDelta),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeFg,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFCCFBF1))

            rows.forEach { item ->
                val isToday = item.date == today
                val dateLabel = "${item.date.format(DateTimeFormatter.ofPattern("MM/dd"))}(${item.dayOfWeekKo})" +
                    if (isToday) " 오늘" else ""

                Surface(
                    color = if (isToday) Color.White else Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isToday) 1.dp else 0.5.dp,
                            color = if (isToday) TealPrimary else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(7.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Date + Time + Source
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1.15f)
                        ) {
                            Text(
                                text = dateLabel,
                                fontSize = 11.5.sp,
                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )
                            if (item.timeText.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${item.timeText}·${item.sourceLabel}",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right: Weight + BodyFat + Delta badge
                        if (item.weightKg != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (item.bodyFatPct != null && item.bodyFatPct > 0f) {
                                    Text(
                                        text = String.format("체지방 %.1f%%", item.bodyFatPct),
                                        fontSize = 10.sp,
                                        color = Color(0xFFE11D48),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = String.format("%.1f kg", item.weightKg),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TealPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                val d = item.deltaKg
                                if (d != null) {
                                    val deltaColor = when {
                                        d < -0.04f -> StatusGreenText
                                        d > 0.04f -> StatusRedText
                                        else -> Color(0xFF64748B)
                                    }
                                    val deltaText = when {
                                        d < -0.04f -> String.format("▼%.1f", d)
                                        d > 0.04f -> String.format("▲+%.1f", d)
                                        else -> "0.0"
                                    }
                                    Text(
                                        text = deltaText,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = deltaColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "미측정 (-)",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFCCFBF1))

            // Quick Today Weight Input Bar for when Fitbit -> Health Connect background sync is delayed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quickWeightText,
                    onValueChange = { quickWeightText = it },
                    label = {
                        Text(
                            text = "⚡ 오늘($today) 체중 즉시 반영 (예: 97.5)",
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val wVal = quickWeightText.trim().toFloatOrNull()
                        if (wVal != null && wVal in 30f..250f) {
                            onSaveQuickTodayWeight(wVal)
                            quickWeightText = ""
                        }
                    },
                    enabled = quickWeightText.trim().toFloatOrNull()?.let { it in 30f..250f } == true,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "오늘 반영",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthWeightTimestampDiagCard(
    diag: HealthWeightSyncDiag?,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔍 Health Connect 체중 송수신 타임스탬프 진단 로그",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 [원인 안내] Fitbit(Google Health) 앱에서 측정된 체중(예: 09:02 97.5kg)은 Fitbit 클라우드에 먼저 기록되며, Android Health Connect DB로는 백그라운드 주기에 따라 지연 전달됩니다. Fitbit 앱 첫 화면('오늘' 탭)을 아래로 당겨 새로고침한 뒤 [Health 동기화]를 누르거나, 위 '오늘 체중 즉시 반영'에 입력하시면 즉시 반영됩니다.",
                            fontSize = 10.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(7.dp)
                        )
                    }

                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.fitbit.FitbitMobile")
                                ?: context.packageManager.getLaunchIntentForPackage("com.google.android.apps.fitness")
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Google Health / Fitbit 앱 열기 (새로고침)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (diag == null) {
                        Text(
                            text = "아직 동기화 로그가 없습니다. 우측 상단 [Health 동기화]를 눌러주세요.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Text(
                            text = "📤 [송신 Request]\n• 요청 시각(Local): ${diag.requestLocalIso} (${diag.systemZoneId})\n• 요청 시각(UTC): ${diag.requestUtcIso}\n• 조회 시작(Start): ${diag.filterStartLocalIso} (UTC ${diag.filterStartUtcIso})\n• 필터 모드: ${diag.filterMode}",
                            fontSize = 10.sp,
                            color = Color(0xFF334155)
                        )
                        if (diag.errorText != null) {
                            Text(
                                text = "⚠️ 오류: ${diag.errorText}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRedText
                            )
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Text(
                            text = "📥 [수신 Response — Health Connect DB 내 최근 체중 ${diag.totalRecordsReturned}건]",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        diag.records.take(5).forEachIndexed { idx, rec ->
                            val delayText = if (rec.syncDelayMinutes > 0) {
                                "+${rec.syncDelayMinutes}분 지연 기록"
                            } else {
                                "즉시 기록(${rec.syncDelayMinutes}분)"
                            }
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = "#${idx + 1} ${rec.weightKg}kg · 출처: ${rec.dataOriginPackage}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TealPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "• 측정(w.time): ${rec.measuredLocalIso} (UTC ${rec.measuredUtcIso}, offset=${rec.zoneOffsetStr})",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "• HC저장(lastModified): ${rec.hcModifiedLocalIso} ($delayText)",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rec.syncDelayMinutes >= 10) Color(0xFFB45309) else StatusGreenText,
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
    }
}

@Composable
private fun InBodyMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .then(
                if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                else Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
        }
    }
}

@Composable
private fun WeightTrendChart(
    weightLogs: List<WeightLogEntity>,
    latestInBody: InBodyEntity,
    targetWeightKg: Float
) {
    val points = remember(weightLogs, latestInBody) {
        val list = weightLogs
            .map { it.dateIso to it.weightKg }
            .toMutableList()
        if (list.isEmpty()) {
            list += latestInBody.dateIso to latestInBody.weightKg
        }
        list.takeLast(14)
    }

    val firstVal = points.first().second
    val lastVal = points.last().second

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📈 체중(kg) 추이 (실선: Health 체중 / 점선: 목표 ${targetWeightKg}kg)",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = String.format("%.1f → %.1fkg", firstVal, lastVal),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
        Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    val minW = (minOf(points.minOf { it.second }, targetWeightKg) - 1.5f)
                    val maxW = (maxOf(points.maxOf { it.second }, targetWeightKg) + 1.5f)
                    val span = (maxW - minW).coerceAtLeast(2f)

                    fun yFor(w: Float): Float {
                        val ratio = (w - minW) / span
                        return size.height * (1f - ratio)
                    }

                    val targetY = yFor(targetWeightKg)
                    drawLine(
                        color = StatusGreenBorder,
                        start = Offset(0f, targetY),
                        end = Offset(size.width, targetY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    if (points.size == 1) {
                        val y = yFor(points.first().second)
                        drawCircle(color = TealPrimary, radius = 8f, center = Offset(size.width / 2f, y))
                    } else {
                        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                        for (i in 0 until points.size - 1) {
                            val x1 = i * stepX
                            val y1 = yFor(points[i].second)
                            val x2 = (i + 1) * stepX
                            val y2 = yFor(points[i + 1].second)
                            drawLine(
                                color = TealPrimary,
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 5f
                            )
                        }
                        points.forEachIndexed { idx, p ->
                            drawCircle(
                                color = TealPrimary,
                                radius = 6f,
                                center = Offset(idx * stepX, yFor(p.second))
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = points.first().first, fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                    Text(text = "총 ${points.size}회 기록", fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                    Text(text = points.last().first, fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun BodyFatTrendChart(
    weightLogs: List<WeightLogEntity>,
    inBodyHistory: List<InBodyEntity>,
    latestInBody: InBodyEntity,
    effectiveStartDate: LocalDate
) {
    val fatPoints = remember(weightLogs, inBodyHistory, latestInBody, effectiveStartDate) {
        val hcFats = weightLogs
            .filter { it.bodyFatPct != null && it.bodyFatPct > 0f }
            .map { it.dateIso to it.bodyFatPct!! }
            .toMutableList()
        if (hcFats.isEmpty()) {
            val ibFats = inBodyHistory
                .filter { it.dateIso >= effectiveStartDate.toString() }
                .sortedBy { it.measuredAtIso }
                .map { it.dateIso to it.bodyFatPct }
            hcFats.addAll(ibFats)
        }
        if (hcFats.isEmpty()) {
            hcFats += latestInBody.dateIso to latestInBody.bodyFatPct
        }
        hcFats.takeLast(14)
    }

    val firstFat = fatPoints.first().second
    val lastFat = fatPoints.last().second
    val targetFatPct = 20.0f

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📉 체지방률(%) 추이 (목표 20%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = String.format("%.1f%% → %.1f%%", firstFat, lastFat),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE11D48),
                maxLines = 1,
                softWrap = false
            )
        }
        Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    val minF = (minOf(fatPoints.minOf { it.second }, targetFatPct) - 2f).coerceAtLeast(5f)
                    val maxF = (maxOf(fatPoints.maxOf { it.second }, targetFatPct) + 2f)
                    val span = (maxF - minF).coerceAtLeast(2f)

                    fun yFor(f: Float): Float {
                        val ratio = (f - minF) / span
                        return size.height * (1f - ratio)
                    }

                    val targetY = yFor(targetFatPct)
                    drawLine(
                        color = StatusGreenBorder,
                        start = Offset(0f, targetY),
                        end = Offset(size.width, targetY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    val lineColor = Color(0xFFE11D48)
                    if (fatPoints.size == 1) {
                        val y = yFor(fatPoints.first().second)
                        drawCircle(color = lineColor, radius = 8f, center = Offset(size.width / 2f, y))
                    } else {
                        val stepX = size.width / (fatPoints.size - 1).coerceAtLeast(1)
                        for (i in 0 until fatPoints.size - 1) {
                            val x1 = i * stepX
                            val y1 = yFor(fatPoints[i].second)
                            val x2 = (i + 1) * stepX
                            val y2 = yFor(fatPoints[i + 1].second)
                            drawLine(
                                color = lineColor,
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 5f
                            )
                        }
                        fatPoints.forEachIndexed { idx, p ->
                            drawCircle(
                                color = lineColor,
                                radius = 6f,
                                center = Offset(idx * stepX, yFor(p.second))
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = fatPoints.first().first, fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                    Text(text = "총 ${fatPoints.size}회 기록", fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                    Text(text = fatPoints.last().first, fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun StageRoadmapItem(
    spec: com.minsoo.slimtrack.domain.StageSpec,
    today: LocalDate,
    effectiveStartDate: LocalDate,
    remainingKgToActiveStage: Double,
    isCurrent: Boolean,
    isDone: Boolean
) {
    val stageDeadline = effectiveStartDate.plusMonths(spec.monthsFromStart.toLong().coerceAtLeast(1L))
    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, stageDeadline).toInt().coerceAtLeast(0)

    val bg = when {
        isDone -> StatusGreenBg
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val border = when {
        isDone -> StatusGreenBorder
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isCurrent || isDone) 1.5.dp else 0.dp, border, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${spec.titleKo} ${if (isCurrent) "[진행중]" else ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                if (isDone) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "달성", tint = StatusGreenBorder, modifier = Modifier.size(16.dp))
                } else if (isCurrent) {
                    // High-urgency D-Day countdown badge for the active stage!
                    Surface(
                        color = Color(0xFFDC2626),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🔥 D-${daysLeft} (${daysLeft}일 남음)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Text(
                        text = "D-${daysLeft}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (isCurrent) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF87171), RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "⏰ 마감일(${stageDeadline})까지 딱 ${daysLeft}일 남음! 목표까지 -${String.format("%.1fkg", remainingKgToActiveStage)} 필수",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB91C1C),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = "목표 체중(중간값): ${spec.targetWeightMidFormatted}kg · 목표 내장지방: ${spec.targetVisceralFatMax}Lv (마감: $stageDeadline)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            spec.strategyPointsKo.forEach { pt ->
                Text(text = "• $pt", fontSize = 11.sp, color = Color(0xFF334155))
            }
        }
    }
}
