package com.minsoo.slimtrack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minsoo.slimtrack.data.ai.InBodyOcrResponse
import com.minsoo.slimtrack.data.db.AdviceEntity
import com.minsoo.slimtrack.data.db.DrinkEntity
import com.minsoo.slimtrack.data.db.ExerciseEntity
import com.minsoo.slimtrack.data.db.FoodItemEntry
import com.minsoo.slimtrack.data.db.InBodyEntity
import com.minsoo.slimtrack.data.db.MealEntity
import com.minsoo.slimtrack.data.db.MealType
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.data.prefs.AppSettings
import com.minsoo.slimtrack.data.prefs.UserProfile
import com.minsoo.slimtrack.data.repo.DashboardState
import com.minsoo.slimtrack.data.repo.SlimTrackRepository
import com.minsoo.slimtrack.domain.AlcoholCalculator
import com.minsoo.slimtrack.domain.AlcoholPreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: SlimTrackRepository
) : ViewModel() {

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedMealType = MutableStateFlow(MealType.LUNCH)
    val selectedMealType: StateFlow<MealType> = _selectedMealType.asStateFlow()

    private val _stagedItems = MutableStateFlow<List<FoodItemEntry>>(emptyList())
    val stagedItems: StateFlow<List<FoodItemEntry>> = _stagedItems.asStateFlow()

    private val _stagedNotes = MutableStateFlow("")
    val stagedNotes: StateFlow<String> = _stagedNotes.asStateFlow()

    private val _stagedConfidence = MutableStateFlow(1.0f)
    val stagedConfidence: StateFlow<Float> = _stagedConfidence.asStateFlow()

    private val _stagedPhotoUri = MutableStateFlow("")

    private val _isAnalyzingPhoto = MutableStateFlow(false)
    val isAnalyzingPhoto: StateFlow<Boolean> = _isAnalyzingPhoto.asStateFlow()

    private val _isAnalyzingInBody = MutableStateFlow(false)
    val isAnalyzingInBody: StateFlow<Boolean> = _isAnalyzingInBody.asStateFlow()

    private val _isGeneratingAiAdvice = MutableStateFlow(false)
    val isGeneratingAiAdvice: StateFlow<Boolean> = _isGeneratingAiAdvice.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val dashboardState: StateFlow<DashboardState?> = _currentYearMonth
        .flatMapLatest { ym -> repository.observeDashboard(ym, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedDateMeals: StateFlow<List<MealEntity>> = _selectedDate
        .flatMapLatest { d -> repository.dao.observeMealsForDate(d.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateDrinks: StateFlow<List<DrinkEntity>> = _selectedDate
        .flatMapLatest { d -> repository.dao.observeDrinksForDate(d.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDateExercises: StateFlow<List<ExerciseEntity>> = _selectedDate
        .flatMapLatest { d -> repository.dao.observeExercisesForDate(d.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWeightLogs: StateFlow<List<WeightLogEntity>> = repository.dao.observeAllWeightLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInBodyRecords: StateFlow<List<InBodyEntity>> = repository.dao.observeAllInBody()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAdviceLogs: StateFlow<List<AdviceEntity>> = repository.dao.observeAllAdvice()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiKeyFlow: StateFlow<String> = repository.secretStore.apiKeyFlow

    fun prevMonth() {
        _currentYearMonth.value = _currentYearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentYearMonth.value = _currentYearMonth.value.plusMonths(1)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _currentYearMonth.value) {
            _currentYearMonth.value = YearMonth.from(date)
        }
    }

    fun selectMealType(type: MealType) {
        _selectedMealType.value = type
    }

    fun updateStagedItem(index: Int, updated: FoodItemEntry) {
        val list = _stagedItems.value.toMutableList()
        if (index in list.indices) {
            list[index] = updated
            _stagedItems.value = list
        }
    }

    fun removeStagedItem(index: Int) {
        val list = _stagedItems.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _stagedItems.value = list
        }
    }

    fun addManualStagedItem(item: FoodItemEntry) {
        _stagedItems.value = _stagedItems.value + item
    }

    fun saveStagedMeal() {
        val items = _stagedItems.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            val saved = repository.saveMealWithItems(
                date = _selectedDate.value,
                mealType = _selectedMealType.value,
                items = items,
                photoUri = _stagedPhotoUri.value,
                confidence = _stagedConfidence.value,
                notes = _stagedNotes.value,
                userEdited = true
            )
            _stagedItems.value = emptyList()
            _stagedNotes.value = ""
            _stagedPhotoUri.value = ""
            _statusMessage.value = "✅ [${_selectedMealType.value.labelKo}] ${saved.totalKcal}kcal 저장 완료"
        }
    }

    fun analyzeMealPhotoFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isAnalyzingPhoto.value = true
            _stagedPhotoUri.value = uri.toString()
            val jpegBytes = runCatching { downscaleImageToJpeg(context, uri) }.getOrNull()
            if (jpegBytes == null) {
                _isAnalyzingPhoto.value = false
                _statusMessage.value = "⚠️ 이미지를 읽을 수 없습니다."
                return@launch
            }
            val apiKey = repository.secretStore.getGeminiApiKey()
            val model = dashboardState.value?.settings?.geminiModel ?: "gemini-2.5-flash"
            val result = repository.geminiClient.analyzeMealPhoto(
                apiKey = apiKey,
                model = model,
                jpegBytes = jpegBytes,
                mealTypeLabelKo = _selectedMealType.value.labelKo
            )
            _isAnalyzingPhoto.value = false
            result.onSuccess { resp ->
                _stagedItems.value = resp.toFoodItemEntries()
                _stagedNotes.value = resp.notes
                _stagedConfidence.value = resp.confidence
                _statusMessage.value = "🤖 Gemini 분석 완료: ${resp.items.size}개 품목, 총 ${resp.totalKcal}kcal (확인 후 저장하세요)"
            }.onFailure { err ->
                _statusMessage.value = "⚠️ 사진 분석 실패: ${err.message}"
            }
        }
    }

    fun analyzeInBodyFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isAnalyzingInBody.value = true
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes == null) {
                _isAnalyzingInBody.value = false
                _statusMessage.value = "⚠️ InBody 스크린샷을 읽을 수 없습니다."
                return@launch
            }
            val apiKey = repository.secretStore.getGeminiApiKey()
            val model = dashboardState.value?.settings?.geminiModel ?: "gemini-2.5-flash"
            val res = repository.geminiClient.analyzeInBodyScreenshot(
                apiKey = apiKey,
                model = model,
                imageBytes = bytes,
                mimeType = "image/png"
            )
            _isAnalyzingInBody.value = false
            res.onSuccess { ocr ->
                val profile = dashboardState.value?.profile ?: UserProfile()
                val saved = repository.saveInBodyFromOcr(ocr, profile, source = "OCR")
                _statusMessage.value = "✅ InBody OCR 추출 및 저장 완료: ${saved.weightKg}kg, 내장지방 ${saved.visceralFatLevel}Lv, 공식 BMR ${saved.bmrCalculated}kcal"
            }.onFailure { err ->
                _statusMessage.value = "⚠️ InBody OCR 실패: ${err.message}"
            }
        }
    }

    fun saveInBodyManual(ocr: InBodyOcrResponse) {
        viewModelScope.launch {
            val profile = dashboardState.value?.profile ?: UserProfile()
            val saved = repository.saveInBodyFromOcr(ocr, profile, source = "MANUAL")
            _statusMessage.value = "✅ 최신 InBody 저장 완료 (${saved.measuredAtIso}): 체중 ${saved.weightKg}kg, 내장지방 ${saved.visceralFatLevel}Lv, BMR ${saved.bmrCalculated}kcal"
        }
    }

    fun addDrink(preset: AlcoholPreset, volumeMl: Int, abvPct: Float) {
        viewModelScope.launch {
            val kcal = AlcoholCalculator.calculatePresetKcal(preset, volumeMl, abvPct.toDouble())
            val entity = DrinkEntity(
                dateIso = _selectedDate.value.toString(),
                kind = preset.name,
                displayLabel = preset.labelKo,
                volumeMl = volumeMl,
                abvPct = abvPct,
                kcal = kcal
            )
            repository.dao.insertDrink(entity)
            _statusMessage.value = "🍺 ${preset.labelKo} ${volumeMl}ml (${kcal}kcal) 기록 추가됨"
        }
    }

    fun deleteMeal(meal: MealEntity) {
        viewModelScope.launch {
            repository.dao.deleteMeal(meal)
        }
    }

    fun deleteDrink(drink: DrinkEntity) {
        viewModelScope.launch {
            repository.dao.deleteDrink(drink)
        }
    }

    fun addManualWeight(date: LocalDate, weightKg: Float, bodyFatPct: Float? = null) {
        viewModelScope.launch {
            val saved = repository.addManualWeight(date, weightKg, bodyFatPct)
            _statusMessage.value = "⚖ ${saved.dateIso} 체중 ${saved.weightKg}kg 저장 완료"
        }
    }

    fun addManualExercise(title: String, durationMin: Int, activeKcal: Int) {
        viewModelScope.launch {
            val entity = ExerciseEntity(
                hcId = "manual-${UUID.randomUUID()}",
                dateIso = _selectedDate.value.toString(),
                exerciseType = title,
                title = title,
                durationMinutes = durationMin,
                activeKcal = activeKcal,
                source = "MANUAL"
            )
            repository.dao.upsertExercise(entity)
            _statusMessage.value = "🏃 운동 [$title] +${activeKcal}kcal 추가됨"
        }
    }

    fun syncHealthConnect() {
        viewModelScope.launch {
            val summary = repository.syncFromHealthConnect()
            _statusMessage.value = summary.messageKo
        }
    }

    fun generateCoachAdviceNow() {
        viewModelScope.launch {
            _isGeneratingAiAdvice.value = true
            val saved = repository.generateAndPersistAdvice(kind = "DAILY")
            _isGeneratingAiAdvice.value = false
            _statusMessage.value = "📋 코치 리포트 생성 완료 (${saved.generator}, 등급 ${saved.grade})"
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.profileStore.saveProfile(profile)
        }
    }

    fun saveAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.settingsStore.updateSettings(settings)
        }
    }

    fun saveGeminiApiKey(key: String) {
        repository.secretStore.setGeminiApiKey(key)
    }

    private fun downscaleImageToJpeg(context: Context, uri: Uri, maxDim: Int = 1024): ByteArray? {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return rawBytes
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val scale = minOf(1.0f, maxDim.toFloat() / maxOf(w, h))
        val scaled = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return out.toByteArray()
    }

    class Factory(private val repository: SlimTrackRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
