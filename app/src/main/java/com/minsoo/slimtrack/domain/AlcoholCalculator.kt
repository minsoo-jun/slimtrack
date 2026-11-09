package com.minsoo.slimtrack.domain

import kotlin.math.roundToInt

enum class AlcoholPreset(
    val labelKo: String,
    val defaultUnitLabelKo: String,
    val unitVolumeMl: Int,
    val bottleVolumeMl: Int,
    val defaultAbvPct: Double,
    val extraCarbKcalPer100Ml: Double
) {
    SOJU(
        labelKo = "소주",
        defaultUnitLabelKo = "잔(50ml) / 병(360ml)",
        unitVolumeMl = 50,
        bottleVolumeMl = 360,
        defaultAbvPct = 16.5,
        extraCarbKcalPer100Ml = 0.0
    ),
    BEER(
        labelKo = "맥주",
        defaultUnitLabelKo = "잔(200ml) / 캔(500ml)",
        unitVolumeMl = 200,
        bottleVolumeMl = 500,
        defaultAbvPct = 4.5,
        extraCarbKcalPer100Ml = 13.0
    ),
    MAKGEOLLI(
        labelKo = "막걸리",
        defaultUnitLabelKo = "사발(200ml) / 병(750ml)",
        unitVolumeMl = 200,
        bottleVolumeMl = 750,
        defaultAbvPct = 6.0,
        extraCarbKcalPer100Ml = 14.0
    ),
    WINE(
        labelKo = "와인",
        defaultUnitLabelKo = "잔(150ml) / 병(750ml)",
        unitVolumeMl = 150,
        bottleVolumeMl = 750,
        defaultAbvPct = 12.5,
        extraCarbKcalPer100Ml = 4.0
    ),
    WHISKY(
        labelKo = "위스키",
        defaultUnitLabelKo = "샷(30ml) / 더블(60ml)",
        unitVolumeMl = 30,
        bottleVolumeMl = 60,
        defaultAbvPct = 40.0,
        extraCarbKcalPer100Ml = 0.0
    ),
    HIGHBALL(
        labelKo = "하이볼",
        defaultUnitLabelKo = "잔(350ml) / 대잔(500ml)",
        unitVolumeMl = 350,
        bottleVolumeMl = 500,
        defaultAbvPct = 7.0,
        extraCarbKcalPer100Ml = 8.0
    ),
    CUSTOM(
        labelKo = "직접 입력",
        defaultUnitLabelKo = "ml / 도수(%)",
        unitVolumeMl = 100,
        bottleVolumeMl = 360,
        defaultAbvPct = 15.0,
        extraCarbKcalPer100Ml = 0.0
    )
}

object AlcoholCalculator {
    private const val ETHANOL_DENSITY_G_PER_ML = 0.789
    private const val KCAL_PER_GRAM_ALCOHOL = 7.0

    /**
     * Pure alcohol kcal = volumeMl * (abvPct / 100) * 0.789 * 7.0
     * Plus carbohydrate kcal from preset (e.g. beer/makgeolli/highball).
     * Example: Soju 360ml @ 16.5% -> 360 * 0.165 * 0.789 * 7 = 328 kcal.
     */
    fun calculateKcal(
        volumeMl: Int,
        abvPct: Double,
        extraCarbKcalPer100Ml: Double = 0.0
    ): Int {
        if (volumeMl <= 0 || abvPct < 0.0) return 0
        val alcoholGrams = volumeMl * (abvPct / 100.0) * ETHANOL_DENSITY_G_PER_ML
        val alcoholKcal = alcoholGrams * KCAL_PER_GRAM_ALCOHOL
        val carbKcal = (volumeMl / 100.0) * extraCarbKcalPer100Ml
        return (alcoholKcal + carbKcal).roundToInt()
    }

    fun calculatePresetKcal(
        preset: AlcoholPreset,
        volumeMl: Int,
        abvPctOverride: Double? = null
    ): Int {
        val abv = abvPctOverride ?: preset.defaultAbvPct
        return calculateKcal(volumeMl, abv, preset.extraCarbKcalPer100Ml)
    }
}
