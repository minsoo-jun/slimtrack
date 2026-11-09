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
    ANJU("안주", "🍻")
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
