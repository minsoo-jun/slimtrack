package com.minsoo.slimtrack.ui.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "⚖ 매일 체중 · InBody(주 1회) · 3단계 목표",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (!syncStatusMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = syncStatusMessage,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Section 1: Daily Manual Weight & Health Connect Manual Sync Menu
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "1️⃣ 매일 체중 입력 & Google Health 동기화",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "현재 반영 체중: ${String.format("%.1fkg", bodyStatus.currentWeightKg)} (${bodyStatus.currentWeightSource}, ${bodyStatus.currentWeightDateIso})",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    FilledTonalButton(onClick = onManualSyncHealthConnectWeight) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Health Connect 동기화", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dailyWeightInput,
                        onValueChange = { dailyWeightInput = it },
                        label = { Text("오늘 체중 (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dailyFatPctInput,
                        onValueChange = { dailyFatPctInput = it },
                        label = { Text("체지방률 (%, 선택)") },
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
                        }
                    ) {
                        Text("체중 저장")
                    }
                }

                // Weight Trend Line Chart
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2️⃣ InBody 정보 (주 1회 갱신 · 항상 최신값 사용)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "최신 측정일시: ${latestInBody.measuredAtIso} (${latestInBody.device}, ${latestInBody.source}) · 기초대사량(BMR)은 항상 InBody 공식 370 + 21.6 × (체중 − 체지방량)으로 자동 계산됩니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPickInBodyScreenshot,
                        enabled = !isAnalyzingInBody,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("InBody 앱 스크린샷에서 자동 추출 (Gemini OCR)")
                    }
                }

                if (isAnalyzingInBody) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("InBody 스크린샷 수치 추출 중...", fontSize = 12.sp)
                    }
                }

                HorizontalDivider()
                Text("InBody 측정값 확인 / 수동 수정", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                OutlinedTextField(
                    value = ibMeasuredAt,
                    onValueChange = { ibMeasuredAt = it },
                    label = { Text("측정일시 (예: 26.11.07(토) 11:44 또는 2026-11-07T11:44)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ibWeight,
                        onValueChange = { ibWeight = it },
                        label = { Text("체중 (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ibMuscle,
                        onValueChange = { ibMuscle = it },
                        label = { Text("골격근량 (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ibFatMass,
                        onValueChange = { ibFatMass = it },
                        label = { Text("체지방량 (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ibFatPct,
                        onValueChange = { ibFatPct = it },
                        label = { Text("체지방률 (%)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ibVisceral,
                        onValueChange = { ibVisceral = it },
                        label = { Text("내장지방 (Level)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ibEcw,
                        onValueChange = { ibEcw = it },
                        label = { Text("세포외수분비") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Surface(
                    color = StatusGreenBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "InBody 공식 자동 계산 기초대사량 (BMR)",
                                fontSize = 11.sp,
                                color = StatusGreenText
                            )
                            Text(
                                text = "${liveCalculatedBmr} kcal / 일",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreenText
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
                            }
                        ) {
                            Text("최신 InBody 저장")
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🎯 3단계 체중감량 및 내장지방 목표 로드맵",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
            text = "최근 체중 추이 (실선: 측정 체중 / 점선: 현재 단계 목표 ${targetWeightKg}kg)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(vertical = 4.dp)
        ) {
            val minW = (minOf(points.minOf { it.second }, targetWeightKg) - 1.5f)
            val maxW = (maxOf(points.maxOf { it.second }, targetWeightKg) + 1.5f)
            val span = (maxW - minW).coerceAtLeast(2f)

            fun yFor(w: Float): Float {
                val ratio = (w - minW) / span
                return size.height * (1f - ratio)
            }

            // Target dashed line
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
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stage.titleKo} ${if (isCurrent) "[현재 단계]" else ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (isDone) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "달성", tint = StatusGreenBorder)
                }
            }
            Text(
                text = "• 목표 체중: ${stage.targetWeightMinKg.toInt()} ~ ${stage.targetWeightMaxKg.toInt()} kg | 목표 내장지방: ${stage.targetVisceralFatMin}~${stage.targetVisceralFatMax} Level",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            stage.strategyPointsKo.forEach { pt ->
                Text(text = "  - $pt", fontSize = 11.sp, color = Color(0xFF334155))
            }
        }
    }
}
