package com.minsoo.slimtrack.data.ai

import android.util.Base64
import com.minsoo.slimtrack.data.db.FoodItemEntry
import com.minsoo.slimtrack.data.prefs.Gemini3Catalog
import com.minsoo.slimtrack.data.prefs.GeminiModelOption
import com.minsoo.slimtrack.domain.AdviceVerdict
import com.minsoo.slimtrack.domain.BmrCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class MealAnalysisResponse(
    val items: List<MealItemDto> = emptyList(),
    @SerialName("total_kcal") val totalKcal: Int = 0,
    val confidence: Float = 0.8f,
    val notes: String = ""
) {
    fun toFoodItemEntries(): List<FoodItemEntry> = items.map {
        FoodItemEntry(
            name = it.name,
            portion = it.portion,
            kcal = it.kcal,
            proteinGrams = it.proteinGrams,
            carbsGrams = it.carbsGrams,
            fatGrams = it.fatGrams
        )
    }
}

@Serializable
data class MealItemDto(
    val name: String,
    val portion: String = "1인분",
    val kcal: Int,
    @SerialName("protein_g") val proteinGrams: Float = 0f,
    @SerialName("carbs_g") val carbsGrams: Float = 0f,
    @SerialName("fat_g") val fatGrams: Float = 0f
)

@Serializable
data class InBodyOcrResponse(
    @SerialName("measured_at") val measuredAtRaw: String = "",
    val device: String = "인바디570",
    @SerialName("weight_kg") val weightKg: Float,
    @SerialName("skeletal_muscle_kg") val skeletalMuscleKg: Float,
    @SerialName("body_fat_mass_kg") val bodyFatMassKg: Float,
    @SerialName("body_fat_pct") val bodyFatPct: Float,
    @SerialName("ecw_ratio") val ecwRatio: Float = 0.374f,
    val bmi: Float,
    @SerialName("visceral_fat_level") val visceralFatLevel: Int,
    @SerialName("bmr_reported") val bmrReported: Int? = null
) {
    val bmrCalculated: Int
        get() = BmrCalculator.inbodyBmr(weightKg.toDouble(), bodyFatMassKg.toDouble())

    fun normalizedMeasuredAtIso(): String = normalizeInBodyTimestamp(measuredAtRaw)
    fun normalizedDateIso(): String = normalizedMeasuredAtIso().substringBefore("T")

    companion object {
        fun normalizeInBodyTimestamp(raw: String): String {
            val trimmed = raw.trim()
            val isoRegex = Regex("""(\d{4})[-.](\d{2})[-.](\d{2})[T\s]+(\d{2}):(\d{2})""")
            isoRegex.find(trimmed)?.let { m ->
                val (y, mo, d, h, mi) = m.destructured
                return "$y-$mo-$d" + "T" + "$h:$mi"
            }
            val shortRegex = Regex("""(\d{2})\.(\d{2})\.(\d{2})(?:\([^)]*\))?\s*(\d{2}):(\d{2})""")
            shortRegex.find(trimmed)?.let { m ->
                val (yy, mo, d, h, mi) = m.destructured
                return "20$yy-$mo-$d" + "T" + "$h:$mi"
            }
            val dateOnly = Regex("""(\d{2,4})\.(\d{2})\.(\d{2})""").find(trimmed)
            if (dateOnly != null) {
                val (yRaw, mo, d) = dateOnly.destructured
                val y = if (yRaw.length == 2) "20$yRaw" else yRaw
                return "$y-$mo-${d}T09:00"
            }
            return "2026-11-07T11:44"
        }
    }
}

@Serializable
data class GeminiCoachAdviceDto(
    val headline: String,
    val body: String,
    @SerialName("actions_tomorrow") val actionsTomorrow: List<String> = emptyList()
)

class GeminiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://generativelanguage.googleapis.com"
) {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Queries GET /v1beta/models?key=... and returns all available Gemini 3.x models
     * that support generateContent. Falls back to Gemini3Catalog.presetModels if none are returned.
     */
    suspend fun fetchAvailableGemini3Models(apiKey: String): Result<List<GeminiModelOption>> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("먼저 Gemini API 키를 입력해주세요."))
            }
            runCatching {
                val url = "${baseUrl.trimEnd('/')}/v1beta/models?key=${apiKey.trim()}"
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { response ->
                    val respBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IOException("모델 목록 조회 실패 (HTTP ${response.code}): ${respBody.take(160)}")
                    }
                    parseGemini3ModelsFromListResponse(respBody)
                }
            }
        }

    fun parseGemini3ModelsFromListResponse(rawJson: String): List<GeminiModelOption> {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val modelsArr = root["models"]?.jsonArray ?: return Gemini3Catalog.presetModels
        val discovered = mutableListOf<GeminiModelOption>()

        for (elem in modelsArr) {
            val obj = elem.jsonObject
            val rawName = obj["name"]?.jsonPrimitive?.content.orEmpty()
            val id = rawName.removePrefix("models/")
            if (!id.startsWith("gemini-3")) continue

            val methods = obj["supportedGenerationMethods"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.content }
                .orEmpty()
            if (methods.isNotEmpty() && "generateContent" !in methods) continue

            val displayName = obj["displayName"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: id
            val description = obj["description"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?: "API에서 확인된 사용 가능 Gemini 3.x 모델"
            val badge = when {
                id.contains("flash-lite") -> "3.x Lite"
                id.contains("flash") -> "3.x Flash"
                id.contains("pro") -> "3.x Pro"
                else -> "Gemini 3.x"
            }
            discovered += GeminiModelOption(
                id = id,
                titleKo = displayName,
                badgeKo = badge,
                descKo = description.take(60)
            )
        }

        if (discovered.isEmpty()) return Gemini3Catalog.presetModels
        // Sort so flash models come first (fastest for meal/OCR), then pro
        return discovered.sortedWith(
            compareBy<GeminiModelOption> {
                when {
                    it.id == "gemini-3-flash-preview" -> 0
                    it.id.contains("flash") && !it.id.contains("lite") -> 1
                    it.id.contains("pro") -> 2
                    else -> 3
                }
            }.thenBy { it.id }
        )
    }

    suspend fun analyzeMealPhoto(
        apiKey: String,
        model: String,
        jpegBytes: ByteArray,
        mealTypeLabelKo: String
    ): Result<MealAnalysisResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API 키가 설정되지 않았습니다. 설정 탭에서 API 키를 입력해주세요."))
        }
        runCatching {
            val base64Image = encodeBase64(jpegBytes)
            val prompt = """
                당신은 한국 음식 및 안주 전문 영양사입니다.
                제공된 사진(${mealTypeLabelKo})에 있는 모든 음식과 음료를 식별하고, 1인분(또는 사진에 보이는 실제 분량) 기준으로 칼로리(kcal)와 탄수화물/단백질/지방(g)을 추정하세요.
                반드시 아래 JSON 형식으로만 응답하세요 (마크다운 코드블록 없이 순수 JSON):
                {"items":[{"name":"음식명","portion":"분량","kcal":450,"protein_g":25.0,"carbs_g":30.0,"fat_g":18.0}],"total_kcal":450,"confidence":0.85,"notes":"참고 설명"}
            """.trimIndent()

            val responseText = callGenerateContentWith3xFallback(
                apiKey = apiKey,
                preferredModel = model,
                prompt = prompt,
                base64Jpeg = base64Image
            )
            parseMealAnalysisJson(responseText)
        }
    }

    suspend fun analyzeInBodyScreenshot(
        apiKey: String,
        model: String,
        imageBytes: ByteArray,
        mimeType: String = "image/png"
    ): Result<InBodyOcrResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API 키가 설정되지 않았습니다. 설정 탭에서 API 키를 입력해주세요."))
        }
        runCatching {
            val base64Image = encodeBase64(imageBytes)
            val prompt = """
                제공된 InBody 앱 결과 화면('인바디결과관리') 또는 결과지 스크린샷에서 수치를 정확히 추출하세요.
                오른쪽의 막대 그래프(표준이하/표준/표준이상, 낮음/높음)는 무시하고 왼쪽에 적힌 실제 측정 숫자만 읽으세요.
                추출 항목:
                - measured_at: 상단 측정일시 (예: "26.11.07(토) 11:44")
                - device: 기기명 (예: "인바디570")
                - weight_kg: 체중 (kg)
                - skeletal_muscle_kg: 골격근량 (kg)
                - body_fat_mass_kg: 체지방량 (kg)
                - body_fat_pct: 체지방률 (%)
                - ecw_ratio: 세포외수분비 (예: 0.374)
                - bmi: BMI (kg/m²)
                - visceral_fat_level: 내장지방레벨 (정수)
                - bmr_reported: 화면에 기초대사량이 적혀 있으면 정수, 없으면 null
                반드시 순수 JSON으로만 응답하세요:
                {"measured_at":"26.11.07(토) 11:44","device":"인바디570","weight_kg":98.9,"skeletal_muscle_kg":38.5,"body_fat_mass_kg":31.1,"body_fat_pct":31.4,"ecw_ratio":0.374,"bmi":31.9,"visceral_fat_level":12,"bmr_reported":null}
            """.trimIndent()

            val responseText = callGenerateContentWith3xFallback(
                apiKey = apiKey,
                preferredModel = model,
                prompt = prompt,
                base64Jpeg = base64Image,
                mimeType = mimeType
            )
            parseInBodyOcrJson(responseText)
        }
    }

    suspend fun generateStrictCoachAdvice(
        apiKey: String,
        model: String,
        verdict: AdviceVerdict
    ): Result<GeminiCoachAdviceDto> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key missing"))
        }
        runCatching {
            val verdictJsonStr = json.encodeToString(verdict)
            val toneGuide = when (verdict.strictness) {
                "VERY_STRICT" -> "변명을 절대 받아주지 말고 매우 엄격하고 직설적인 코치 말투로 지적하라. 칭찬은 생략하라."
                "NORMAL" -> "수치에 근거해 명확히 지적하되 실천 가능한 격려를 한 문장 덧붙여라."
                else -> "칭찬은 위반이 없을 때 한 문장만 허용한다. 변명을 받아주지 말고 숫자로만 냉정하게 코칭하라."
            }
            val prompt = """
                너는 개인 체중감량 전담 엄격 코치다. $toneGuide
                반드시 아래 판정 JSON의 숫자만 근거로 말하고, 없는 수치를 지어내지 마라.
                기초대사량(${verdict.bmrKcal}kcal) 미만의 극단적 절식은 절대 권하지 마라.
                응답은 순수 JSON으로만 출력하라:
                {"headline":"한 줄 핵심 판정","body":"3~6줄의 구체적 수치 지적 및 전략 코칭","actions_tomorrow":["내일 행동 1","내일 행동 2","내일 행동 3"]}
                판정 데이터:
                $verdictJsonStr
            """.trimIndent()

            val responseText = callGenerateContentWith3xFallback(
                apiKey = apiKey,
                preferredModel = model,
                prompt = prompt,
                base64Jpeg = null
            )
            parseCoachAdviceJson(responseText)
        }
    }

    fun parseMealAnalysisJson(rawText: String): MealAnalysisResponse {
        val clean = extractJsonObject(rawText)
        val parsed = json.decodeFromString<MealAnalysisResponse>(clean)
        val computedTotal = if (parsed.totalKcal > 0) parsed.totalKcal else parsed.items.sumOf { it.kcal }
        return parsed.copy(totalKcal = computedTotal)
    }

    fun parseInBodyOcrJson(rawText: String): InBodyOcrResponse {
        val clean = extractJsonObject(rawText)
        return json.decodeFromString<InBodyOcrResponse>(clean)
    }

    fun parseCoachAdviceJson(rawText: String): GeminiCoachAdviceDto {
        val clean = extractJsonObject(rawText)
        return json.decodeFromString<GeminiCoachAdviceDto>(clean)
    }

    private fun extractJsonObject(rawText: String): String {
        val stripped = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()
        val start = stripped.indexOf('{')
        val end = stripped.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return stripped.substring(start, end + 1)
        }
        throw IllegalArgumentException("유효한 JSON 응답을 찾을 수 없습니다: ${rawText.take(120)}")
    }

    /**
     * Calls generateContent with the user's selected Gemini 3.x model.
     * If the selected model returns HTTP 404 (e.g. preview alias changed), automatically
     * tries the remaining Gemini 3.x candidates so the user never gets stuck.
     */
    private fun callGenerateContentWith3xFallback(
        apiKey: String,
        preferredModel: String,
        prompt: String,
        base64Jpeg: String?,
        mimeType: String = "image/jpeg"
    ): String {
        val primary = Gemini3Catalog.ensureGemini3Model(preferredModel)
        val candidates = (listOf(primary) + Gemini3Catalog.presetModels.map { it.id }).distinct()
        var lastError: Throwable? = null

        for (candidateModel in candidates) {
            try {
                return callGenerateContent(
                    apiKey = apiKey,
                    model = candidateModel,
                    prompt = prompt,
                    base64Jpeg = base64Jpeg,
                    mimeType = mimeType
                )
            } catch (e: IOException) {
                lastError = e
                // Only fallback to next 3.x candidate if model was not found (404) or unsupported (400)
                if (e.message?.contains("HTTP 404") == true || e.message?.contains("not found") == true) {
                    continue
                } else {
                    throw e
                }
            }
        }
        throw lastError ?: IOException("사용 가능한 Gemini 3.x 모델을 찾을 수 없습니다.")
    }

    private fun callGenerateContent(
        apiKey: String,
        model: String,
        prompt: String,
        base64Jpeg: String?,
        mimeType: String = "image/jpeg"
    ): String {
        val partsArray = buildJsonArray {
            add(buildJsonObject { put("text", prompt) })
            if (base64Jpeg != null) {
                add(
                    buildJsonObject {
                        put(
                            "inline_data",
                            buildJsonObject {
                                put("mime_type", mimeType)
                                put("data", base64Jpeg)
                            }
                        )
                    }
                )
            }
        }

        val payload = buildJsonObject {
            put(
                "contents",
                buildJsonArray {
                    add(buildJsonObject { put("parts", partsArray) })
                }
            )
            put(
                "generationConfig",
                buildJsonObject {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                }
            )
        }

        val cleanModel = Gemini3Catalog.ensureGemini3Model(model)
        val url = "${baseUrl.trimEnd('/')}/v1beta/models/$cleanModel:generateContent?key=${apiKey.trim()}"
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        httpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Gemini API 오류 (HTTP ${response.code}, 모델 $cleanModel): ${respBody.take(200)}")
            }
            val root = json.parseToJsonElement(respBody).jsonObject
            val candidates = root["candidates"]?.jsonArray
                ?: throw IOException("Gemini 응답에 candidates가 없습니다.")
            val firstContent = candidates.firstOrNull()?.jsonObject?.get("content")?.jsonObject
                ?: throw IOException("Gemini 응답 content가 비어 있습니다.")
            val text = firstContent["parts"]?.jsonArray
                ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: throw IOException("Gemini 응답 텍스트가 비어 있습니다.")
            return text
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return runCatching {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.getOrElse {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }
}
