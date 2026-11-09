package com.minsoo.slimtrack.ui.settings

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
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.R
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

    // 3-Stage Diet Goal Customization States (Defaulting to the current 3-stage plan!)
    var startWeightText by rememberSaveable(profile.startWeightKg) { mutableStateOf(profile.startWeightKg.toString()) }

    var s1MinText by rememberSaveable(profile.stage1WeightMinKg) { mutableStateOf(profile.stage1WeightMinKg.toString()) }
    var s1MaxText by rememberSaveable(profile.stage1WeightMaxKg) { mutableStateOf(profile.stage1WeightMaxKg.toString()) }
    var s1VfText by rememberSaveable(profile.stage1VisceralFatMax) { mutableStateOf(profile.stage1VisceralFatMax.toString()) }
    var s1MonthsText by rememberSaveable(profile.stage1Months) { mutableStateOf(profile.stage1Months.toString()) }

    var s2MinText by rememberSaveable(profile.stage2WeightMinKg) { mutableStateOf(profile.stage2WeightMinKg.toString()) }
    var s2MaxText by rememberSaveable(profile.stage2WeightMaxKg) { mutableStateOf(profile.stage2WeightMaxKg.toString()) }
    var s2VfText by rememberSaveable(profile.stage2VisceralFatMax) { mutableStateOf(profile.stage2VisceralFatMax.toString()) }
    var s2MonthsText by rememberSaveable(profile.stage2Months) { mutableStateOf(profile.stage2Months.toString()) }

    var s3MinText by rememberSaveable(profile.stage3WeightMinKg) { mutableStateOf(profile.stage3WeightMinKg.toString()) }
    var s3MaxText by rememberSaveable(profile.stage3WeightMaxKg) { mutableStateOf(profile.stage3WeightMaxKg.toString()) }
    var s3VfText by rememberSaveable(profile.stage3VisceralFatMax) { mutableStateOf(profile.stage3VisceralFatMax.toString()) }
    var s3MonthsText by rememberSaveable(profile.stage3Months) { mutableStateOf(profile.stage3Months.toString()) }

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
        val startW = startWeightText.toDoubleOrNull()?.takeIf { it in 35.0..250.0 } ?: profile.startWeightKg

        val s1Min = s1MinText.toDoubleOrNull()?.takeIf { it in 35.0..250.0 } ?: profile.stage1WeightMinKg
        val s1Max = s1MaxText.toDoubleOrNull()?.takeIf { it in s1Min..250.0 } ?: maxOf(s1Min, profile.stage1WeightMaxKg)
        val s1Vf = s1VfText.toIntOrNull()?.coerceIn(1, 30) ?: profile.stage1VisceralFatMax
        val s1M = s1MonthsText.toIntOrNull()?.coerceIn(1, 36) ?: profile.stage1Months

        val s2Min = s2MinText.toDoubleOrNull()?.takeIf { it in 35.0..250.0 } ?: profile.stage2WeightMinKg
        val s2Max = s2MaxText.toDoubleOrNull()?.takeIf { it in s2Min..250.0 } ?: maxOf(s2Min, profile.stage2WeightMaxKg)
        val s2Vf = s2VfText.toIntOrNull()?.coerceIn(1, 30) ?: profile.stage2VisceralFatMax
        val s2M = s2MonthsText.toIntOrNull()?.coerceIn(1, 36) ?: profile.stage2Months

        val s3Min = s3MinText.toDoubleOrNull()?.takeIf { it in 35.0..250.0 } ?: profile.stage3WeightMinKg
        val s3Max = s3MaxText.toDoubleOrNull()?.takeIf { it in s3Min..250.0 } ?: maxOf(s3Min, profile.stage3WeightMaxKg)
        val s3Vf = s3VfText.toIntOrNull()?.coerceIn(1, 30) ?: profile.stage3VisceralFatMax
        val s3M = s3MonthsText.toIntOrNull()?.coerceIn(1, 48) ?: profile.stage3Months

        return profile.copy(
            sex = selectedSex,
            heightCm = h,
            birthDate = b,
            activityFactor = act,
            muscleBaselineKg = smm,
            goalStartDate = s,
            startWeightKg = startW,
            stage1WeightMinKg = s1Min,
            stage1WeightMaxKg = s1Max,
            stage1VisceralFatMax = s1Vf,
            stage1Months = s1M,
            stage2WeightMinKg = s2Min,
            stage2WeightMaxKg = s2Max,
            stage2VisceralFatMax = s2Vf,
            stage2Months = s2M,
            stage3WeightMinKg = s3Min,
            stage3WeightMaxKg = s3Max,
            stage3VisceralFatMax = s3Vf,
            stage3Months = s3M
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.sticker_love),
                contentDescription = "프로필 설정 스티커",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚙ 프로필 · 시작일 · 3단계 목표 · Gemini 3.x 설정",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "모든 설정과 3단계 목표는 변경 즉시 로컬에 이중 안전 저장됩니다.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

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

        // Section 1: User Profile & Effective Start Date
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

        // Section 2: Customizable 3-Stage Diet Goals (Keeps current goals as default, editable for any user!)
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
                            text = "🎯 다이어트 3단계 목표 설정 (맞춤 변경 가능)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "현재 목표가 기본 저장되어 있으며, 다른 사용자도 자유롭게 재설정 가능합니다.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            val def = previewProfile.withDefaultGoals()
                            startWeightText = def.startWeightKg.toString()
                            s1MinText = def.stage1WeightMinKg.toString()
                            s1MaxText = def.stage1WeightMaxKg.toString()
                            s1VfText = def.stage1VisceralFatMax.toString()
                            s1MonthsText = def.stage1Months.toString()
                            s2MinText = def.stage2WeightMinKg.toString()
                            s2MaxText = def.stage2WeightMaxKg.toString()
                            s2VfText = def.stage2VisceralFatMax.toString()
                            s2MonthsText = def.stage2Months.toString()
                            s3MinText = def.stage3WeightMinKg.toString()
                            s3MaxText = def.stage3WeightMaxKg.toString()
                            s3VfText = def.stage3VisceralFatMax.toString()
                            s3MonthsText = def.stage3Months.toString()
                            onSaveProfile(def)
                            savedBanner = "✅ 3단계 다이어트 목표가 기본 설정(1단계 93~94kg, 2단계 85~88kg, 3단계 78~82kg)으로 복원되었습니다."
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("기본값 복원", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                OutlinedTextField(
                    value = startWeightText,
                    onValueChange = {
                        startWeightText = it
                        if (it.toDoubleOrNull() != null) autoPersistAll()
                    },
                    label = { Text("출발 기준 체중 (kg, 기본 98.9)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                StageGoalEditorRow(
                    stageTitle = "1단계 (단기 목표)",
                    minWeight = s1MinText,
                    maxWeight = s1MaxText,
                    visceralFat = s1VfText,
                    months = s1MonthsText,
                    onMinChange = { s1MinText = it; if (it.toDoubleOrNull() != null) autoPersistAll() },
                    onMaxChange = { s1MaxText = it; if (it.toDoubleOrNull() != null) autoPersistAll() },
                    onVfChange = { s1VfText = it; if (it.toIntOrNull() != null) autoPersistAll() },
                    onMonthsChange = { s1MonthsText = it; if (it.toIntOrNull() != null) autoPersistAll() }
                )

                StageGoalEditorRow(
                    stageTitle = "2단계 (중기 목표)",
                    minWeight = s2MinText,
                    maxWeight = s2MaxText,
                    visceralFat = s2VfText,
                    months = s2MonthsText,
                    onMinChange = { s2MinText = it; if (it.toDoubleOrNull() != null) autoPersistAll() },
                    onMaxChange = { s2MaxText = it; if (it.toDoubleOrNull() != null) autoPersistAll() },
                    onVfChange = { s2VfText = it; if (it.toIntOrNull() != null) autoPersistAll() },
                    onMonthsChange = { s2MonthsText = it; if (it.toIntOrNull() != null) autoPersistAll() }
                )

                StageGoalEditorRow(
                    stageTitle = "3단계 (최종 목표)",
                    minWeight = s3MinText,
                    maxWeight = s3MaxText,
                    visceralFat = s3VfText,
                    months = s3MonthsText,
                    onMinChange = { s3MinText = it; if (it.toDoubleOrNull() != null) autoPersistAll() },
                    onMaxChange = { s3MaxText = it; if (it.toDoubleOrNull() != null) autoPersistAll() },
                    onVfChange = { s3VfText = it; if (it.toIntOrNull() != null) autoPersistAll() },
                    onMonthsChange = { s3MonthsText = it; if (it.toIntOrNull() != null) autoPersistAll() }
                )
            }
        }

        // Section 3: Compact Gemini 3.x Select Box & API Key
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

        // Section 4: Advice Strictness, Alarms & Health Connect
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
                            text = "운동 · 활동 칼로리 · 걸음수(방식A) · 체중 · 체지방 권한",
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

                Button(
                    onClick = {
                        autoPersistAll()
                        val p = buildCurrentProfile()
                        savedBanner = "✅ 프로필·3단계 목표(1단계 ${p.stage1WeightMinKg.toInt()}~${p.stage1WeightMaxKg.toInt()}kg) 및 설정이 로컬에 영구 저장되었습니다."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💾 전체 프로필 · 3단계 목표 · 설정 안전 저장",
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

@Composable
private fun StageGoalEditorRow(
    stageTitle: String,
    minWeight: String,
    maxWeight: String,
    visceralFat: String,
    months: String,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onVfChange: (String) -> Unit,
    onMonthsChange: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stageTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = minWeight,
                    onValueChange = onMinChange,
                    label = { Text("최소(kg)", fontSize = 9.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxWeight,
                    onValueChange = onMaxChange,
                    label = { Text("최대(kg)", fontSize = 9.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = visceralFat,
                    onValueChange = onVfChange,
                    label = { Text("내장지방(Lv)", fontSize = 9.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = months,
                    onValueChange = onMonthsChange,
                    label = { Text("기간(개월)", fontSize = 9.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(0.95f),
                    singleLine = true
                )
            }
        }
    }
}
