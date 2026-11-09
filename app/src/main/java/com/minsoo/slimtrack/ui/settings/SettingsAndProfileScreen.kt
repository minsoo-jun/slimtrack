package com.minsoo.slimtrack.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.data.health.HealthConnectStatus
import com.minsoo.slimtrack.data.prefs.AppSettings
import com.minsoo.slimtrack.data.prefs.Gemini3Catalog
import com.minsoo.slimtrack.data.prefs.GeminiModelOption
import com.minsoo.slimtrack.data.prefs.Sex
import com.minsoo.slimtrack.data.prefs.UserProfile
import com.minsoo.slimtrack.domain.AdviceStrictness
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenBorder
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import java.time.LocalDate

@Composable
fun SettingsAndProfileScreen(
    today: LocalDate,
    profile: UserProfile,
    settings: AppSettings,
    currentApiKey: String,
    availableGemini3Models: List<GeminiModelOption>,
    isFetchingModels: Boolean,
    modelFetchMessage: String?,
    healthConnectStatus: HealthConnectStatus,
    onSaveProfile: (UserProfile) -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onFetchAvailableGemini3Models: (String) -> Unit,
    onRequestHealthPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSex by remember(profile.sex) { mutableStateOf(profile.sex) }
    var heightText by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var birthDateText by remember(profile.birthDate) { mutableStateOf(profile.birthDate.toString()) }
    var activityFactorText by remember(profile.activityFactor) { mutableStateOf(profile.activityFactor.toString()) }
    var muscleBaselineText by remember(profile.muscleBaselineKg) { mutableStateOf(profile.muscleBaselineKg.toString()) }

    var apiKeyText by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var selectedModelId by remember(settings.geminiModel) {
        mutableStateOf(Gemini3Catalog.ensureGemini3Model(settings.geminiModel))
    }
    var toleranceText by remember(settings.toleranceKcal) { mutableStateOf(settings.toleranceKcal.toString()) }
    var strictness by remember(settings.strictness) { mutableStateOf(settings.strictness) }
    var sundayAlarm by remember(settings.sundayMorningAlarmEnabled) { mutableStateOf(settings.sundayMorningAlarmEnabled) }
    var missingMealAlarm by remember(settings.missingMeal21AlarmEnabled) { mutableStateOf(settings.missingMeal21AlarmEnabled) }

    var savedBanner by remember { mutableStateOf<String?>(null) }

    val previewProfile = remember(selectedSex, heightText, birthDateText, activityFactorText, muscleBaselineText) {
        val h = heightText.toDoubleOrNull() ?: 176.0
        val b = runCatching { LocalDate.parse(birthDateText.trim()) }.getOrDefault(profile.birthDate)
        val act = activityFactorText.toDoubleOrNull() ?: 1.2
        val smm = muscleBaselineText.toDoubleOrNull() ?: 38.5
        profile.copy(
            sex = selectedSex,
            heightCm = h,
            birthDate = b,
            activityFactor = act,
            muscleBaselineKg = smm
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⚙ 사용자 프로필 & Gemini 3.x 설정",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )

        if (!savedBanner.isNullOrBlank()) {
            Surface(
                color = StatusGreenBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StatusGreenBorder, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = savedBanner!!,
                    color = StatusGreenText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        // Section 1: Gemini 3.x Model Selector & API Key (Placed at top for quick access!)
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
                            text = "🤖 Gemini 3.x 버전 선택 & API 키",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "현재 선택: $selectedModelId",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { onFetchAvailableGemini3Models(apiKeyText) },
                        enabled = !isFetchingModels,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isFetchingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "3.x 모델 탐색",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("Google AI Studio Gemini API Key (암호화 저장)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (!modelFetchMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = modelFetchMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = "사용할 Gemini 3.x 모델 선택 (터치 시 즉시 반영)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false
                )

                // Selectable Gemini 3.x model cards
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    availableGemini3Models.forEach { option ->
                        val isSelected = selectedModelId == option.id
                        Gemini3ModelRow(
                            option = option,
                            selected = isSelected,
                            onClick = {
                                selectedModelId = option.id
                                onSaveSettings(settings.copy(geminiModel = option.id))
                                savedBanner = "✅ Gemini 3.x 모델이 [${option.id}]로 변경되었습니다."
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = selectedModelId,
                    onValueChange = { selectedModelId = it },
                    label = { Text("선택된 Gemini 3.x 모델 ID (직접 입력 가능)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val validModel = Gemini3Catalog.ensureGemini3Model(selectedModelId)
                        selectedModelId = validModel
                        onSaveApiKey(apiKeyText)
                        onSaveSettings(settings.copy(geminiModel = validModel))
                        savedBanner = "✅ API 키 및 모델($validModel) 저장 완료"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "API 키 & Gemini 3.x 모델 저장",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Section 2: User Profile (Sex, Height, BirthDate -> Age & Zone 2 HR)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "👤 사용자 프로필 (성별 · 키 · 생년월일/나이 계산)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Sex.entries.forEach { sex ->
                        val selected = selectedSex == sex
                        val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val fg = if (selected) Color.White else Color(0xFF334155)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { selectedSex = sex }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sex.labelKo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = fg,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("키 (cm)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = birthDateText,
                        onValueChange = { birthDateText = it },
                        label = { Text("생년월일 (YYYY-MM-DD)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1.4f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = activityFactorText,
                        onValueChange = { activityFactorText = it },
                        label = { Text("활동계수 (기본 1.2)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = muscleBaselineText,
                        onValueChange = { muscleBaselineText = it },
                        label = { Text("골격근 기준 (kg)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                val calculatedAge = previewProfile.ageOn(today)
                val zone2Range = previewProfile.zone2HrRange(today)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "자동 계산: 만 ${calculatedAge}세 (${previewProfile.sex.labelKo}, 키 ${previewProfile.heightCm}cm)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Zone 2 유산소 권장 심박수: ${zone2Range.first} ~ ${zone2Range.last} bpm",
                            fontSize = 11.5.sp,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Button(
                    onClick = {
                        onSaveProfile(previewProfile)
                        savedBanner = "✅ 프로필(만 ${calculatedAge}세, 키 ${previewProfile.heightCm}cm) 저장 완료"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("프로필 저장", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        // Section 3: Advice Strictness & Alarms (Sunday 09:00 & Daily 21:00 Missing Meal) & Health Connect
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🔔 엄격 어드바이스 강도 & 알람 설정",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdviceStrictness.entries.forEach { mode ->
                        val selected = strictness == mode
                        val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val fg = if (selected) Color.White else Color(0xFF334155)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { strictness = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.labelKo,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = fg,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                Text(
                    text = strictness.descriptionKo,
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = toleranceText,
                    onValueChange = { toleranceText = it },
                    label = { Text("일일 달성 허용 오차 (kcal, 기본 150)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "일요일 아침 09:00 주간 어드바이스 알람",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "매주 일요일 오전 9시에 주간 리포트 1회 발송",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Switch(checked = sundayAlarm, onCheckedChange = { sundayAlarm = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "저녁 21:00 식사 미기록 실패 경고 알람",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "식사 미기록 시 실패(빨강) 판정 및 21:00 경고 알람",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Switch(checked = missingMealAlarm, onCheckedChange = { missingMealAlarm = it })
                }

                Button(
                    onClick = {
                        val tol = toleranceText.toIntOrNull() ?: 150
                        onSaveSettings(
                            settings.copy(
                                geminiModel = Gemini3Catalog.ensureGemini3Model(selectedModelId),
                                toleranceKcal = tol,
                                strictness = strictness,
                                sundayMorningAlarmEnabled = sundayAlarm,
                                missingMeal21AlarmEnabled = missingMealAlarm
                            )
                        )
                        savedBanner = "✅ 어드바이스 강도(${strictness.labelKo}) 및 알람 설정 저장 완료"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("어드바이스 & 알람 설정 저장", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Health Connect: $healthConnectStatus",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "운동 리스트 · 활동 칼로리 · 체중 동기화 권한",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRequestHealthPermissions,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("권한 요청", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun Gemini3ModelRow(
    option: GeminiModelOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) StatusGreenBg else MaterialTheme.colorScheme.surfaceVariant
    val border = if (selected) StatusGreenBorder else Color.Transparent
    val fg = if (selected) StatusGreenText else Color(0xFF0F172A)

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(if (selected) 1.5.dp else 0.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (selected) StatusGreenBorder else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(
                        text = option.badgeKo,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.id,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = fg,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${option.titleKo} · ${option.descKo}",
                        fontSize = 10.5.sp,
                        color = if (selected) StatusGreenText else Color(0xFF475569),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(StatusGreenBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "선택됨",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
