package com.minsoo.slimtrack.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.minsoo.slimtrack.domain.BmrCalculator
import com.minsoo.slimtrack.domain.GoalStage
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenBorder
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import com.minsoo.slimtrack.ui.theme.TealPrimary
import java.time.LocalDate

@Composable
fun BodyAndStageScreen(
    today: LocalDate,
    bodyStatus: CurrentBodyStatus,
    weightLogs: List<WeightLogEntity>,
    inBodyHistory: List<InBodyEntity>,
    isAnalyzingInBody: Boolean,
    syncStatusMessage: String?,
    onAddManualWeight: (LocalDate, Float, Float?) -> Unit,
    onManualSyncHealthConnectWeight: () -> Unit,
    onPickInBodyScreenshot: () -> Unit,
    onSaveInBodyManualOrOcr: (InBodyOcrResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    var dailyWeightInput by remember(bodyStatus.currentWeightKg) {
        mutableStateOf(String.format("%.1f", bodyStatus.currentWeightKg))
    }
    var dailyFatPctInput by remember { mutableStateOf("") }

    val latestInBody = bodyStatus.latestInBody
    var ibMeasuredAt by remember(latestInBody.measuredAtIso) { mutableStateOf(latestInBody.measuredAtIso) }
    var ibWeight by remember(latestInBody.weightKg) { mutableStateOf(latestInBody.weightKg.toString()) }
    var ibMuscle by remember(latestInBody.skeletalMuscleKg) { mutableStateOf(latestInBody.skeletalMuscleKg.toString()) }
    var ibFatMass by remember(latestInBody.bodyFatMassKg) { mutableStateOf(latestInBody.bodyFatMassKg.toString()) }
    var ibFatPct by remember(latestInBody.bodyFatPct) { mutableStateOf(latestInBody.bodyFatPct.toString()) }
    var ibEcw by remember(latestInBody.ecwRatio) { mutableStateOf(latestInBody.ecwRatio.toString()) }
    var ibBmi by remember(latestInBody.bmi) { mutableStateOf(latestInBody.bmi.toString()) }
    var ibVisceral by remember(latestInBody.visceralFatLevel) { mutableStateOf(latestInBody.visceralFatLevel.toString()) }

    val liveCalculatedBmr = remember(ibWeight, ibFatMass) {
        val w = ibWeight.toDoubleOrNull() ?: latestInBody.weightKg.toDouble()
        val f = ibFatMass.toDoubleOrNull() ?: latestInBody.bodyFatMassKg.toDouble()
        BmrCalculator.inbodyBmr(w, f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⚖ 매일 체중 · InBody(주 1회) · 3단계 목표",
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

        // Section 1: Daily Manual Weight & Health Connect Manual Sync Menu
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
                            text = "1️⃣ 매일 체중 입력 & Health 동기화",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "현재: ${String.format("%.1fkg", bodyStatus.currentWeightKg)} (${bodyStatus.currentWeightSource})",
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
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "HC 동기화",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dailyWeightInput,
                        onValueChange = { dailyWeightInput = it },
                        label = { Text("체중(kg)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dailyFatPctInput,
                        onValueChange = { dailyFatPctInput = it },
                        label = { Text("체지방(%)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val w = dailyWeightInput.toFloatOrNull()
                            val fat = dailyFatPctInput.toFloatOrNull()
                            if (w != null && w in 35f..250f) {
                                onAddManualWeight(today, w, fat)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("체중 저장", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                WeightTrendChart(
                    weightLogs = weightLogs,
                    latestInBody = latestInBody,
                    targetWeightKg = bodyStatus.stageResult.activeStage.targetWeightMidKg.toFloat()
                )
            }
        }

        // Section 2: InBody Weekly Record (Screenshot OCR + Manual + Auto Formula BMR)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "2️⃣ InBody 정보 (주 1회 · 항상 최신값 사용)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "최신: ${latestInBody.measuredAtIso} (${latestInBody.device}) · BMR = 370 + 21.6 × 제지방량",
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
                        text = "InBody 스크린샷 OCR 자동 추출 (Gemini 3.x)",
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

                HorizontalDivider()

                OutlinedTextField(
                    value = ibMeasuredAt,
                    onValueChange = { ibMeasuredAt = it },
                    label = { Text("측정일시 (2026-11-07T11:44)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = ibWeight,
                        onValueChange = { ibWeight = it },
                        label = { Text("체중(kg)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ibMuscle,
                        onValueChange = { ibMuscle = it },
                        label = { Text("골격근(kg)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ibFatMass,
                        onValueChange = { ibFatMass = it },
                        label = { Text("체지방(kg)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = ibFatPct,
                        onValueChange = { ibFatPct = it },
                        label = { Text("체지방률(%)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ibVisceral,
                        onValueChange = { ibVisceral = it },
                        label = { Text("내장지방(Lv)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ibEcw,
                        onValueChange = { ibEcw = it },
                        label = { Text("세포외수분", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
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
                                text = "InBody 공식 자동 계산 BMR",
                                fontSize = 10.5.sp,
                                color = StatusGreenText,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "${liveCalculatedBmr} kcal / 일",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreenText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Button(
                            onClick = {
                                val w = ibWeight.toFloatOrNull() ?: 98.9f
                                val smm = ibMuscle.toFloatOrNull() ?: 38.5f
                                val fm = ibFatMass.toFloatOrNull() ?: 31.1f
                                val fp = ibFatPct.toFloatOrNull() ?: 31.4f
                                val ecw = ibEcw.toFloatOrNull() ?: 0.374f
                                val bmi = ibBmi.toFloatOrNull() ?: 31.9f
                                val vf = ibVisceral.toIntOrNull() ?: 12
                                onSaveInBodyManualOrOcr(
                                    InBodyOcrResponse(
                                        measuredAtRaw = ibMeasuredAt,
                                        device = "인바디570",
                                        weightKg = w,
                                        skeletalMuscleKg = smm,
                                        bodyFatMassKg = fm,
                                        bodyFatPct = fp,
                                        ecwRatio = ecw,
                                        bmi = bmi,
                                        visceralFatLevel = vf,
                                        bmrReported = null
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("InBody 저장", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
private fun WeightTrendChart(
    weightLogs: List<WeightLogEntity>,
    latestInBody: InBodyEntity,
    targetWeightKg: Float
) {
    val points = remember(weightLogs, latestInBody) {
        val list = weightLogs
            .sortedBy { it.measuredAtIso }
            .map { it.dateIso to it.weightKg }
            .toMutableList()
        if (list.isEmpty()) {
            list += latestInBody.dateIso to latestInBody.weightKg
        }
        list.takeLast(14)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "체중 추이 (실선: 측정 체중 / 점선: 단계 목표 ${targetWeightKg}kg)",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(vertical = 4.dp)
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
