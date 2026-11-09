package com.minsoo.slimtrack.ui.meal

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minsoo.slimtrack.R
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
    isAnalyzingPhoto: Boolean,
    analysisStatusMessage: String?,
    todayMeals: List<MealEntity>,
    todayDrinks: List<DrinkEntity>,
    todayExercises: List<ExerciseEntity>,
    onSelectMealType: (MealType) -> Unit,
    onTakePhotoClick: () -> Unit,
    onPickPhotoClick: () -> Unit,
    onSearchAndAddFoodByName: (String) -> Unit,
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
                            onDeleteMeal = { onDeleteMeal(meal) }
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
                                text = "음식명 입력 (예: 김치찌개 정식, 돈까스)",
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
    onDeleteMeal: () -> Unit
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
                            text = "[$typeTitle] ${meal.totalKcal} kcal (단백질 ${meal.proteinGrams.toInt()}g)",
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
                        // Macro nutrient summary badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroBadgeChip("열량", "${meal.totalKcal}kcal", MaterialTheme.colorScheme.primary)
                            MacroBadgeChip("단백질", "${meal.proteinGrams.toInt()}g", StatusGreenText)
                            MacroBadgeChip("탄수화물", "${meal.carbsGrams.toInt()}g", Color(0xFF0284C7))
                            MacroBadgeChip("지방", "${meal.fatGrams.toInt()}g", Color(0xFFD97706))
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
                                        text = "${item.kcal}kcal (단${item.proteinGrams.toInt()}·탄${item.carbsGrams.toInt()}·지${item.fatGrams.toInt()}g)",
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

                        if (meal.notes.isNotBlank()) {
                            Text(
                                text = "💬 메모: ${meal.notes}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
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
