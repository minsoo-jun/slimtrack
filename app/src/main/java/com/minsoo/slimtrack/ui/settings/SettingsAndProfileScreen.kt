package com.minsoo.slimtrack.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.data.health.HealthConnectStatus
import com.minsoo.slimtrack.data.prefs.AppSettings
import com.minsoo.slimtrack.data.prefs.Sex
import com.minsoo.slimtrack.data.prefs.UserProfile
import com.minsoo.slimtrack.domain.AdviceStrictness
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndProfileScreen(
    today: LocalDate,
    profile: UserProfile,
    settings: AppSettings,
    currentApiKey: String,
    healthConnectStatus: HealthConnectStatus,
    onSaveProfile: (UserProfile) -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onRequestHealthPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSex by remember(profile.sex) { mutableStateOf(profile.sex) }
    var heightText by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var birthDateText by remember(profile.birthDate) { mutableStateOf(profile.birthDate.toString()) }
    var activityFactorText by remember(profile.activityFactor) { mutableStateOf(profile.activityFactor.toString()) }
    var muscleBaselineText by remember(profile.muscleBaselineKg) { mutableStateOf(profile.muscleBaselineKg.toString()) }

    var apiKeyText by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var modelText by remember(settings.geminiModel) { mutableStateOf(settings.geminiModel) }
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "⚙ 사용자 프로필 & 앱 설정",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (!savedBanner.isNullOrBlank()) {
            Surface(
                color = StatusGreenBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = savedBanner!!,
                    color = StatusGreenText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Section 1: User Profile (Sex, Height, BirthDate -> Age & Zone 2 HR)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "👤 사용자 프로필 (성별 · 키 · 생년월일/나이 계산)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { sex ->
                        FilterChip(
                            selected = selectedSex == sex,
                            onClick = { selectedSex = sex },
                            label = { Text(sex.labelKo) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("키 (cm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = birthDateText,
                        onValueChange = { birthDateText = it },
                        label = { Text("생년월일 (YYYY-MM-DD)") },
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
                        label = { Text("활동계수 (기본 1.2)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = muscleBaselineText,
                        onValueChange = { muscleBaselineText = it },
                        label = { Text("골격근 방어 기준 (kg)") },
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
                            fontSize = 13.sp
                        )
                        Text(
                            text = "2단계 권장 Zone 2 저강도 유산소 심박수 구간: ${zone2Range.first} ~ ${zone2Range.last} bpm",
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }

                Button(
                    onClick = {
                        onSaveProfile(previewProfile)
                        savedBanner = "✅ 사용자 프로필(만 ${calculatedAge}세, 키 ${previewProfile.heightCm}cm)이 저장되었습니다."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("프로필 저장")
                }
            }
        }

        // Section 2: Advice Strictness & Alarms (Sunday 09:00 & Daily 21:00 Missing Meal)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🔔 엄격 어드바이스 강도 & 알람 시간 설정",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("어드바이스 강도 선택", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdviceStrictness.entries.forEach { mode ->
                        FilterChip(
                            selected = strictness == mode,
                            onClick = { strictness = mode },
                            label = { Text(mode.labelKo) }
                        )
                    }
                }
                Text(text = strictness.descriptionKo, fontSize = 12.sp, color = Color(0xFF475569))

                OutlinedTextField(
                    value = toleranceText,
                    onValueChange = { toleranceText = it },
                    label = { Text("일일 달성 허용 오차 (kcal, 기본 150)") },
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
                        Text("일요일 아침 09:00 주간 어드바이스 알람", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("주 1회 일요일 오전 9시에 주간 성적표와 엄격 어드바이스를 발송합니다.", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(checked = sundayAlarm, onCheckedChange = { sundayAlarm = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("저녁 21:00 식사 미기록 실패 경고 알람", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("식사를 기록하지 않은 날은 실패(빨강)로 판정하고 저녁 9시에 알람을 줍니다.", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(checked = missingMealAlarm, onCheckedChange = { missingMealAlarm = it })
                }

                Button(
                    onClick = {
                        val tol = toleranceText.toIntOrNull() ?: 150
                        onSaveSettings(
                            settings.copy(
                                geminiModel = modelText.ifBlank { "gemini-2.5-flash" },
                                toleranceKcal = tol,
                                strictness = strictness,
                                sundayMorningAlarmEnabled = sundayAlarm,
                                missingMeal21AlarmEnabled = missingMealAlarm
                            )
                        )
                        savedBanner = "✅ 어드바이스 강도(${strictness.labelKo}) 및 알람 설정이 저장되었습니다."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("어드바이스 & 알람 설정 저장")
                }
            }
        }

        // Section 3: Gemini API Key & Health Connect Permissions
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🔑 Gemini API 키 & Health Connect 권한",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("Google AI Studio Gemini API Key (암호화 저장)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text("Gemini 모델명 (기본: gemini-2.5-flash)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        onSaveApiKey(apiKeyText)
                        onSaveSettings(settings.copy(geminiModel = modelText.ifBlank { "gemini-2.5-flash" }))
                        savedBanner = "✅ Gemini API 키가 암호화 저장소에 저장되었습니다."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("API 키 저장")
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Health Connect 상태: $healthConnectStatus", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("운동 리스트, 활동 칼로리, 체중/체지방 동기화 권한", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onRequestHealthPermissions) {
                        Text("권한 요청")
                    }
                }
            }
        }
    }
}
