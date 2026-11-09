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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var selectedSexName by rememberSaveable(profile.sex.name) { mutableStateOf(profile.sex.name) }
    val selectedSex = runCatching { Sex.valueOf(selectedSexName) }.getOrDefault(profile.sex)

    var heightText by rememberSaveable(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var birthDateText by rememberSaveable(profile.birthDate.toString()) { mutableStateOf(profile.birthDate.toString()) }
    var startDateText by rememberSaveable(profile.goalStartDate.toString()) { mutableStateOf(profile.goalStartDate.toString()) }
    var activityFactorText by rememberSaveable(profile.activityFactor) { mutableStateOf(profile.activityFactor.toString()) }
    var muscleBaselineText by rememberSaveable(profile.muscleBaselineKg) { mutableStateOf(profile.muscleBaselineKg.toString()) }

    var apiKeyText by rememberSaveable(currentApiKey) { mutableStateOf(currentApiKey) }
    var selectedModelId by rememberSaveable(settings.geminiModel) {
        mutableStateOf(Gemini3Catalog.ensureGemini3Model(settings.geminiModel))
    }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var toleranceText by rememberSaveable(settings.toleranceKcal) { mutableStateOf(settings.toleranceKcal.toString()) }
    var strictnessName by rememberSaveable(settings.strictness.name) { mutableStateOf(settings.strictness.name) }
    val strictness = runCatching { AdviceStrictness.valueOf(strictnessName) }.getOrDefault(settings.strictness)
    var sundayAlarm by rememberSaveable(settings.sundayMorningAlarmEnabled) { mutableStateOf(settings.sundayMorningAlarmEnabled) }
    var missingMealAlarm by rememberSaveable(settings.missingMeal21AlarmEnabled) { mutableStateOf(settings.missingMeal21AlarmEnabled) }

    var savedBanner by remember { mutableStateOf<String?>(null) }

    fun buildCurrentProfile(): UserProfile {
        val h = heightText.toDoubleOrNull()?.takeIf { it in 100.0..250.0 } ?: profile.heightCm
        val b = runCatching { LocalDate.parse(birthDateText.trim()) }.getOrDefault(profile.birthDate)
        val s = runCatching { LocalDate.parse(startDateText.trim()) }.getOrDefault(profile.goalStartDate)
        val act = activityFactorText.toDoubleOrNull()?.takeIf { it in 1.0..2.5 } ?: profile.activityFactor
        val smm = muscleBaselineText.toDoubleOrNull()?.takeIf { it in 10.0..80.0 } ?: profile.muscleBaselineKg
        return profile.copy(
            sex = selectedSex,
            heightCm = h,
            birthDate = b,
            activityFactor = act,
            muscleBaselineKg = smm,
            goalStartDate = s
        )
    }

    fun buildCurrentSettings(): AppSettings {
        val tol = toleranceText.toIntOrNull()?.coerceIn(0, 1000) ?: settings.toleranceKcal
        return settings.copy(
            geminiModel = Gemini3Catalog.ensureGemini3Model(selectedModelId),
            toleranceKcal = tol,
            strictness = strictness,
            sundayMorningAlarmEnabled = sundayAlarm,
            missingMeal21AlarmEnabled = missingMealAlarm
        )
    }

    // Auto-save helper so user edits are immediately persisted even without pressing the bottom button
    fun autoPersistAll() {
        onSaveProfile(buildCurrentProfile())
        onSaveSettings(buildCurrentSettings())
        if (apiKeyText.isNotBlank()) {
            onSaveApiKey(apiKeyText)
        }
    }

    val previewProfile = buildCurrentProfile()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "⚙ 사용자 프로필 · 유효 시작일 · Gemini 3.x 설정",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
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

        // Section 1: User Profile & Effective Start Date (Auto-persisted locally!)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "👤 사용자 프로필 & 기록 유효 시작일 (자동 안전 저장)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )

                // Effective Start Date Row (Point 7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startDateText,
                        onValueChange = {
                            startDateText = it
                            if (runCatching { LocalDate.parse(it.trim()) }.isSuccess) {
                                autoPersistAll()
                            }
                        },
                        label = { Text("📅 기록 유효 시작일 (YYYY-MM-DD)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            startDateText = today.toString()
                            autoPersistAll()
                            savedBanner = "✅ 기록 유효 시작일이 오늘($today)로 설정되었으며 이전 데이터는 제외됩니다."
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("오늘로 설정", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
                Text(
                    text = "※ 시작일(${previewProfile.goalStartDate}) 이전 데이터는 캘린더·누적 칼로리·코치 판정에서 모두 제외됩니다.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                HorizontalDivider()

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
                                .clickable {
                                    selectedSexName = sex.name
                                    autoPersistAll()
                                }
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
                        onValueChange = {
                            heightText = it
                            if (it.toDoubleOrNull() != null) autoPersistAll()
                        },
                        label = { Text("키 (cm)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = birthDateText,
                        onValueChange = {
                            birthDateText = it
                            if (runCatching { LocalDate.parse(it.trim()) }.isSuccess) autoPersistAll()
                        },
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
                        onValueChange = {
                            activityFactorText = it
                            if (it.toDoubleOrNull() != null) autoPersistAll()
                        },
                        label = { Text("활동계수 (기본 1.2)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = muscleBaselineText,
                        onValueChange = {
                            muscleBaselineText = it
                            if (it.toDoubleOrNull() != null) autoPersistAll()
                        },
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
                            text = "자동 계산: 만 ${calculatedAge}세 (${previewProfile.sex.labelKo}, 키 ${previewProfile.heightCm}cm) · 시작일 ${previewProfile.goalStartDate}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Zone 2 유산소 권장 심박수: ${zone2Range.first} ~ ${zone2Range.last} bpm",
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Section 2: Compact Gemini 3.x Select Box & API Key (Point 8)
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
                    Text(
                        text = "🤖 Gemini 3.x 모델 선택박스 & API 키",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { onFetchAvailableGemini3Models(apiKeyText) },
                        enabled = !isFetchingModels,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (isFetchingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("목록 갱신", fontSize = 11.sp, maxLines = 1, softWrap = false)
                    }
                }

                // Compact 1-line Dropdown Select Box for Gemini 3.x model!
                val selectedOption = availableGemini3Models.firstOrNull { it.id == selectedModelId }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .clickable { modelDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "선택된 Gemini 3.x 모델",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = if (selectedOption != null) {
                                        "${selectedOption.id} (${selectedOption.titleKo})"
                                    } else {
                                        selectedModelId
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "모델 선택",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        availableGemini3Models.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "${option.id} (${option.badgeKo})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "${option.titleKo} · ${option.descKo}",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                },
                                onClick = {
                                    selectedModelId = option.id
                                    modelDropdownExpanded = false
                                    autoPersistAll()
                                    savedBanner = "✅ Gemini 3.x 모델이 [${option.id}]로 변경·저장되었습니다."
                                }
                            )
                        }
                    }
                }

                if (!modelFetchMessage.isNullOrBlank()) {
                    Text(
                        text = modelFetchMessage,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        onSaveApiKey(it)
                    },
                    label = { Text("Google AI Studio Gemini API Key (암호화 로컬 저장)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Section 3: Advice Strictness, Alarms & Health Connect
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🔔 코치 어드바이스 강도 · 알람 · Health 권한",
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
                                .clickable {
                                    strictnessName = mode.name
                                    autoPersistAll()
                                }
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

                OutlinedTextField(
                    value = toleranceText,
                    onValueChange = {
                        toleranceText = it
                        if (it.toIntOrNull() != null) autoPersistAll()
                    },
                    label = { Text("일일 달성 허용 오차 (kcal, 기본 150)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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
                    Switch(
                        checked = sundayAlarm,
                        onCheckedChange = {
                            sundayAlarm = it
                            autoPersistAll()
                        }
                    )
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
                    Switch(
                        checked = missingMealAlarm,
                        onCheckedChange = {
                            missingMealAlarm = it
                            autoPersistAll()
                        }
                    )
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
                            text = "운동 리스트 · 활동 칼로리 · 체중 · 체지방 동기화 권한",
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

                // Single Unified Save Button
                Button(
                    onClick = {
                        autoPersistAll()
                        val p = buildCurrentProfile()
                        savedBanner = "✅ 전체 프로필(만 ${p.ageOn(today)}세, 시작일 ${p.goalStartDate}) 및 설정이 로컬에 영구 저장되었습니다."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💾 전체 프로필 및 설정 안전 저장",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
