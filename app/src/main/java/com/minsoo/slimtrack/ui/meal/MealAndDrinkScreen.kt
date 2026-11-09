package com.minsoo.slimtrack.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.data.db.DrinkEntity
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.FoodItemEntry
import com.minsoo.slimtrack.data.db.MealType
import com.minsoo.slimtrack.domain.AlcoholCalculator
import com.minsoo.slimtrack.domain.AlcoholPreset
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import com.minsoo.slimtrack.ui.theme.StatusRedBg
import com.minsoo.slimtrack.ui.theme.StatusRedText
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealAndDrinkScreen(
    selectedDate: LocalDate,
    selectedMealType: MealType,
    stagedItems: List<FoodItemEntry>,
    stagedNotes: String,
    stagedConfidence: Float,
    isAnalyzingPhoto: Boolean,
    analysisStatusMessage: String?,
    todayDrinks: List<DrinkEntity>,
    todayExercises: List<ExerciseEntity>,
    onSelectMealType: (MealType) -> Unit,
    onTakePhotoClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onUpdateStagedItem: (Int, FoodItemEntry) -> Unit,
    onRemoveStagedItem: (Int) -> Unit,
    onAddManualFoodItem: (FoodItemEntry) -> Unit,
    onSaveMeal: () -> Unit,
    onAddDrink: (AlcoholPreset, Int, Float) -> Unit,
    onDeleteDrink: (DrinkEntity) -> Unit,
    onSyncHealthConnectExercises: () -> Unit,
    onAddManualExercise: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualFoodName by remember { mutableStateOf("") }
    var manualFoodKcal by remember { mutableStateOf("") }
    var manualFoodProtein by remember { mutableStateOf("20") }

    var selectedDrinkPreset by remember { mutableStateOf(AlcoholPreset.SOJU) }
    var drinkVolumeMl by remember { mutableStateOf("360") }
    var drinkAbvText by remember { mutableStateOf("16.5") }

    var manualExTitle by remember { mutableStateOf("Zone 2 러닝") }
    var manualExMin by remember { mutableStateOf("40") }
    var manualExKcal by remember { mutableStateOf("320") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "🍽 $selectedDate 식단 · 회식(술/안주) · 운동 기록",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Section 1: Meal & Anju Photo Calorie Analysis
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📸 매일 3식 및 회식 안주 사진 칼로리 계산",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "아침·점심·저녁 식사 또는 회식 안주를 사진으로 촬영/선택하면 Gemini Vision이 음식별 칼로리와 단백질을 자동 계산합니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedMealType == type,
                            onClick = { onSelectMealType(type) },
                            label = { Text("${type.emoji} ${type.labelKo}") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTakePhotoClick,
                        enabled = !isAnalyzingPhoto,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${selectedMealType.labelKo} 카메라 촬영")
                    }
                    OutlinedButton(
                        onClick = onPickPhotoClick,
                        enabled = !isAnalyzingPhoto,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("갤러리 사진 선택")
                    }
                }

                if (isAnalyzingPhoto) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Gemini Vision으로 음식 사진 칼로리 분석 중...", fontSize = 13.sp)
                    }
                }

                if (!analysisStatusMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = analysisStatusMessage,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider()
                Text(
                    text = "품목 확인 및 직접 추가 (편집 가능)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                stagedItems.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { onUpdateStagedItem(index, item.copy(name = it)) },
                            label = { Text("음식명") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = item.kcal.toString(),
                            onValueChange = { v ->
                                val k = v.toIntOrNull() ?: 0
                                onUpdateStagedItem(index, item.copy(kcal = k))
                            },
                            label = { Text("kcal") },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = item.proteinGrams.toInt().toString(),
                            onValueChange = { v ->
                                val p = v.toFloatOrNull() ?: 0f
                                onUpdateStagedItem(index, item.copy(proteinGrams = p))
                            },
                            label = { Text("단백(g)") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                        IconButton(onClick = { onRemoveStagedItem(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
                    }
                }

                // Quick manual item add row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualFoodName,
                        onValueChange = { manualFoodName = it },
                        label = { Text("음식/안주 직접입력") },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualFoodKcal,
                        onValueChange = { manualFoodKcal = it },
                        label = { Text("kcal") },
                        modifier = Modifier.weight(0.9f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualFoodProtein,
                        onValueChange = { manualFoodProtein = it },
                        label = { Text("단백(g)") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            val k = manualFoodKcal.toIntOrNull() ?: 0
                            val p = manualFoodProtein.toFloatOrNull() ?: 0f
                            if (manualFoodName.isNotBlank() && k > 0) {
                                onAddManualFoodItem(
                                    FoodItemEntry(
                                        name = manualFoodName.trim(),
                                        kcal = k,
                                        proteinGrams = p
                                    )
                                )
                                manualFoodName = ""
                                manualFoodKcal = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "품목 추가")
                    }
                }

                val stagedTotalKcal = stagedItems.sumOf { it.kcal }
                val stagedTotalProtein = stagedItems.sumOf { it.proteinGrams.toDouble() }.toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "합계: ${stagedTotalKcal}kcal (단백질 ${stagedTotalProtein}g)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = onSaveMeal,
                        enabled = stagedItems.isNotEmpty()
                    ) {
                        Text("${selectedMealType.labelKo} 기록 저장 (${stagedTotalKcal}kcal)")
                    }
                }
            }
        }

        // Section 2: Manual Alcohol Calculator (for company dinners / 회식)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalBar, contentDescription = null, tint = StatusRedText)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "🍺 회식 수동 음주량 칼로리 계산기",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "공식: ml × 도수(%) × 0.789(g/ml) × 7kcal + 주종별 탄수화물 kcal. 안주는 위에서 [🍻 안주]를 선택해 사진으로 촬영하세요.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AlcoholPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = selectedDrinkPreset == preset,
                            onClick = {
                                selectedDrinkPreset = preset
                                drinkVolumeMl = preset.bottleVolumeMl.toString()
                                drinkAbvText = preset.defaultAbvPct.toString()
                            },
                            label = { Text(preset.labelKo) }
                        )
                    }
                }

                // Quick unit buttons (1잔, 2잔, 1병/캔, 2병/캔)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { drinkVolumeMl = selectedDrinkPreset.unitVolumeMl.toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1잔(${selectedDrinkPreset.unitVolumeMl}ml)", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { drinkVolumeMl = (selectedDrinkPreset.unitVolumeMl * 3).toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("3잔(${selectedDrinkPreset.unitVolumeMl * 3}ml)", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { drinkVolumeMl = selectedDrinkPreset.bottleVolumeMl.toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("1병/캔(${selectedDrinkPreset.bottleVolumeMl}ml)", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { drinkVolumeMl = (selectedDrinkPreset.bottleVolumeMl * 2).toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("2병/캔(${selectedDrinkPreset.bottleVolumeMl * 2}ml)", fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = drinkVolumeMl,
                        onValueChange = { drinkVolumeMl = it },
                        label = { Text("마신 양 (ml)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = drinkAbvText,
                        onValueChange = { drinkAbvText = it },
                        label = { Text("도수 (%)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                val vol = drinkVolumeMl.toIntOrNull() ?: 0
                val abv = drinkAbvText.toDoubleOrNull() ?: selectedDrinkPreset.defaultAbvPct
                val previewDrinkKcal = AlcoholCalculator.calculatePresetKcal(selectedDrinkPreset, vol, abv)

                Surface(
                    color = StatusRedBg,
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
                        Text(
                            text = "${selectedDrinkPreset.labelKo} ${vol}ml (${abv}%) = ${previewDrinkKcal}kcal",
                            fontWeight = FontWeight.Bold,
                            color = StatusRedText
                        )
                        Button(
                            onClick = {
                                if (vol > 0) {
                                    onAddDrink(selectedDrinkPreset, vol, abv.toFloat())
                                }
                            }
                        ) {
                            Text("술 기록 추가")
                        }
                    }
                }
            }
        }

        // Section 3: Health Connect Exercise List & Manual Exercise
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = StatusGreenText)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "🏃 Google Health Connect 운동 리스트",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(onClick = onSyncHealthConnectExercises) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("동기화", fontSize = 12.sp)
                    }
                }

                if (todayExercises.isEmpty()) {
                    Text(
                        text = "오늘 동기화된 운동 기록이 없습니다. Health Connect 동기화를 누르거나 아래에서 직접 추가하세요.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                } else {
                    todayExercises.forEach { ex ->
                        Surface(
                            color = StatusGreenBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "[${ex.source}] ${ex.title} · ${ex.durationMinutes}분",
                                    fontSize = 13.sp,
                                    color = StatusGreenText,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "+${ex.activeKcal} kcal",
                                    fontSize = 13.sp,
                                    color = StatusGreenText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()
                Text("운동 수동 입력 (보조)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualExTitle,
                        onValueChange = { manualExTitle = it },
                        label = { Text("운동명") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualExMin,
                        onValueChange = { manualExMin = it },
                        label = { Text("분") },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualExKcal,
                        onValueChange = { manualExKcal = it },
                        label = { Text("소모 kcal") },
                        modifier = Modifier.weight(0.9f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val min = manualExMin.toIntOrNull() ?: 30
                            val kcal = manualExKcal.toIntOrNull() ?: 0
                            if (manualExTitle.isNotBlank() && kcal > 0) {
                                onAddManualExercise(manualExTitle.trim(), min, kcal)
                            }
                        }
                    ) {
                        Text("추가")
                    }
                }
            }
        }
    }
}
