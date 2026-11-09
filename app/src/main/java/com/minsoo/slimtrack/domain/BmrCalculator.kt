package com.minsoo.slimtrack.domain

import kotlin.math.roundToInt

/**
 * InBody Katch-McArdle formula:
 * BMR = 370 + 21.6 * LeanBodyMass(kg)
 * where LeanBodyMass = weightKg - bodyFatMassKg
 */
object BmrCalculator {
    fun inbodyBmr(weightKg: Double, bodyFatMassKg: Double): Int {
        val leanMass = (weightKg - bodyFatMassKg).coerceAtLeast(20.0)
        return (370.0 + 21.6 * leanMass).roundToInt()
    }

    fun inbodyBmrFromPct(weightKg: Double, bodyFatPct: Double): Int {
        val fatMass = weightKg * (bodyFatPct.coerceIn(3.0, 65.0) / 100.0)
        return inbodyBmr(weightKg, fatMass)
    }
}
