package com.minsoo.slimtrack.ui.body

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.data.ai.InBodyOcrResponse
import com.minsoo.slimtrack.data.db.InBodyEntity
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.data.repo.CurrentBodyStatus
import com.minsoo.slimtrack.domain.GoalStage
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenBorder
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import com.minsoo.slimtrack.ui.theme.TealPrimary
import java.time.LocalDate

@Composable
fun BodyAndStageScreen(
    today: LocalDate,
    effectiveStartDate: LocalDate = LocalDate.now(),
    bodyStatus: CurrentBodyStatus,
    weightLogs: List<WeightLogEntity>,
    inBodyHistory: List<InBodyEntity>,
    isAnalyzingInBody: Boolean,
    syncStatusMessage: String?,
    onManualSyncHealthConnectWeight: () -> Unit,
    onPickInBodyScreenshot: () -> Unit,
    onSaveInBodyManualOrOcr: (InBodyOcrResponse) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val latestInBody = bodyStatus.latestInBody
    val filteredWeightLogs = remember(weightLogs, effectiveStartDate) {
        weightLogs
            .filter { it.dateIso >= effectiveStartDate.toString() }
            .sortedBy { it.measuredAtIso }
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
        Text(
            text = "📊 Health 체중·체지방 그래프 & InBody · 3단계 목표",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )

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

                // Summary tiles for Health Connect Weight & Body Fat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthSummaryBox(
                        label = "현재 체중 (${bodyStatus.currentWeightSource})",
                        value = String.format("%.1f kg", bodyStatus.currentWeightKg),
                        subText = "1단계 목표: ${bodyStatus.stageResult.activeStage.targetWeightMinKg.toInt()}~${bodyStatus.stageResult.activeStage.targetWeightMaxKg.toInt()}kg",
                        color = TealPrimary,
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

                // Graph 1: Weight (kg) Trend Chart
                WeightTrendChart(
                    weightLogs = filteredWeightLogs,
                    latestInBody = latestInBody,
                    targetWeightKg = bodyStatus.stageResult.activeStage.targetWeightMidKg.toFloat()
                )

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

                // Clean summary grid of latest InBody metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InBodyMetricChip("골격근량", "${latestInBody.skeletalMuscleKg}kg", Modifier.weight(1f))
                    InBodyMetricChip("체지방량", "${latestInBody.bodyFatMassKg}kg", Modifier.weight(1f))
                    InBodyMetricChip("내장지방", "${latestInBody.visceralFatLevel}Lv", Modifier.weight(1f))
                    InBodyMetricChip("세포외수분", "${latestInBody.ecwRatio}", Modifier.weight(1f))
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

        // Section 3: 3-Stage Goal Roadmap
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
                GoalStage.entries.forEach { stage ->
                    val isCurrent = bodyStatus.stageResult.activeStage == stage
                    val isDone = when (stage) {
                        GoalStage.STAGE_1_SHORT -> bodyStatus.stageResult.stage1Completed
                        GoalStage.STAGE_2_MID -> bodyStatus.stageResult.stage2Completed
                        GoalStage.STAGE_3_LONG -> bodyStatus.stageResult.stage3Completed
                    }
                    StageRoadmapItem(stage = stage, isCurrent = isCurrent, isDone = isDone)
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
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
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
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subText,
                fontSize = 9.5.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InBodyMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
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
                text = "📉 체지방률(%) 추이 (실선: Health 체지방 / 점선: 표준 20%)",
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
    stage: GoalStage,
    isCurrent: Boolean,
    isDone: Boolean
) {
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
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stage.titleKo} ${if (isCurrent) "[진행중]" else ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (isDone) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "달성", tint = StatusGreenBorder, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = "목표 체중: ${stage.targetWeightMinKg.toInt()}~${stage.targetWeightMaxKg.toInt()}kg · 내장지방: ${stage.targetVisceralFatMin}~${stage.targetVisceralFatMax}Lv",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
            stage.strategyPointsKo.forEach { pt ->
                Text(text = "• $pt", fontSize = 11.sp, color = Color(0xFF334155))
            }
        }
    }
}
