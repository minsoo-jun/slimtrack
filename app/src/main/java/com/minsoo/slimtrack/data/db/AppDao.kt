package com.minsoo.slimtrack.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Meals
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Query("DELETE FROM meals WHERE dateIso < :startDateIso")
    suspend fun deleteMealsBefore(startDateIso: String)

    @Query("SELECT * FROM meals WHERE dateIso = :dateIso ORDER BY createdAtEpochMs ASC")
    fun observeMealsForDate(dateIso: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC, createdAtEpochMs ASC")
    fun observeMealsBetween(startIso: String, endIso: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC, createdAtEpochMs ASC")
    suspend fun getMealsBetween(startIso: String, endIso: String): List<MealEntity>

    @Query("SELECT COUNT(*) FROM meals WHERE dateIso = :dateIso")
    suspend fun countMealsForDate(dateIso: String): Int

    // Drinks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrink(drink: DrinkEntity): Long

    @Delete
    suspend fun deleteDrink(drink: DrinkEntity)

    @Query("DELETE FROM drinks WHERE id = :id")
    suspend fun deleteDrinkById(id: Long)

    @Query("DELETE FROM drinks WHERE dateIso < :startDateIso")
    suspend fun deleteDrinksBefore(startDateIso: String)

    @Query("SELECT * FROM drinks WHERE dateIso = :dateIso ORDER BY createdAtEpochMs ASC")
    fun observeDrinksForDate(dateIso: String): Flow<List<DrinkEntity>>

    @Query("SELECT * FROM drinks WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC, createdAtEpochMs ASC")
    fun observeDrinksBetween(startIso: String, endIso: String): Flow<List<DrinkEntity>>

    @Query("SELECT * FROM drinks WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC, createdAtEpochMs ASC")
    suspend fun getDrinksBetween(startIso: String, endIso: String): List<DrinkEntity>

    // InBody
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInBody(record: InBodyEntity): Long

    @Delete
    suspend fun deleteInBody(record: InBodyEntity)

    @Query("SELECT * FROM inbody_records ORDER BY measuredAtIso DESC")
    fun observeAllInBody(): Flow<List<InBodyEntity>>

    @Query("SELECT * FROM inbody_records ORDER BY measuredAtIso DESC LIMIT 1")
    fun observeLatestInBody(): Flow<InBodyEntity?>

    @Query("SELECT * FROM inbody_records ORDER BY measuredAtIso DESC LIMIT 1")
    suspend fun getLatestInBody(): InBodyEntity?

    @Query("SELECT * FROM inbody_records WHERE measuredAtIso = :measuredAtIso LIMIT 1")
    suspend fun findInBodyByTimestamp(measuredAtIso: String): InBodyEntity?

    // Weight Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeightLog(log: WeightLogEntity): Long

    @Delete
    suspend fun deleteWeightLog(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE dateIso < :startDateIso")
    suspend fun deleteWeightLogsBefore(startDateIso: String)

    @Query("SELECT * FROM weight_logs ORDER BY measuredAtIso DESC")
    fun observeAllWeightLogs(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY measuredAtIso DESC")
    suspend fun getAllWeightLogs(): List<WeightLogEntity>

    @Query("SELECT * FROM weight_logs ORDER BY measuredAtIso DESC LIMIT 1")
    fun observeLatestWeightLog(): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_logs ORDER BY measuredAtIso DESC LIMIT 1")
    suspend fun getLatestWeightLog(): WeightLogEntity?

    @Query("SELECT * FROM weight_logs WHERE hcRecordId = :hcRecordId LIMIT 1")
    suspend fun findWeightLogByHcId(hcRecordId: String): WeightLogEntity?

    // Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(item: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(item: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE hcId = :hcId")
    suspend fun deleteExerciseByHcId(hcId: String)

    @Query("DELETE FROM exercises WHERE source IN ('HC', 'HC_STEPS', 'MANUAL') AND dateIso BETWEEN :startIso AND :endIso")
    suspend fun deleteHcExercisesBetween(startIso: String, endIso: String)

    @Query("DELETE FROM exercises WHERE dateIso < :startDateIso")
    suspend fun deleteExercisesBefore(startDateIso: String)

    @Query("SELECT * FROM exercises WHERE dateIso = :dateIso ORDER BY durationMinutes DESC")
    fun observeExercisesForDate(dateIso: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso DESC")
    fun observeExercisesBetween(startIso: String, endIso: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso DESC")
    suspend fun getExercisesBetween(startIso: String, endIso: String): List<ExerciseEntity>

    // Advice
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvice(advice: AdviceEntity): Long

    @Query("DELETE FROM advice_logs WHERE dateIso < :startDateIso")
    suspend fun deleteAdviceBefore(startDateIso: String)

    @Query("SELECT * FROM advice_logs ORDER BY createdAtEpochMs DESC")
    fun observeAllAdvice(): Flow<List<AdviceEntity>>

    @Query("SELECT * FROM advice_logs ORDER BY createdAtEpochMs DESC LIMIT 1")
    fun observeLatestAdvice(): Flow<AdviceEntity?>
}
