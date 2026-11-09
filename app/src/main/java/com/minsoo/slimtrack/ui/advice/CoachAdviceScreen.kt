package com.minsoo.slimtrack.ui.advice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "📋 엄격 코치 판정 & 주간 어드바이스",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )

        // Alarm Schedule Banner
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "알람: ① 일요일 09:00 주간 코치 1회 ② 식사 미기록 시 실패(빨강) 및 21:00 경고",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(headerBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = liveVerdict.grade,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "진행 등급 ${liveVerdict.grade} (${liveVerdict.strictness})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = headerFg,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = liveVerdict.headlineKo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = headerFg,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = onGenerateAiAdviceNow,
                        enabled = !isGeneratingAiAdvice,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isGeneratingAiAdvice) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("코치 생성", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                HorizontalDivider(color = headerBorder.copy(alpha = 0.3f))

                liveVerdict.bodyLinesKo.forEach { line ->
                    Text(text = "• $line", fontSize = 12.sp, color = headerFg)
                }

                Text(
                    text = "🔥 내일 필수 행동 지침 (3대 수칙)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = headerFg,
                    maxLines = 1,
                    softWrap = false
                )
                liveVerdict.actionsTomorrowKo.forEachIndexed { idx, act ->
                    Text(
                        text = "${idx + 1}. $act",
                        fontSize = 12.sp,
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
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🔍 세부 규칙 판정 내역 (${liveVerdict.violations.size}건 지적)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                if (liveVerdict.violations.isEmpty()) {
                    Text(
                        text = "✅ 지적된 위반 항목이 없습니다. 현재 페이스를 그대로 유지하십시오.",
                        fontSize = 12.sp,
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
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(text = v.detailKo, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }

        if (adviceHistory.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🗂 저장된 코치 어드바이스 이력",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
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
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(text = item.bodyText, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
