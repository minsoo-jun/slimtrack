package com.minsoo.slimtrack.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class MealType(val labelKo: String, val emoji: String) {
    BREAKFAST("아침", "🌅"),
    LUNCH("점심", "☀️"),
    DINNER("저녁", "🌙"),
    SNACK("간식", "🍎"),
    BEVERAGE("음료", "☕"),
    ANJU("안주", "🍻")
}

data class DailyBeveragePreset(
    val id: String,
    val nameKo: String,
    val emoji: String,
    val portion: String,
    val kcal: Int,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatGrams: Float = 0f
) {
    fun toFoodItemEntry(): FoodItemEntry = FoodItemEntry(
        name = nameKo,
        portion = portion,
        kcal = kcal,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams
    )

    companion object {
        fun formatMacroGrams(grams: Float): String {
            val rounded1 = kotlin.math.round(grams * 10f) / 10f
            return if (rounded1 % 1f == 0f) {
                "${rounded1.toInt()}"
            } else {
                String.format(java.util.Locale.US, "%.1f", rounded1)
            }
        }

        val presets: List<DailyBeveragePreset> = listOf(
            DailyBeveragePreset("AMERICANO", "아메리카노(블랙커피)", "☕", "1잔(355ml)", 10, 0.5f, 2f, 0f),
            DailyBeveragePreset("OOLONG_TEA", "우롱차/녹차", "🍵", "1병/1잔(500ml)", 0, 0f, 0f, 0f),
            // User's exact protein drink bottle (375ml): 41 kcal, protein 7.9g, fat 0g, carbs 2.3g, BCAA 1000mg, Collagen 6000mg
            DailyBeveragePreset(
                id = "PROTEIN_SHAKE",
                nameKo = "프로틴 음료(375ml)",
                emoji = "🥤",
                portion = "1병(375ml · BCAA 1000mg · 콜라겐 6000mg)",
                kcal = 41,
                proteinGrams = 7.9f,
                carbsGrams = 2.3f,
                fatGrams = 0f
            ),
            DailyBeveragePreset("CAFE_LATTE", "카페라떼(무가당)", "🥛", "1잔(355ml)", 140, 7f, 10f, 7f),
            DailyBeveragePreset("ZERO_DRINK", "제로 탄산/이온음료", "🥤", "1캔(355ml)", 0, 0f, 0f, 0f),
            DailyBeveragePreset("SOY_MILK", "무가당 두유", "🫘", "1팩(190ml)", 95, 7f, 4f, 5f)
        )
    }
}

@Serializable
data class FoodItemEntry(
    val name: String,
    val portion: String = "1인분",
    val kcal: Int,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatGrams: Float = 0f
)

@Entity(
    tableName = "meals",
    indices = [Index(value = ["dateIso"])]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,          // YYYY-MM-DD
    val mealType: String,         // BREAKFAST, LUNCH, DINNER, SNACK, ANJU
    val photoUri: String = "",
    val itemsJson: String = "[]",
    val totalKcal: Int,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatGrams: Float = 0f,
    val confidence: Float = 1.0f,
    val notes: String = "",
    val userEdited: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "drinks",
    indices = [Index(value = ["dateIso"])]
)
data class DrinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,          // YYYY-MM-DD
    val kind: String,             // SOJU, BEER, MAKGEOLLI, WINE, WHISKY, HIGHBALL, CUSTOM
    val displayLabel: String,
    val volumeMl: Int,
    val abvPct: Float,
    val kcal: Int,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inbody_records",
    indices = [Index(value = ["measuredAtIso"], unique = true)]
)
data class InBodyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measuredAtIso: String,    // YYYY-MM-DDTHH:mm
    val dateIso: String,          // YYYY-MM-DD
    val device: String = "인바디570",
    val weightKg: Float,
    val skeletalMuscleKg: Float,
    val bodyFatMassKg: Float,
    val bodyFatPct: Float,
    val ecwRatio: Float = 0.374f,
    val bmi: Float,
    val visceralFatLevel: Int,
    val bmrCalculated: Int,       // Always 370 + 21.6 * (weightKg - bodyFatMassKg)
    val bmrReported: Int? = null, // Optional reported BMR
    val source: String = "OCR"    // HC, OCR, MANUAL, SEED
)

@Entity(
    tableName = "weight_logs",
    indices = [
        Index(value = ["dateIso"]),
        Index(value = ["hcRecordId"], unique = true)
    ]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measuredAtIso: String,    // YYYY-MM-DDTHH:mm:ss
    val dateIso: String,          // YYYY-MM-DD
    val weightKg: Float,
    val bodyFatPct: Float? = null,
    val source: String = "MANUAL", // MANUAL, HC
    val hcRecordId: String? = null
)

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["dateIso"])]
)
data class ExerciseEntity(
    @PrimaryKey val hcId: String,
    val dateIso: String,          // YYYY-MM-DD
    val exerciseType: String,
    val title: String,
    val durationMinutes: Int,
    val activeKcal: Int,
    val steps: Int = 0,
    val source: String = "HC"     // HC, MANUAL
)

@Entity(
    tableName = "advice_logs",
    indices = [Index(value = ["dateIso"])]
)
data class AdviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,          // YYYY-MM-DD
    val kind: String,             // DAILY, WEEKLY_SUNDAY
    val grade: String,            // A..F
    val headline: String,
    val bodyText: String,
    val actionsJson: String,
    val verdictJson: String,
    val generator: String = "RULE", // RULE, GEMINI
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
