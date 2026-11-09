package com.minsoo.slimtrack.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.minsoo.slimtrack.domain.BmrCalculator

@Database(
    entities = [
        MealEntity::class,
        DrinkEntity::class,
        InBodyEntity::class,
        WeightLogEntity::class,
        ExerciseEntity::class,
        AdviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "slimtrack.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }

        /**
         * Initial seed from the user's InBody 570 screenshot on 2026-11-07 11:44:
         * Weight 98.9kg, Skeletal Muscle 38.5kg, Body Fat Mass 31.1kg, Body Fat % 31.4%,
         * ECW Ratio 0.374, BMI 31.9, Visceral Fat Level 12, BMR = 370 + 21.6*(98.9 - 31.1) = 1834 kcal.
         */
        fun defaultSeedInBody(): InBodyEntity {
            val weight = 98.9f
            val fatMass = 31.1f
            val bmr = BmrCalculator.inbodyBmr(weight.toDouble(), fatMass.toDouble())
            return InBodyEntity(
                measuredAtIso = "2026-11-07T11:44",
                dateIso = "2026-11-07",
                device = "인바디570",
                weightKg = weight,
                skeletalMuscleKg = 38.5f,
                bodyFatMassKg = fatMass,
                bodyFatPct = 31.4f,
                ecwRatio = 0.374f,
                bmi = 31.9f,
                visceralFatLevel = 12,
                bmrCalculated = bmr,
                bmrReported = null,
                source = "SEED"
            )
        }
    }
}
