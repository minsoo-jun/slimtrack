package com.minsoo.slimtrack.ui.meal

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.data.db.DrinkEntity
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.FoodItemEntry
import com.minsoo.slimtrack.data.db.MealEntity
import com.minsoo.slimtrack.data.db.MealType
import com.minsoo.slimtrack.domain.AlcoholCalculator
import com.minsoo.slimtrack.domain.AlcoholPreset
import com.minsoo.slimtrack.ui.theme.StatusGreenBg
import com.minsoo.slimtrack.ui.theme.StatusGreenBorder
import com.minsoo.slimtrack.ui.theme.StatusGreenText
import com.minsoo.slimtrack.ui.theme.StatusRedBg
import com.minsoo.slimtrack.ui.theme.StatusRedBorder
import com.minsoo.slimtrack.ui.theme.StatusRedText
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val itemJsonParser = Json { ignoreUnknownKeys = true }

@Composable
fun MealAndDrinkScreen(
    selectedDate: LocalDate,
    selectedMealType: MealType,
    stagedItems: List<FoodItemEntry>,
    stagedNotes: String,
    stagedConfidence: Float,
    isAnalyzingPhoto: Boolean,
    analysisStatusMessage: String?,
    todayMeals: List<MealEntity>,
    todayDrinks: List<DrinkEntity>,
    todayExercises: List<ExerciseEntity>,
    onSelectMealType: (MealType) -> Unit,
    onTakePhotoClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onUpdateStagedItem: (Int, FoodItemEntry) -> Unit,
    onRemoveStagedItem: (Int) -> Unit,
    onAddManualFoodItem: (FoodItemEntry) -> Unit,
    onSaveMeal: () -> Unit,
    onDeleteMeal: (MealEntity) -> Unit,
    onAddDrink: (AlcoholPreset, Int, Float) -> Unit,
    onDeleteDrink: (DrinkEntity) -> Unit,
    onSyncHealthConnectExercises: () -> Unit,
    onAddManualExercise: (String, Int, Int) -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit = {},
    onSaveDailySteps: (Int) -> Unit = {},
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
    var manualStepsInput by remember { mutableStateOf("10000") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🍽 $selectedDate 식단 · 회식(술/안주) · 운동",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )

        if (!analysisStatusMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = analysisStatusMessage,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        // Section 0: Saved Meals for Selected Date (Persisted in local Room DB with 1-tap Delete!)
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "✅ $selectedDate 저장된 식단 내역 (${todayMeals.size}건)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = StatusGreenBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, StatusGreenBorder, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "총 ${todayMeals.sumOf { it.totalKcal }} kcal",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusGreenText,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (todayMeals.isEmpty()) {
                    Text(
                        text = "아직 저장된 식단이 없습니다. 아래에서 사진 촬영 또는 수동 입력 후 [기록 저장]을 눌러주세요.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    todayMeals.forEach { meal ->
                        val mealTypeEnum = runCatching { MealType.valueOf(meal.mealType) }.getOrNull()
                        val typeTitle = mealTypeEnum?.let { "${it.emoji} ${it.labelKo}" } ?: meal.mealType
                        val parsedItems = remember(meal.itemsJson) {
                            runCatching { itemJsonParser.decodeFromString<List<FoodItemEntry>>(meal.itemsJson) }
                                .getOrDefault(emptyList())
                        }
                        val itemsSummary = if (parsedItems.isNotEmpty()) {
                            parsedItems.joinToString(", ") { "${it.name}(${it.kcal}kcal)" }
                        } else {
                            meal.notes
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "[$typeTitle] ${meal.totalKcal} kcal (단백질 ${meal.proteinGrams.toInt()}g)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (itemsSummary.isNotBlank()) {
                                        Text(
                                            text = itemsSummary,
                                            fontSize = 11.sp,
                                            color = Color(0xFF475569),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                DeleteRecordButton(onClick = { onDeleteMeal(meal) })
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Meal & Anju Photo Calorie Analysis
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📸 새 식사 & 회식 안주 추가 (사진 또는 직접 입력)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )

                // 5 MealType chips in a single equal-width row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealType.entries.forEach { type ->
                        val isSelected = selectedMealType == type
                        val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val fg = if (isSelected) Color.White else Color(0xFF334155)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { onSelectMealType(type) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${type.emoji}${type.labelKo}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = fg,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Camera & Gallery buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTakePhotoClick,
                        enabled = !isAnalyzingPhoto,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "${selectedMealType.labelKo} 촬영",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    OutlinedButton(
                        onClick = onPickPhotoClick,
                        enabled = !isAnalyzingPhoto,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "앨범에서 선택",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (isAnalyzingPhoto) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Gemini 3.x Vision으로 칼로리 분석 중...",
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                HorizontalDivider()
                Text(
                    text = "품목 확인 및 직접 추가",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    softWrap = false
                )

                stagedItems.forEachIndexed { index, item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { onUpdateStagedItem(index, item.copy(name = it)) },
                                label = { Text("음식명", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                                modifier = Modifier.weight(1.4f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = item.kcal.toString(),
                                onValueChange = { v ->
                                    val k = v.toIntOrNull() ?: 0
                                    onUpdateStagedItem(index, item.copy(kcal = k))
                                },
                                label = { Text("kcal", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                                modifier = Modifier.weight(0.9f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = item.proteinGrams.toInt().toString(),
                                onValueChange = { v ->
                                    val p = v.toFloatOrNull() ?: 0f
                                    onUpdateStagedItem(index, item.copy(proteinGrams = p))
                                },
                                label = { Text("단백(g)", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = { onRemoveStagedItem(index) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Clean single-line manual food entry row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualFoodName,
                        onValueChange = { manualFoodName = it },
                        label = { Text("음식명 입력", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1.4f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualFoodKcal,
                        onValueChange = { manualFoodKcal = it },
                        label = { Text("kcal", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(0.85f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualFoodProtein,
                        onValueChange = { manualFoodProtein = it },
                        label = { Text("단백(g)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(0.75f),
                        singleLine = true
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable {
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
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "추가", tint = Color.White)
                        }
                    }
                }

                val stagedTotalKcal = stagedItems.sumOf { it.kcal }
                val stagedTotalProtein = stagedItems.sumOf { it.proteinGrams.toDouble() }.toInt()

                Button(
                    onClick = onSaveMeal,
                    enabled = stagedItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${selectedMealType.labelKo} 기록 저장 · 합계 ${stagedTotalKcal}kcal (단백질 ${stagedTotalProtein}g)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section 2: Manual Alcohol Calculator (for company dinners / 회식)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalBar, contentDescription = null, tint = StatusRedText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "🍺 회식 수동 음주량 칼로리 계산기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                val presetsRow1 = AlcoholPreset.entries.take(4)
                val presetsRow2 = AlcoholPreset.entries.drop(4)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetsRow1.forEach { preset ->
                            DrinkPresetChip(
                                preset = preset,
                                selected = selectedDrinkPreset == preset,
                                onClick = {
                                    selectedDrinkPreset = preset
                                    drinkVolumeMl = preset.bottleVolumeMl.toString()
                                    drinkAbvText = preset.defaultAbvPct.toString()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetsRow2.forEach { preset ->
                            DrinkPresetChip(
                                preset = preset,
                                selected = selectedDrinkPreset == preset,
                                onClick = {
                                    selectedDrinkPreset = preset
                                    drinkVolumeMl = preset.bottleVolumeMl.toString()
                                    drinkAbvText = preset.defaultAbvPct.toString()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickVolumeTile(
                        topLabel = "1잔",
                        mlValue = selectedDrinkPreset.unitVolumeMl,
                        onClick = { drinkVolumeMl = selectedDrinkPreset.unitVolumeMl.toString() },
                        modifier = Modifier.weight(1f)
                    )
                    QuickVolumeTile(
                        topLabel = "3잔",
                        mlValue = selectedDrinkPreset.unitVolumeMl * 3,
                        onClick = { drinkVolumeMl = (selectedDrinkPreset.unitVolumeMl * 3).toString() },
                        modifier = Modifier.weight(1f)
                    )
                    QuickVolumeTile(
                        topLabel = "1병/캔",
                        mlValue = selectedDrinkPreset.bottleVolumeMl,
                        onClick = { drinkVolumeMl = selectedDrinkPreset.bottleVolumeMl.toString() },
                        modifier = Modifier.weight(1f)
                    )
                    QuickVolumeTile(
                        topLabel = "2병/캔",
                        mlValue = selectedDrinkPreset.bottleVolumeMl * 2,
                        onClick = { drinkVolumeMl = (selectedDrinkPreset.bottleVolumeMl * 2).toString() },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = drinkVolumeMl,
                        onValueChange = { drinkVolumeMl = it },
                        label = { Text("마신 양 (ml)", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = drinkAbvText,
                        onValueChange = { drinkAbvText = it },
                        label = { Text("도수 (%)", fontSize = 11.sp, maxLines = 1, softWrap = false) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StatusRedBorder, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedDrinkPreset.labelKo} ${vol}ml (${abv}%) = ${previewDrinkKcal}kcal",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = StatusRedText,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (vol > 0) {
                                    onAddDrink(selectedDrinkPreset, vol, abv.toFloat())
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("술 추가", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (todayDrinks.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "저장된 음주 내역 (${todayDrinks.sumOf { it.kcal }}kcal)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    todayDrinks.forEach { d ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${d.displayLabel} ${d.volumeMl}ml (${d.abvPct}%) — ${d.kcal}kcal",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                DeleteRecordButton(onClick = { onDeleteDrink(d) })
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Health Connect Exercise List & Steps (Method A)
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = StatusGreenText, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "🏃 Health 운동 & 도보(방식 A: 4천보 초과분)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onSyncHealthConnectExercises,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("동기화", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                }
                Text(
                    text = "※ 방식 A: 기본 생활 걸음(4,000보)은 활동계수(1.2)에 포함되며, 4,000보를 초과한 걸음수만 체중 비례 칼로리로 자동 합산됩니다.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Quick Steps (Method A) Input Row
                val previewSteps = manualStepsInput.toIntOrNull() ?: 0
                val previewBonusSteps = (previewSteps - com.minsoo.slimtrack.domain.CalorieEngine.BASELINE_STEPS_INCLUDED_IN_TDEE).coerceAtLeast(0)
                val previewBonusKcal = com.minsoo.slimtrack.domain.CalorieEngine.computeBonusStepsKcal(previewSteps, 98.4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualStepsInput,
                        onValueChange = { manualStepsInput = it },
                        label = { Text("오늘 총 걸음수 (보)", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    Surface(
                        color = StatusGreenBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "초과 ${previewBonusSteps}보",
                                fontSize = 10.sp,
                                color = StatusGreenText,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "+${previewBonusKcal} kcal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreenText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (previewSteps > 0) {
                                onSaveDailySteps(previewSteps)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text("걸음 반영", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                if (todayExercises.isEmpty()) {
                    Text(
                        text = "동기화된 운동이 없습니다. 동기화하거나 아래에서 직접 추가하세요.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    todayExercises.forEach { ex ->
                        Surface(
                            color = StatusGreenBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[${ex.source}] ${ex.title} (${ex.durationMinutes}분) · ${ex.activeKcal} kcal",
                                    fontSize = 12.sp,
                                    color = StatusGreenText,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                DeleteRecordButton(onClick = { onDeleteExercise(ex) })
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualExTitle,
                        onValueChange = { manualExTitle = it },
                        label = { Text("운동명", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualExMin,
                        onValueChange = { manualExMin = it },
                        label = { Text("분", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(0.65f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = manualExKcal,
                        onValueChange = { manualExKcal = it },
                        label = { Text("kcal", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val min = manualExMin.toIntOrNull() ?: 30
                            val kcal = manualExKcal.toIntOrNull() ?: 0
                            if (manualExTitle.isNotBlank() && kcal > 0) {
                                onAddManualExercise(manualExTitle.trim(), min, kcal)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text("추가", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteRecordButton(onClick: () -> Unit) {
    Surface(
        color = StatusRedBg,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .border(1.dp, StatusRedBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "삭제",
                tint = StatusRedText,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "삭제",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StatusRedText,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DrinkPresetChip(
    preset: AlcoholPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White else Color(0xFF334155)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = preset.labelKo,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun QuickVolumeTile(
    topLabel: String,
    mlValue: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = topLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "${mlValue}ml",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
