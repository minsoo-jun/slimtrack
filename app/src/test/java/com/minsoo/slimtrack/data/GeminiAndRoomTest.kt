package com.minsoo.slimtrack.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.minsoo.slimtrack.data.ai.GeminiClient
import com.minsoo.slimtrack.data.db.AppDatabase
import com.minsoo.slimtrack.data.db.WeightLogEntity
import com.minsoo.slimtrack.data.health.HealthConnectRepository
import com.minsoo.slimtrack.data.prefs.SecretStore
import com.minsoo.slimtrack.data.prefs.SettingsStore
import com.minsoo.slimtrack.data.prefs.UserProfile
import com.minsoo.slimtrack.data.prefs.UserProfileStore
import com.minsoo.slimtrack.data.repo.SlimTrackRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiAndRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SlimTrackRepository
    private lateinit var mockServer: MockWebServer

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mockServer = MockWebServer()
        mockServer.start()

        val geminiClient = GeminiClient(baseUrl = mockServer.url("/").toString())
        repository = SlimTrackRepository(
            dao = db.dao(),
            profileStore = UserProfileStore(context),
            settingsStore = SettingsStore(context),
            secretStore = SecretStore(context),
            healthConnectRepository = HealthConnectRepository(context),
            geminiClient = geminiClient
        )
    }

    @After
    fun tearDown() {
        db.close()
        mockServer.shutdown()
    }

    @Test
    fun `inbody sample screenshot fixture exists and OCR parser extracts exact sample metrics and formula BMR`() {
        val stream = javaClass.classLoader?.getResourceAsStream("inbody_sample.png")
        assertThat(stream).isNotNull()
        val bytes = stream!!.use { it.readBytes() }
        assertThat(bytes.size).isGreaterThan(10_000)

        val sampleJson = """
            {
              "measured_at": "26.11.07(토) 11:44",
              "device": "인바디570",
              "weight_kg": 98.9,
              "skeletal_muscle_kg": 38.5,
              "body_fat_mass_kg": 31.1,
              "body_fat_pct": 31.4,
              "ecw_ratio": 0.374,
              "bmi": 31.9,
              "visceral_fat_level": 12,
              "bmr_reported": null
            }
        """.trimIndent()

        val parsed = repository.geminiClient.parseInBodyOcrJson(sampleJson)
        assertThat(parsed.normalizedMeasuredAtIso()).isEqualTo("2026-11-07T11:44")
        assertThat(parsed.normalizedDateIso()).isEqualTo("2026-11-07")
        assertThat(parsed.weightKg).isWithin(0.01f).of(98.9f)
        assertThat(parsed.skeletalMuscleKg).isWithin(0.01f).of(38.5f)
        assertThat(parsed.bodyFatMassKg).isWithin(0.01f).of(31.1f)
        assertThat(parsed.bodyFatPct).isWithin(0.01f).of(31.4f)
        assertThat(parsed.ecwRatio).isWithin(0.001f).of(0.374f)
        assertThat(parsed.bmi).isWithin(0.01f).of(31.9f)
        assertThat(parsed.visceralFatLevel).isEqualTo(12)
        assertThat(parsed.bmrCalculated).isEqualTo(1834)
    }

    @Test
    fun `geminiClient analyzes meal photo via MockWebServer and handles HTTP errors`() = runBlocking {
        val geminiEnvelope = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\"items\":[{\"name\":\"김치찌개\",\"portion\":\"1인분\",\"kcal\":450,\"protein_g\":25,\"carbs_g\":20,\"fat_g\":28}],\"total_kcal\":450,\"confidence\":0.85,\"notes\":\"밥 별도\"}"
                  }]
                }
              }]
            }
        """.trimIndent()
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(geminiEnvelope))

        val ok = repository.geminiClient.analyzeMealPhoto(
            apiKey = "test-key",
            model = "gemini-2.5-flash",
            jpegBytes = ByteArray(32),
            mealTypeLabelKo = "점심"
        )
        assertThat(ok.isSuccess).isTrue()
        assertThat(ok.getOrThrow().totalKcal).isEqualTo(450)
        assertThat(ok.getOrThrow().items.first().name).isEqualTo("김치찌개")

        mockServer.enqueue(MockResponse().setResponseCode(429).setBody("Quota exceeded"))
        val err = repository.geminiClient.analyzeMealPhoto(
            apiKey = "test-key",
            model = "gemini-2.5-flash",
            jpegBytes = ByteArray(32),
            mealTypeLabelKo = "점심"
        )
        assertThat(err.isFailure).isTrue()
    }

    @Test
    fun `weight logs deduplicate by hcRecordId and resolve latest weight newer than InBody`() = runBlocking {
        repository.ensureSeedInBodyExists()

        // Insert Health Connect weight record twice with same hcRecordId
        val hcWeight = WeightLogEntity(
            measuredAtIso = "2026-11-08T07:30:00",
            dateIso = "2026-11-08",
            weightKg = 98.6f,
            source = "HC",
            hcRecordId = "hc-rec-001"
        )
        db.dao().upsertWeightLog(hcWeight)
        db.dao().upsertWeightLog(hcWeight.copy(weightKg = 98.5f))

        val allAfterHc = db.dao().getAllWeightLogs()
        assertThat(allAfterHc).hasSize(1)
        assertThat(allAfterHc.first().weightKg).isWithin(0.01f).of(98.5f)

        // Add manual weight on 2026-11-09 twice; latest should win
        repository.addManualWeight(
            date = LocalDate.of(2026, 11, 9),
            weightKg = 98.4f,
            now = LocalDateTime.of(2026, 11, 9, 7, 0, 0)
        )
        repository.addManualWeight(
            date = LocalDate.of(2026, 11, 9),
            weightKg = 98.2f,
            now = LocalDateTime.of(2026, 11, 9, 8, 0, 0)
        )

        val status = repository.resolveCurrentBodyStatus(
            latestInBody = db.dao().getLatestInBody(),
            latestWeightLog = db.dao().getLatestWeightLog(),
            profile = UserProfile(),
            today = LocalDate.of(2026, 11, 9)
        )
        assertThat(status.currentWeightKg).isWithin(0.01).of(98.2)
        assertThat(status.bmrKcal).isEqualTo(1834) // BMR always from latest InBody formula
    }
}
