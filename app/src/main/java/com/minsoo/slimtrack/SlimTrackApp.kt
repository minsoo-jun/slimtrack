package com.minsoo.slimtrack

import android.app.Application
import com.minsoo.slimtrack.data.ai.GeminiClient
import com.minsoo.slimtrack.data.db.AppDatabase
import com.minsoo.slimtrack.data.health.HealthConnectRepository
import com.minsoo.slimtrack.data.prefs.SecretStore
import com.minsoo.slimtrack.data.prefs.SettingsStore
import com.minsoo.slimtrack.data.prefs.UserProfileStore
import com.minsoo.slimtrack.data.repo.SlimTrackRepository
import com.minsoo.slimtrack.work.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SlimTrackApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val profileStore: UserProfileStore by lazy { UserProfileStore(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val secretStore: SecretStore by lazy { SecretStore(this) }
    val healthConnectRepository: HealthConnectRepository by lazy { HealthConnectRepository(this) }
    val geminiClient: GeminiClient by lazy { GeminiClient() }

    val repository: SlimTrackRepository by lazy {
        SlimTrackRepository(
            dao = database.dao(),
            profileStore = profileStore,
            settingsStore = settingsStore,
            secretStore = secretStore,
            healthConnectRepository = healthConnectRepository,
            geminiClient = geminiClient
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.ensureSeedInBodyExists()
        }
        runCatching {
            AlarmScheduler.scheduleAll(this)
        }
    }
}
