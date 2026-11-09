package com.minsoo.slimtrack.ui.advice

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.data.db.AdviceEntity
import com.minsoo.slimtrack.domain.AdviceVerdict
import com.minsoo.slimtrack.domain.ViolationSeverity
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenBorder
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import com.minsoo.slimtrack.ui.theme.StatusRedBg
import com.minsoo.slimtrack.ui.theme.StatusRedBorder
import com.minsoo.slimtrack.ui.theme.StatusRedText
import com.minsoo.slimtrack.ui.theme.StatusYellowBg
import com.minsoo.slimtrack.ui.theme.StatusYellowBorder
import com.minsoo.slimtrack.ui.theme.StatusYellowText

@Composable
fun CoachAdviceScreen(
    liveVerdict: AdviceVerdict,
    adviceHistory: List<AdviceEntity>,
    isGeneratingAiAdvice: Boolean,
    onGenerateAiAdviceNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBad = liveVerdict.grade in listOf("C", "D", "F")
    val headerBg = if (isBad) StatusRedBg else StatusGreenBg
    val headerBorder = if (isBad) StatusRedBorder else StatusGreenBorder
    val headerFg = if (isBad) StatusRedText else StatusGreenText

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "📋 엄격 코치 판정 & 주간 어드바이스",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Alarm Schedule Banner
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "알람 정책: ① 매주 일요일 아침 09:00 주간 어드바이스 1회 발송 ② 당일 식사 미기록 시 실패(빨강) 처리 및 저녁 21:00 경고 알람 발송",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Live Verdict Grade Card
        Surface(
            color = headerBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, headerBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "현재 진행 등급: ${liveVerdict.grade} (강도: ${liveVerdict.strictness})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = headerFg
                        )
                        Text(
                            text = liveVerdict.headlineKo,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = headerFg
                        )
                    }
                    Button(
                        onClick = onGenerateAiAdviceNow,
                        enabled = !isGeneratingAiAdvice
                    ) {
                        if (isGeneratingAiAdvice) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("코치 리포트 생성", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = headerBorder.copy(alpha = 0.3f))

                liveVerdict.bodyLinesKo.forEach { line ->
                    Text(text = "• $line", fontSize = 13.sp, color = headerFg)
                }

                Text(
                    text = "🔥 내일 필수 행동 지침 (3대 수칙)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = headerFg
                )
                liveVerdict.actionsTomorrowKo.forEachIndexed { idx, act ->
                    Text(
                        text = "${idx + 1}. $act",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = headerFg
                    )
                }
            }
        }

        // Detailed Violation Checklist
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🔍 세부 규칙 판정 내역 (${liveVerdict.violations.size}건 지적)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (liveVerdict.violations.isEmpty()) {
                    Text(
                        text = "✅ 지적된 위반 항목이 없습니다. 현재 페이스를 그대로 유지하십시오.",
                        fontSize = 13.sp,
                        color = StatusGreenText,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    liveVerdict.violations.forEach { v ->
                        val bg = when (v.severity) {
                            ViolationSeverity.HIGH -> StatusRedBg
                            ViolationSeverity.MEDIUM -> StatusYellowBg
                            ViolationSeverity.LOW -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val border = when (v.severity) {
                            ViolationSeverity.HIGH -> StatusRedBorder
                            ViolationSeverity.MEDIUM -> StatusYellowBorder
                            ViolationSeverity.LOW -> Color.Gray
                        }
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${v.severity.badgeKo} · ${v.titleKo}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(text = v.detailKo, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Saved / Gemini Advice History
        if (adviceHistory.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🗂 저장된 코치 어드바이스 이력",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    adviceHistory.take(10).forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "[${item.dateIso} · ${item.generator} · 등급 ${item.grade}] ${item.headline}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(text = item.bodyText, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
