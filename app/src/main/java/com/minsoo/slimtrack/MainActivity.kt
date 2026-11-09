package com.minsoo.slimtrack

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import java.io.File

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
        super.onCreate(savedInstanceState)

        setContent {
            SlimTrackTheme {
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

    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.syncHealthConnect()
    }

    Scaffold(
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
                    onSyncHealthConnect = viewModel::syncHealthConnect,
                    onNavigateToMealTab = { activeTab = MainTab.MEAL },
                    onNavigateToCoachTab = { activeTab = MainTab.COACH },
                    onDeleteMeal = viewModel::deleteMeal,
                    onDeleteDrink = viewModel::deleteDrink,
                    onDeleteExercise = viewModel::deleteExercise,
                    modifier = Modifier.padding(innerPadding)
                )

                MainTab.MEAL -> MealAndDrinkScreen(
                    selectedDate = selectedDate,
                    selectedMealType = selectedMealType,
                    stagedItems = stagedItems,
                    stagedNotes = stagedNotes,
                    stagedConfidence = stagedConfidence,
                    isAnalyzingPhoto = isAnalyzingPhoto,
                    analysisStatusMessage = statusMessage,
                    todayMeals = selectedDateMeals,
                    todayDrinks = selectedDateDrinks,
                    todayExercises = selectedDateExercises,
                    onSelectMealType = viewModel::selectMealType,
                    onTakePhotoClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onPickPhotoClick = {
                        pickMealPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUpdateStagedItem = viewModel::updateStagedItem,
                    onRemoveStagedItem = viewModel::removeStagedItem,
                    onAddManualFoodItem = viewModel::addManualStagedItem,
                    onSaveMeal = viewModel::saveStagedMeal,
                    onDeleteMeal = viewModel::deleteMeal,
                    onAddDrink = viewModel::addDrink,
                    onDeleteDrink = viewModel::deleteDrink,
                    onSyncHealthConnectExercises = viewModel::syncHealthConnect,
                    onAddManualExercise = viewModel::addManualExercise,
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
                    onManualSyncHealthConnectWeight = viewModel::syncHealthConnect,
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
