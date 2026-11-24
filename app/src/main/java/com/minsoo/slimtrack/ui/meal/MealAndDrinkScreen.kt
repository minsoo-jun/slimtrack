package com.minsoo.slimtrack.ui.meal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.R
import com.minsoo.slimtrack.data.db.DailyBeveragePreset
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
import kotlin.math.roundToInt

private val itemJsonParser = Json { ignoreUnknownKeys = true }

@Composable
fun MealAndDrinkScreen(
    selectedDate: LocalDate,
    selectedMealType: MealType,
    isAnalyzingPhoto: Boolean,
    analysisStatusMessage: String?,
    todayMeals: List<MealEntity>,
    todayDrinks: List<DrinkEntity>,
    todayExercises: List<ExerciseEntity>,
    targetProteinGrams: Int = 108,
    onSelectMealType: (MealType) -> Unit,
    onTakePhotoClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onSearchAndAddFoodByName: (String) -> Unit,
    onAddQuickBeverage: (DailyBeveragePreset) -> Unit = {},
    onChangeMealType: (MealEntity, MealType) -> Unit = { _, _ -> },
    onDeleteMeal: (MealEntity) -> Unit,
    onAddDrinkWithCount: (AlcoholPreset, Int, Float, Float, String) -> Unit,
    onDeleteDrink: (DrinkEntity) -> Unit,
    onSyncHealthConnectExercises: () -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var foodQueryText by remember { mutableStateOf("") }

    var selectedDrinkPreset by remember { mutableStateOf(AlcoholPreset.SOJU) }
    var drinkUnitMode by remember { mutableStateOf("잔") } // "잔" or "병"
    var drinkUnitVolumeMl by remember { mutableStateOf("50") }
    var drinkAbvText by remember { mutableStateOf("16.5") }
    var drinkCountText by remember { mutableStateOf("3") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with KakaoTalk Emoticon ("맛점하세요")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.sticker_eat_well),
                contentDescription = "맛점하세요 스티커",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🍽 $selectedDate 식단 · 음주 · 운동",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "사진 촬영 또는 음식명 입력 시 Gemini가 칼로리를 자동 추가합니다.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 🥩 Daily Protein Progress & Breakdown Card
        val consumedProteinGrams = remember(todayMeals) {
            todayMeals.sumOf { it.proteinGrams.toDouble() }.toFloat()
        }
        val breakfastProtein = remember(todayMeals) {
            todayMeals.filter { it.mealType == MealType.BREAKFAST.name }.sumOf { it.proteinGrams.toDouble() }.toFloat()
        }
        val lunchProtein = remember(todayMeals) {
            todayMeals.filter { it.mealType == MealType.LUNCH.name }.sumOf { it.proteinGrams.toDouble() }.toFloat()
        }
        val dinnerProtein = remember(todayMeals) {
            todayMeals.filter { it.mealType == MealType.DINNER.name }.sumOf { it.proteinGrams.toDouble() }.toFloat()
        }
        val snackOrDrinkProtein = remember(todayMeals) {
            todayMeals.filter { it.mealType == MealType.SNACK.name || it.mealType == MealType.BEVERAGE.name }
                .sumOf { it.proteinGrams.toDouble() }.toFloat()
        }

        DailyProteinOverviewCard(
            consumedGrams = consumedProteinGrams,
            targetGrams = targetProteinGrams,
            breakfastGrams = breakfastProtein,
            lunchGrams = lunchProtein,
            dinnerGrams = dinnerProtein,
            otherGrams = snackOrDrinkProtein
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
                        text = "아직 저장된 식단이 없습니다. 아래에서 사진 촬영 또는 음식명 AI 검색으로 추가하세요.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "💡 식단 항목을 터치하면 세부 음식 및 영양성분(탄·단·지)이 아코디언으로 펼쳐집니다.",
                        fontSize = 10.5.sp,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    todayMeals.forEachIndexed { idx, meal ->
                        ExpandableMealAccordionCard(
                            meal = meal,
                            initiallyExpanded = idx == 0,
                            onDeleteMeal = { onDeleteMeal(meal) },
                            onChangeMealType = { newType -> onChangeMealType(meal, newType) }
                        )
                    }
                }
            }
        }

        // Section 1: Meal & Anju Photo OR Food Name AI Calorie Auto-Addition (Manual item list removed!)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🤖 식사 & 회식 안주 AI 칼로리 자동 추가",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )

                // 6 MealType chips in a 2x3 equal-width grid so "☕음료" and all labels never truncate at 155% font scale!
                val mealTypesRow1 = MealType.entries.take(3)
                val mealTypesRow2 = MealType.entries.drop(3)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(mealTypesRow1, mealTypesRow2).forEach { rowTypes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowTypes.forEach { type ->
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
                                        text = "${type.emoji} ${type.labelKo}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fg,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick 1-Tap Daily Beverage Bar (커피, 우롱차, 프로틴 음료, 라떼, 제로음료, 두유)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "☕ 일상 음료 원터치 빠른 기록 (커피 · 우롱차 · 프로틴 음료 등)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        val bevRow1 = DailyBeveragePreset.presets.take(3)
                        val bevRow2 = DailyBeveragePreset.presets.drop(3)
                        listOf(bevRow1, bevRow2).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowPresets.forEach { preset ->
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(7.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(7.dp))
                                            .clickable { onAddQuickBeverage(preset) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${preset.emoji} ${preset.nameKo}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val subLabel = if (preset.proteinGrams >= 1f) {
                                                "${preset.kcal}kcal·단${DailyBeveragePreset.formatMacroGrams(preset.proteinGrams)}g"
                                            } else {
                                                "${preset.kcal}kcal"
                                            }
                                            Text(
                                                text = subLabel,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = StatusGreenText,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Method 1: Photo Camera / Gallery -> Gemini Auto-Save
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
                            text = "${selectedMealType.labelKo} 사진 촬영",
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

                HorizontalDivider()

                // Method 2: Type Food Name -> Gemini Searches & Automatically Adds Calories!
                Text(
                    text = "🔍 음식명으로 Gemini 칼로리 자동 검색 & 추가",
                    fontSize = 12.5.sp,
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
                        value = foodQueryText,
                        onValueChange = { foodQueryText = it },
                        label = {
                            Text(
                                text = "음식·음료명 입력 (예: 김치찌개, 아메리카노, 프로틴 음료)",
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (foodQueryText.isNotBlank()) {
                                onSearchAndAddFoodByName(foodQueryText.trim())
                                foodQueryText = ""
                            }
                        },
                        enabled = !isAnalyzingPhoto && foodQueryText.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "AI 추가",
                            fontSize = 12.sp,
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
                            text = "Gemini 3.x가 음식 칼로리를 검색하여 자동 저장 중...",
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Section 2: Manual Alcohol Calculator (with Manual Glass/Bottle Count & "xxxml * 알콜도수 * 잔수/병수" notation!)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.sticker_sorry),
                        contentDescription = "음주 주의 스티커",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🍺 회식 수동 음주량 칼로리 계산기",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "표기법: 용량(ml) * 알콜도수(%) * 잔수/병수",
                            fontSize = 10.5.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
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
                                    drinkUnitVolumeMl = if (drinkUnitMode == "잔") {
                                        preset.unitVolumeMl.toString()
                                    } else {
                                        preset.bottleVolumeMl.toString()
                                    }
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
                                    drinkUnitVolumeMl = if (drinkUnitMode == "잔") {
                                        preset.unitVolumeMl.toString()
                                    } else {
                                        preset.bottleVolumeMl.toString()
                                    }
                                    drinkAbvText = preset.defaultAbvPct.toString()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Quick Preset Tiles: sets Unit Mode ("잔" or "병"), Unit Volume (ml), and Count!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickVolumeTile(
                        topLabel = "1잔",
                        mlValue = selectedDrinkPreset.unitVolumeMl,
                        selected = drinkUnitMode == "잔" && drinkCountText == "1",
                        onClick = {
                            drinkUnitMode = "잔"
                            drinkUnitVolumeMl = selectedDrinkPreset.unitVolumeMl.toString()
                            drinkCountText = "1"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickVolumeTile(
                        topLabel = "3잔",
                        mlValue = selectedDrinkPreset.unitVolumeMl,
                        selected = drinkUnitMode == "잔" && drinkCountText == "3",
                        onClick = {
                            drinkUnitMode = "잔"
                            drinkUnitVolumeMl = selectedDrinkPreset.unitVolumeMl.toString()
                            drinkCountText = "3"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickVolumeTile(
                        topLabel = "1병/캔",
                        mlValue = selectedDrinkPreset.bottleVolumeMl,
                        selected = drinkUnitMode == "병" && drinkCountText == "1",
                        onClick = {
                            drinkUnitMode = "병"
                            drinkUnitVolumeMl = selectedDrinkPreset.bottleVolumeMl.toString()
                            drinkCountText = "1"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickVolumeTile(
                        topLabel = "2병/캔",
                        mlValue = selectedDrinkPreset.bottleVolumeMl,
                        selected = drinkUnitMode == "병" && drinkCountText == "2",
                        onClick = {
                            drinkUnitMode = "병"
                            drinkUnitVolumeMl = selectedDrinkPreset.bottleVolumeMl.toString()
                            drinkCountText = "2"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unit Mode Toggle ("잔 단위" vs "병/캔 단위")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("잔" to "잔 단위 입력 (${selectedDrinkPreset.unitVolumeMl}ml)", "병" to "병/캔 단위 입력 (${selectedDrinkPreset.bottleVolumeMl}ml)").forEach { (mode, title) ->
                        val isSel = drinkUnitMode == mode
                        val bg = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        val fg = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF475569)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable {
                                    drinkUnitMode = mode
                                    drinkUnitVolumeMl = if (mode == "잔") {
                                        selectedDrinkPreset.unitVolumeMl.toString()
                                    } else {
                                        selectedDrinkPreset.bottleVolumeMl.toString()
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = fg,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // 3 Input Fields: 1잔/1병 용량(ml) * 알콜도수(%) * 잔수/병수(수동 입력!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = drinkUnitVolumeMl,
                        onValueChange = { drinkUnitVolumeMl = it },
                        label = { Text("1$drinkUnitMode 용량(ml)", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = drinkAbvText,
                        onValueChange = { drinkAbvText = it },
                        label = { Text("도수(%)", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(0.9f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = drinkCountText,
                        onValueChange = { drinkCountText = it },
                        label = { Text("${drinkUnitMode}수 입력", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(0.9f),
                        singleLine = true
                    )
                }

                val unitMl = drinkUnitVolumeMl.toIntOrNull() ?: selectedDrinkPreset.unitVolumeMl
                val abv = drinkAbvText.toDoubleOrNull() ?: selectedDrinkPreset.defaultAbvPct
                val countVal = drinkCountText.toFloatOrNull()?.coerceAtLeast(0f) ?: 1f
                val totalVolMl = (unitMl * countVal).toInt()
                val previewDrinkKcal = AlcoholCalculator.calculatePresetKcal(selectedDrinkPreset, totalVolMl, abv)
                val countFormatted = if (countVal % 1f == 0f) "${countVal.toInt()}" else String.format("%.1f", countVal)

                // Exact Notation: "xxxml * 알콜도수 * 잔수/병수 = kcal"
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
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedDrinkPreset.labelKo}: ${unitMl}ml * ${abv}% * ${countFormatted}${drinkUnitMode}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = StatusRedText,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "총 ${totalVolMl}ml = ${previewDrinkKcal} kcal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRedText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (unitMl > 0 && countVal > 0f) {
                                    onAddDrinkWithCount(
                                        selectedDrinkPreset,
                                        unitMl,
                                        abv.toFloat(),
                                        countVal,
                                        drinkUnitMode
                                    )
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
                        val formattedLine = if (d.displayLabel.contains("*")) {
                            "${d.displayLabel} = ${d.kcal}kcal"
                        } else {
                            "${d.displayLabel}: ${d.volumeMl}ml * ${d.abvPct}% * 1회 = ${d.kcal}kcal"
                        }
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
                                    text = formattedLine,
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

        // Section 3: Health Connect Exercise List & Steps (Method A — Synced from Google Health only!)
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
                        Image(
                            painter = painterResource(id = R.drawable.sticker_workout_hot),
                            contentDescription = "운동 스티커",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "🏃 Health 운동 & 도보 동기화 (방식 A)",
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
                        Text("Health 동기화", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
                Text(
                    text = "※ 운동과 도보수는 Google Health 동기화로만 작동합니다 (기본 4,000보 초과분 칼로리 합산).",
                    fontSize = 10.5.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                if (todayExercises.isEmpty()) {
                    Text(
                        text = "동기화된 운동·걸음수가 없습니다. [Health 동기화]를 눌러 가져오세요.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    todayExercises.forEach { ex ->
                        val itemText = if (ex.source == "HC_STEPS") {
                            val bonus = (ex.steps - 4000).coerceAtLeast(0)
                            "👟 [Health] 도보 ${ex.steps}보(초과 ${bonus}보) · +${ex.activeKcal} kcal"
                        } else {
                            "🏃 [${ex.source}] ${ex.title} (${ex.durationMinutes}분) · +${ex.activeKcal} kcal"
                        }
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
                                    text = itemText,
                                    fontSize = 11.5.sp,
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
            }
        }
    }
}

@Composable
fun ExpandableMealAccordionCard(
    meal: MealEntity,
    initiallyExpanded: Boolean = false,
    onDeleteMeal: () -> Unit,
    onChangeMealType: (MealType) -> Unit = {}
) {
    var expanded by remember(meal.id) { mutableStateOf(initiallyExpanded) }
    val mealTypeEnum = runCatching { MealType.valueOf(meal.mealType) }.getOrNull()
    val typeTitle = mealTypeEnum?.let { "${it.emoji} ${it.labelKo}" } ?: meal.mealType
    val parsedItems = remember(meal.itemsJson) {
        runCatching { itemJsonParser.decodeFromString<List<FoodItemEntry>>(meal.itemsJson) }
            .getOrDefault(emptyList())
    }
    val summaryLine = if (parsedItems.isNotEmpty()) {
        parsedItems.joinToString(", ") { "${it.name}(${it.kcal}kcal)" }
    } else {
        meal.notes
    }
    val dietAdviceText = remember(meal.id, meal.mealType, meal.totalKcal, meal.proteinGrams, meal.carbsGrams, meal.fatGrams, meal.notes) {
        buildDietAdviceForMeal(meal, mealTypeEnum, parsedItems)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Accordion Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "[$typeTitle] ${meal.totalKcal} kcal (단백질 ${DailyBeveragePreset.formatMacroGrams(meal.proteinGrams)}g)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "접기" else "상세 펼치기",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (!expanded && summaryLine.isNotBlank()) {
                        Text(
                            text = summaryLine,
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!expanded && dietAdviceText.isNotBlank()) {
                        Text(
                            text = "💡 $dietAdviceText",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F766E),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                DeleteRecordButton(onClick = onDeleteMeal)
            }

            // Accordion Expanded Detail Section
            AnimatedVisibility(visible = expanded) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Change Row
                        Text(
                            text = "🏷️ 식단 카테고리 변경 (현재: $typeTitle)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MealType.entries.forEach { type ->
                                val isSelected = meal.mealType == type.name
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!isSelected) {
                                                onChangeMealType(type)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "${type.emoji} ${type.labelKo}",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Macro nutrient summary badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroBadgeChip("열량", "${meal.totalKcal}kcal", MaterialTheme.colorScheme.primary)
                            MacroBadgeChip("단백질", "${DailyBeveragePreset.formatMacroGrams(meal.proteinGrams)}g", StatusGreenText)
                            MacroBadgeChip("탄수화물", "${DailyBeveragePreset.formatMacroGrams(meal.carbsGrams)}g", Color(0xFF0284C7))
                            MacroBadgeChip("지방", "${DailyBeveragePreset.formatMacroGrams(meal.fatGrams)}g", Color(0xFFD97706))
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Text(
                            text = "📋 상세 음식 품목 (${if (parsedItems.isNotEmpty()) parsedItems.size else 1}개)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )

                        if (parsedItems.isNotEmpty()) {
                            parsedItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${item.name} (${item.portion})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${item.kcal}kcal (단${DailyBeveragePreset.formatMacroGrams(item.proteinGrams)}·탄${DailyBeveragePreset.formatMacroGrams(item.carbsGrams)}·지${DailyBeveragePreset.formatMacroGrams(item.fatGrams)}g)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "• ${meal.notes.ifBlank { typeTitle }} — ${meal.totalKcal} kcal",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // Highlighted Diet Coach Advice Box inside Expanded Meal Card!
                        Surface(
                            color = Color(0xFFF0FDFA),
                            shape = RoundedCornerShape(7.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF14B8A6), RoundedCornerShape(7.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "🩺 다이어트 관점 식단 어드바이스",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F766E),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = dietAdviceText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF134E4A),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildDietAdviceForMeal(
    meal: MealEntity,
    mealTypeEnum: MealType?,
    parsedItems: List<FoodItemEntry>
): String {
    val rawNotes = meal.notes.trim()
    // If Gemini already stored a custom coaching sentence in notes (not just "음식명(kcal)" list), use it!
    val looksLikeAdviceSentence = rawNotes.length >= 22 &&
        !rawNotes.endsWith("kcal)") &&
        (rawNotes.contains("단백질") || rawNotes.contains("탄수화물") || rawNotes.contains("칼로리") ||
            rawNotes.contains("섭취") || rawNotes.contains("줄이") || rawNotes.contains("좋") ||
            rawNotes.contains("권장") || rawNotes.contains("저녁") || rawNotes.contains("점심") ||
            rawNotes.contains("유산소") || rawNotes.contains("조절"))
    if (looksLikeAdviceSentence) {
        return rawNotes
    }

    val kcal = meal.totalKcal
    val p = meal.proteinGrams
    val c = meal.carbsGrams
    val f = meal.fatGrams

    return when (mealTypeEnum) {
        MealType.BREAKFAST -> when {
            kcal > 650 -> "아침 열량(${kcal}kcal)이 일일 권장량(1,834kcal) 대비 다소 높습니다. 점심·저녁은 정제 탄수화물을 줄이고 채소·단백질 위주로 각 550kcal 이내로 조절하세요."
            p < 20f -> "아침 칼로리(${kcal}kcal)는 적정하나 단백질(${DailyBeveragePreset.formatMacroGrams(p)}g)이 부족합니다. 골격근(38.5kg) 유지를 위해 삶은 계란이나 프로틴 음료(7.9g)를 곁들이면 공복 혈당 상승도 막을 수 있습니다."
            else -> "좋은 아침 식단(${kcal}kcal · 단백질 ${DailyBeveragePreset.formatMacroGrams(p)}g)입니다! 아침 단백질 섭취가 대사를 깨우고 점심 과식을 예방합니다."
        }
        MealType.LUNCH -> when {
            kcal > 850 && p >= 35f -> "단백질(${DailyBeveragePreset.formatMacroGrams(p)}g) 보충은 매우 훌륭하나 점심 열량(${kcal}kcal)이 하루 예산(1,834kcal)의 ${((kcal * 100f) / 1834f).toInt()}%를 차지합니다. 저녁은 500kcal 이내의 저탄수·채소 식단으로 맞추세요."
            kcal > 850 -> "점심 열량(${kcal}kcal)과 탄수화물·지방 비중이 높습니다. 오후에 우롱차/물을 충분히 마시고, 저녁은 탄수화물을 절반 이하로 줄여 일일 적자 목표를 방어하세요."
            p < 25f -> "점심 칼로리(${kcal}kcal)는 무난하나 단백질(${DailyBeveragePreset.formatMacroGrams(p)}g)이 다소 적습니다. 저녁에는 닭가슴살·생선·두부 등 단백질 35g 이상을 꼭 보충하세요."
            else -> "점심 칼로리(${kcal}kcal)와 단백질(${DailyBeveragePreset.formatMacroGrams(p)}g) 밸런스가 이상적입니다. 저녁까지 간식을 피하고 이 페이스를 유지하세요!"
        }
        MealType.DINNER -> when {
            kcal > 700 -> "저녁 열량(${kcal}kcal)이 높아 야간 체지방·내장지방(현재 12Lv) 축적 위험이 있습니다. 식후 30분 뒤 가벼운 걷기/산책으로 최소 150~200kcal를 추가 소모하세요."
            c > 65f -> "저녁 탄수화물(${DailyBeveragePreset.formatMacroGrams(c)}g) 비중이 다소 높습니다. 내장지방 10Lv 달성을 위해 저녁에는 밥·면류를 반 공기 이하로 줄이고 단백질·채소 비중을 높이세요."
            p >= 25f -> "저녁으로 아주 훌륭한 고단백·절제 식단(${kcal}kcal · 단백질 ${DailyBeveragePreset.formatMacroGrams(p)}g)입니다! 취침 전 야식만 피하면 내일 아침 체중 감량이 기대됩니다."
            else -> "저녁 열량(${kcal}kcal)이 깔끔하게 관리되었습니다. 수면 중 근손실 방지를 위해 다음에는 살코기·두부 등 단백질을 조금 더 곁들여 보세요."
        }
        MealType.BEVERAGE -> when {
            kcal == 0 -> "0kcal 무당 음료(차/블랙커피)로 칼로리 부담 없이 수분 보충과 지방 대사 촉진에 매우 좋습니다."
            p >= 5f && kcal <= 70 -> "저열량(${kcal}kcal)으로 단백질 ${DailyBeveragePreset.formatMacroGrams(p)}g(BCAA·콜라겐)을 보충하여 당류 부담 없이 골격근 방어에 최적입니다!"
            else -> "음료로 ${kcal}kcal(탄수화물 ${DailyBeveragePreset.formatMacroGrams(c)}g)를 섭취했습니다. 액상 칼로리는 포만감이 낮으므로 남은 식사에서 탄수화물을 그만큼 줄여주세요."
        }
        MealType.ANJU -> "회식 안주(${kcal}kcal · 지방 ${DailyBeveragePreset.formatMacroGrams(f)}g)는 알코올과 결합해 내장지방으로 직행하기 쉽습니다. 국물·튀김 대신 구이·숙회·채소 위주로 드시고 물을 술의 2배로 마시세요."
        else -> "간식(${kcal}kcal) 섭취 후에는 다음 본식사의 탄수화물 양을 조절하여 일일 권장 섭취량(1,834kcal)을 초과하지 않도록 관리하세요."
    }
}

@Composable
private fun MacroBadgeChip(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
        Text(text = value, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, softWrap = false)
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
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderCol = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)
    val bgCol = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        color = bgCol,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .border(if (selected) 1.5.dp else 1.dp, borderCol, RoundedCornerShape(8.dp))
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
                text = "1단위 ${mlValue}ml",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun DailyProteinOverviewCard(
    consumedGrams: Float,
    targetGrams: Int,
    breakfastGrams: Float,
    lunchGrams: Float,
    dinnerGrams: Float,
    otherGrams: Float
) {
    val consumedStr = DailyBeveragePreset.formatMacroGrams(consumedGrams)
    val target = targetGrams.coerceAtLeast(1)
    val pct = ((consumedGrams / target.toFloat()) * 100f).roundToInt()
    val remaining = (target.toFloat() - consumedGrams).coerceAtLeast(0f)
    val remainingStr = DailyBeveragePreset.formatMacroGrams(remaining)
    val isAchieved = consumedGrams >= target.toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🥩 오늘 단백질 섭취 현황",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = if (isAchieved) StatusGreenBg else Color(0xFFE0E7FF),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(
                            1.dp,
                            if (isAchieved) StatusGreenBorder else Color(0xFFA5B4FC),
                            RoundedCornerShape(4.dp)
                        )
                    ) {
                        Text(
                            text = if (isAchieved) "목표 달성 $pct%" else "$pct%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAchieved) StatusGreenText else Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "일일 권장 ${target}g",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }

            // Big Numbers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "현재 섭취량",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${consumedStr} g",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isAchieved) StatusGreenText else Color(0xFF0F172A)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isAchieved) "초과 달성량" else "목표까지 잔여량",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isAchieved) {
                            val overG = DailyBeveragePreset.formatMacroGrams(consumedGrams - target.toFloat())
                            "+$overG g"
                        } else {
                            "$remainingStr g"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAchieved) StatusGreenText else Color(0xFF2563EB)
                    )
                }
            }

            // Visual Progress Bar
            val ratio = (consumedGrams / target.toFloat()).coerceIn(0f, 1.25f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
            ) {
                drawRoundRect(
                    color = Color(0xFFE2E8F0),
                    size = size,
                    cornerRadius = CornerRadius(4.5f, 4.5f)
                )
                val fillW = (size.width * (ratio / 1.25f)).coerceAtMost(size.width)
                val barColor = when {
                    isAchieved -> Color(0xFF10B981)
                    pct >= 70 -> Color(0xFF0D9488)
                    pct >= 40 -> Color(0xFF6366F1)
                    else -> Color(0xFFF59E0B)
                }
                drawRoundRect(
                    color = barColor,
                    size = Size(fillW, size.height),
                    cornerRadius = CornerRadius(4.5f, 4.5f)
                )
                val markerX = size.width * (1.0f / 1.25f)
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(markerX, 0f),
                    end = Offset(markerX, size.height),
                    strokeWidth = 2.5f
                )
            }

            // Meal Breakdown Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ProteinMealChip("🍳 아침", breakfastGrams, Modifier.weight(1f))
                ProteinMealChip("🍚 점심", lunchGrams, Modifier.weight(1f))
                ProteinMealChip("🥗 저녁", dinnerGrams, Modifier.weight(1f))
                ProteinMealChip("🥤 음료·간식", otherGrams, Modifier.weight(1f))
            }

            // Actionable advice text
            Surface(
                color = if (isAchieved) StatusGreenBg else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isAchieved) StatusGreenBorder else Color(0xFFE2E8F0),
                        RoundedCornerShape(6.dp)
                    )
            ) {
                val tip = if (isAchieved) {
                    "💪 근손실 방지 일일 권장 단백질(${target}g) 충족 완료! 수분을 충분히 섭취해주세요."
                } else {
                    "💡 목표까지 ${remainingStr}g 부족 · 하단 빠른 음료에서 프로틴 음료(375ml) 추가 시 +7.9g 즉시 충전"
                }
                Text(
                    text = tip,
                    fontSize = 10.5.sp,
                    color = if (isAchieved) StatusGreenText else Color(0xFF475569),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun ProteinMealChip(
    title: String,
    grams: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "${DailyBeveragePreset.formatMacroGrams(grams)}g",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (grams > 0f) Color(0xFF0F172A) else Color(0xFF94A3B8),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
