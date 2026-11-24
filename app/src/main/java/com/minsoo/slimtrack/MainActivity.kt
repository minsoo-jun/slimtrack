package com.minsoo.slimtrack

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.health.connect.client.PermissionController
import com.minsoo.slimtrack.ui.advice.CoachAdviceScreen
import com.minsoo.slimtrack.ui.body.BodyAndStageScreen
import com.minsoo.slimtrack.ui.home.HomeScreen
import com.minsoo.slimtrack.ui.meal.MealAndDrinkScreen
import com.minsoo.slimtrack.ui.settings.SettingsAndProfileScreen
import com.minsoo.slimtrack.ui.theme.SlimTrackTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Concise 2-character Korean labels so bottom NavigationBar NEVER wraps into 2 lines!
 */
enum class MainTab(val labelKo: String, val icon: ImageVector) {
    HOME("홈", Icons.Default.CalendarMonth),
    MEAL("식단", Icons.Default.Restaurant),
    BODY("신체", Icons.Default.MonitorWeight),
    COACH("코치", Icons.AutoMirrored.Filled.Assignment),
    SETTINGS("설정", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as SlimTrackApp
        MainViewModel.Factory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val dashboard by viewModel.dashboardState.collectAsState()
            val fontScalePct = dashboard?.settings?.fontScalePercent ?: 125
            SlimTrackTheme(fontScalePercent = fontScalePct) {
                SlimTrackRoot(
                    activity = this,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun SlimTrackRoot(
    activity: ComponentActivity,
    viewModel: MainViewModel
) {
    val app = activity.application as SlimTrackApp
    var activeTab by remember { mutableStateOf(MainTab.HOME) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val dashboard by viewModel.dashboardState.collectAsState()
    val yearMonth by viewModel.currentYearMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedMealType by viewModel.selectedMealType.collectAsState()
    val stagedItems by viewModel.stagedItems.collectAsState()
    val stagedNotes by viewModel.stagedNotes.collectAsState()
    val stagedConfidence by viewModel.stagedConfidence.collectAsState()
    val isAnalyzingPhoto by viewModel.isAnalyzingPhoto.collectAsState()
    val isAnalyzingInBody by viewModel.isAnalyzingInBody.collectAsState()
    val isGeneratingAiAdvice by viewModel.isGeneratingAiAdvice.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val weightSyncDiag by viewModel.weightSyncDiag.collectAsState()
    val availableGemini3Models by viewModel.availableGemini3Models.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()
    val modelFetchMessage by viewModel.modelFetchMessage.collectAsState()

    val selectedDateMeals by viewModel.selectedDateMeals.collectAsState()
    val selectedDateDrinks by viewModel.selectedDateDrinks.collectAsState()
    val selectedDateExercises by viewModel.selectedDateExercises.collectAsState()
    val allWeightLogs by viewModel.allWeightLogs.collectAsState()
    val allInBodyRecords by viewModel.allInBodyRecords.collectAsState()
    val allAdviceLogs by viewModel.allAdviceLogs.collectAsState()
    val apiKey by viewModel.apiKeyFlow.collectAsState()

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.analyzeMealPhotoFromUri(activity, uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoDir = File(activity.cacheDir, "photos").apply { mkdirs() }
            val file = File(photoDir, "meal_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                file
            )
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    val pickMealPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzeMealPhotoFromUri(activity, uri)
        }
    }

    val pickInBodyPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzeInBodyFromUri(activity, uri)
        }
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.syncHealthConnect()
    }

    val onTriggerHealthSync: () -> Unit = {
        scope.launch {
            val hasPerms = app.healthConnectRepository.hasWeightAndExercisePermissions()
            if (!hasPerms) {
                runCatching {
                    healthConnectPermissionLauncher.launch(app.healthConnectRepository.requiredPermissions)
                }.onFailure {
                    viewModel.syncHealthConnect()
                }
            } else {
                viewModel.syncHealthConnect()
            }
        }
    }

    Scaffold(
        topBar = {
            CurrentTimeTopBar()
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.labelKo) },
                        label = {
                            Text(
                                text = tab.labelKo,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        val dash = dashboard
        if (dash == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            when (activeTab) {
                MainTab.HOME -> HomeScreen(
                    dashboard = dash,
                    currentYearMonth = yearMonth,
                    selectedDate = selectedDate,
                    selectedDateMeals = selectedDateMeals,
                    selectedDateDrinks = selectedDateDrinks,
                    selectedDateExercises = selectedDateExercises,
                    syncMessage = statusMessage,
                    onPrevMonth = viewModel::prevMonth,
                    onNextMonth = viewModel::nextMonth,
                    onSelectDate = viewModel::selectDate,
                    onSyncHealthConnect = onTriggerHealthSync,
                    onNavigateToMealTab = { activeTab = MainTab.MEAL },
                    onNavigateToCoachTab = { activeTab = MainTab.COACH },
                    onDeleteMeal = viewModel::deleteMeal,
                    onDeleteDrink = viewModel::deleteDrink,
                    onDeleteExercise = viewModel::deleteExercise,
                    onChangeMealType = viewModel::changeMealType,
                    onSaveManualWeight = { wKg ->
                        viewModel.saveManualWeight(wKg, dash.today)
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                MainTab.MEAL -> MealAndDrinkScreen(
                    selectedDate = selectedDate,
                    selectedMealType = selectedMealType,
                    isAnalyzingPhoto = isAnalyzingPhoto,
                    analysisStatusMessage = statusMessage,
                    todayMeals = selectedDateMeals,
                    todayDrinks = selectedDateDrinks,
                    todayExercises = selectedDateExercises,
                    targetProteinGrams = dash.bodyStatus.targetProteinGrams,
                    onSelectMealType = viewModel::selectMealType,
                    onTakePhotoClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onPickPhotoClick = {
                        pickMealPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSearchAndAddFoodByName = viewModel::analyzeAndSaveMealByFoodName,
                    onAddQuickBeverage = viewModel::addQuickBeverage,
                    onChangeMealType = viewModel::changeMealType,
                    onDeleteMeal = viewModel::deleteMeal,
                    onAddDrinkWithCount = viewModel::addDrink,
                    onDeleteDrink = viewModel::deleteDrink,
                    onSyncHealthConnectExercises = onTriggerHealthSync,
                    onDeleteExercise = viewModel::deleteExercise,
                    modifier = Modifier.padding(innerPadding)
                )

                MainTab.BODY -> BodyAndStageScreen(
                    today = dash.today,
                    effectiveStartDate = dash.profile.goalStartDate,
                    bodyStatus = dash.bodyStatus,
                    weightLogs = allWeightLogs,
                    inBodyHistory = allInBodyRecords,
                    isAnalyzingInBody = isAnalyzingInBody,
                    syncStatusMessage = statusMessage,
                    weightSyncDiag = weightSyncDiag,
                    onManualSyncHealthConnectWeight = onTriggerHealthSync,
                    onSaveQuickTodayWeight = { wKg ->
                        viewModel.saveManualWeight(wKg, dash.today)
                    },
                    onPickInBodyScreenshot = {
                        pickInBodyPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSaveInBodyManualOrOcr = viewModel::saveInBodyManual,
                    modifier = Modifier.padding(innerPadding)
                )

                MainTab.COACH -> CoachAdviceScreen(
                    liveVerdict = dash.liveVerdict,
                    adviceHistory = allAdviceLogs,
                    isGeneratingAiAdvice = isGeneratingAiAdvice,
                    onGenerateAiAdviceNow = viewModel::generateCoachAdviceNow,
                    modifier = Modifier.padding(innerPadding)
                )

                MainTab.SETTINGS -> SettingsAndProfileScreen(
                    today = dash.today,
                    profile = dash.profile,
                    settings = dash.settings,
                    currentApiKey = apiKey,
                    availableGemini3Models = availableGemini3Models,
                    isFetchingModels = isFetchingModels,
                    modelFetchMessage = modelFetchMessage,
                    healthConnectStatus = app.healthConnectRepository.checkAvailability(),
                    onSaveProfile = viewModel::saveUserProfile,
                    onSaveSettings = viewModel::saveAppSettings,
                    onSaveApiKey = viewModel::saveGeminiApiKey,
                    onFetchAvailableGemini3Models = viewModel::fetchAvailableGemini3Models,
                    onRequestHealthPermissions = {
                        runCatching {
                            healthConnectPermissionLauncher.launch(
                                app.healthConnectRepository.requiredPermissions
                            )
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

/**
 * Live Top Bar displaying current local date, day of week, and real-time clock (ticks every second).
 */
@Composable
fun CurrentTimeTopBar() {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L)
        }
    }
    val dowKo = when (now.dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
    val dateTimeStr = "${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} ($dowKo) ${now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}"
    val zoneOffsetStr = ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()).id

    Surface(
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🕒 현재 시간",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "$dateTimeStr (UTC$zoneOffsetStr)",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
