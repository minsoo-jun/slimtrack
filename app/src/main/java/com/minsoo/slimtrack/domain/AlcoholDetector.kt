package com.minsoo.slimtrack.domain

import com.minsoo.slimtrack.data.db.FoodItemEntry
import java.util.Locale

data class ExtractedAlcohol(
    val originalItem: FoodItemEntry,
    val preset: AlcoholPreset,
    val volumeMl: Int,
    val abvPct: Float,
    val kcal: Int,
    val displayLabel: String
)

object AlcoholDetector {
    private val SOJU_KEYWORDS = listOf("소주", "soju", "참이슬", "처음처럼", "진로", "새로", "한라산", "좋은데이")
    private val BEER_KEYWORDS = listOf(
        "맥주", "생맥주", "캔맥주", "병맥주", "beer", "카스", "테라", "켈리", "클라우드",
        "칭따오", "칭타오", "아사히", "하이네켄", "버드와이저", "기네스", "스텔라", "호가든", "에일", "라거", "ipa"
    )
    private val MAKGEOLLI_KEYWORDS = listOf("막걸리", "동동주", "탁주", "전통주", "지평", "장수막걸리")
    private val WINE_KEYWORDS = listOf("와인", "wine", "레드와인", "화이트와인", "샴페인", "스파클링와인", "뱅쇼")
    private val WHISKY_KEYWORDS = listOf(
        "위스키", "whisky", "whiskey", "버번", "싱글몰트", "발렌타인", "조니워커", "잭다니엘",
        "보드카", "vodka", "고량주", "연태고량주", "이과두주", "사케", "청주", "진", "데킬라", "럼", "브랜디", "꼬냨"
    )
    private val HIGHBALL_KEYWORDS = listOf("하이볼", "highball", "소맥", "폭탄주", "칵테일", "cocktail", "진토닉", "모히또")
    private val GENERAL_ALCOHOL_KEYWORDS = listOf("주류", "알코올", "술")

    fun isAlcohol(item: FoodItemEntry): Boolean {
        val text = "${item.name} ${item.portion}".lowercase(Locale.KOREAN)
        return (SOJU_KEYWORDS + BEER_KEYWORDS + MAKGEOLLI_KEYWORDS + WINE_KEYWORDS + WHISKY_KEYWORDS + HIGHBALL_KEYWORDS + GENERAL_ALCOHOL_KEYWORDS)
            .any { kw -> text.contains(kw) }
    }

    fun isAlcohol(name: String, portion: String = ""): Boolean {
        return isAlcohol(FoodItemEntry(name = name, portion = portion, kcal = 0))
    }

    fun extract(item: FoodItemEntry): ExtractedAlcohol {
        val text = "${item.name} ${item.portion}".lowercase(Locale.KOREAN)
        val preset = when {
            SOJU_KEYWORDS.any { text.contains(it) } -> AlcoholPreset.SOJU
            BEER_KEYWORDS.any { text.contains(it) } -> AlcoholPreset.BEER
            MAKGEOLLI_KEYWORDS.any { text.contains(it) } -> AlcoholPreset.MAKGEOLLI
            WINE_KEYWORDS.any { text.contains(it) } -> AlcoholPreset.WINE
            WHISKY_KEYWORDS.any { text.contains(it) } -> AlcoholPreset.WHISKY
            HIGHBALL_KEYWORDS.any { text.contains(it) } -> AlcoholPreset.HIGHBALL
            else -> AlcoholPreset.CUSTOM
        }

        // Parse volume from portion string (e.g. "500ml", "360ml", "1병(360ml)", "1잔(50ml)")
        val mlMatch = Regex("(\\d+)\\s*(?:ml|미리)", RegexOption.IGNORE_CASE).find(item.portion)
            ?: Regex("(\\d+)\\s*(?:ml|미리)", RegexOption.IGNORE_CASE).find(item.name)
        val parsedMl = mlMatch?.groupValues?.get(1)?.toIntOrNull()

        val volumeMl = when {
            parsedMl != null && parsedMl in 10..5000 -> parsedMl
            item.portion.contains("병") -> preset.bottleVolumeMl
            item.portion.contains("캔") -> if (preset == AlcoholPreset.BEER) 500 else preset.unitVolumeMl
            item.portion.contains("잔") || item.portion.contains("사발") || item.portion.contains("샷") -> preset.unitVolumeMl
            else -> preset.bottleVolumeMl
        }

        val abvPct = preset.defaultAbvPct.toFloat()
        val calculatedKcal = AlcoholCalculator.calculatePresetKcal(preset, volumeMl, abvPct.toDouble())
        val kcal = if (item.kcal in 20..2500) item.kcal else calculatedKcal
        val formulaLabel = "${preset.labelKo} (${item.name}, ${item.portion}): ${volumeMl}ml * ${abvPct}%"

        return ExtractedAlcohol(
            originalItem = item,
            preset = preset,
            volumeMl = volumeMl,
            abvPct = abvPct,
            kcal = kcal,
            displayLabel = formulaLabel
        )
    }
}
