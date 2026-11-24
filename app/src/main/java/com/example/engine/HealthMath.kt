package com.example.engine

import com.example.data.model.UnitSystem
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

object HealthMath {
    private const val KG_PER_LB = 0.45359237
    private const val CM_PER_INCH = 2.54

    fun kgToLbs(kg: Double): Double = round((kg / KG_PER_LB) * 10.0) / 10.0
    fun lbsToKg(lbs: Double): Double = round((lbs * KG_PER_LB) * 100.0) / 100.0

    fun cmToFtIn(cm: Double): Pair<Int, Int> {
        val totalInches = (cm / CM_PER_INCH).roundToInt()
        return (totalInches / 12) to (totalInches % 12)
    }

    fun ftInToCm(feet: Int, inches: Int): Double {
        val totalInches = max(0, feet) * 12 + max(0, inches)
        if (totalInches <= 0) return 0.0
        return round((totalInches * CM_PER_INCH) * 10.0) / 10.0
    }

    fun formatWeightDisplay(weightKg: Double?, unit: UnitSystem): String {
        if (weightKg == null || !weightKg.isFinite() || weightKg <= 0.0) return "--"
        return if (unit == UnitSystem.IMPERIAL) "${kgToLbs(weightKg)} lbs"
        else "${round(weightKg * 10.0) / 10.0} kg"
    }

    fun formatHeightDisplay(heightCm: Double?, unit: UnitSystem): String {
        if (heightCm == null || !heightCm.isFinite() || heightCm <= 0.0) return "--"
        return if (unit == UnitSystem.IMPERIAL) {
            val (f, i) = cmToFtIn(heightCm)
            "${f}'${i}\""
        } else {
            "${round(heightCm * 10.0) / 10.0} cm"
        }
    }

    fun safePercent(numerator: Double, denominator: Double): Int {
        if (!numerator.isFinite() || !denominator.isFinite() || denominator <= 0.0) return 0
        return ((numerator / denominator) * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun sanitizeSleepHours(raw: Double): Double {
        if (!raw.isFinite()) return 0.0
        val clamped = raw.coerceIn(0.0, 16.0)
        return round(clamped * 10.0) / 10.0
    }

    fun computeSleepDurationHours(bedTime24: String, wakeTime24: String): Double {
        fun parseMinutes(t: String): Int? {
            val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(t.trim()) ?: return null
            val hh = m.groupValues[1].toIntOrNull() ?: return null
            val mm = m.groupValues[2].toIntOrNull() ?: return null
            if (hh !in 0..23 || mm !in 0..59) return null
            return hh * 60 + mm
        }
        val bed = parseMinutes(bedTime24) ?: return 7.0
        val wake = parseMinutes(wakeTime24) ?: return 7.0
        var diffMin = wake - bed
        if (diffMin <= 0) diffMin += 24 * 60
        if (diffMin >= 24 * 60) diffMin -= 24 * 60
        val hrs = min(16.0, diffMin / 60.0)
        return round(hrs * 10.0) / 10.0
    }

    fun sleepHoursToQuality(hours: Double): Int {
        val h = sanitizeSleepHours(hours)
        return when {
            h < 4.5 -> 1
            h < 6.0 -> 2
            h < 6.8 -> 3
            h <= 8.5 -> 4
            else -> 5
        }
    }

    data class ReadinessBreakdown(
        val score: Int,
        val hydrationPct: Int,
        val stepsPct: Int,
        val sleepPct: Int,
        val adherencePct: Int,
        val hasScheduledAlarms: Boolean,
    )

    fun computeDailyReadinessScore(
        waterMl: Int,
        waterGoalMl: Int,
        steps: Int,
        stepGoal: Int,
        sleepHours: Double,
        activeAlarmsCount: Int,
        dueAlarmsCount: Int,
    ): ReadinessBreakdown {
        val hydrationPct = safePercent(max(0, waterMl).toDouble(), max(500, waterGoalMl).toDouble())
        val stepsPct = safePercent(max(0, steps).toDouble(), max(1000, stepGoal).toDouble())
        val s = sanitizeSleepHours(sleepHours)
        val sleepPct = when {
            s in 7.0..9.0 -> 100
            s > 0.0 && s < 7.0 -> safePercent(s, 7.0)
            s > 9.0 -> max(55, (100.0 - (s - 9.0) * 10.0).roundToInt())
            else -> 0
        }
        val hasAlarms = activeAlarmsCount > 0
        val completedAlarms = max(0, activeAlarmsCount - max(0, dueAlarmsCount))
        val adherencePct = if (hasAlarms) safePercent(completedAlarms.toDouble(), activeAlarmsCount.toDouble()) else 100
        val weighted = if (hasAlarms) {
            hydrationPct * 0.28 + stepsPct * 0.28 + sleepPct * 0.24 + adherencePct * 0.20
        } else {
            hydrationPct * 0.35 + stepsPct * 0.35 + sleepPct * 0.30
        }
        return ReadinessBreakdown(
            score = weighted.roundToInt().coerceIn(0, 100),
            hydrationPct = hydrationPct,
            stepsPct = stepsPct,
            sleepPct = sleepPct,
            adherencePct = adherencePct,
            hasScheduledAlarms = hasAlarms,
        )
    }

    data class Parsed12hTime(val hour12: Int, val minute: Int, val period: String)

    fun parse24hTo12h(time24h: String): Parsed12hTime {
        val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(time24h.trim().ifEmpty { "08:00" })
        val hh = m?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val mm = m?.groupValues?.get(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val period = if (hh >= 12) "PM" else "AM"
        val hour12 = if (hh % 12 == 0) 12 else hh % 12
        return Parsed12hTime(hour12, mm, period)
    }

    fun to24hTime(hour12: Int, minute: Int, period: String): String {
        val h12 = hour12.coerceIn(1, 12)
        val min = minute.coerceIn(0, 59)
        var hh = h12 % 12
        if (period.uppercase() == "PM") hh += 12
        return "${hh.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}"
    }

    fun formatTime12h(time24h: String): String {
        val (h, m, p) = parse24hTo12h(time24h)
        return "$h:${m.toString().padStart(2, '0')} $p"
    }

    // --- High-pass gravity-filtered peak/valley step detector ---
    data class StepDetectorState(
        var gravityEstimate: Double? = null,
        val smoothBuffer: ArrayDeque<Double> = ArrayDeque(),
        var peakArmed: Boolean = false,
        var peakValue: Double = 0.0,
        var lastStepAtMs: Long = 0L,
    )

    fun processMotionSample(
        state: StepDetectorState,
        x: Double,
        y: Double,
        z: Double,
        nowMs: Long,
        sensitive: Boolean = false,
    ): Boolean {
        val mag = sqrt(x * x + y * y + z * z)
        if (!mag.isFinite()) return false
        state.gravityEstimate = state.gravityEstimate?.let { it * 0.88 + mag * 0.12 } ?: mag
        val linearAcc = mag - (state.gravityEstimate ?: mag)
        state.smoothBuffer.addLast(linearAcc)
        if (state.smoothBuffer.size > 3) state.smoothBuffer.removeFirst()
        val filtered = state.smoothBuffer.average()

        val peakThreshold = if (sensitive) 1.15 else 1.55
        val valleyThreshold = if (sensitive) -0.35 else -0.50
        val maxHumanSpike = 24.0

        if (abs(filtered) > maxHumanSpike) {
            state.peakArmed = false
            state.peakValue = 0.0
            return false
        }
        if (filtered >= peakThreshold) {
            state.peakArmed = true
            if (filtered > state.peakValue) state.peakValue = filtered
            return false
        }
        if (state.peakArmed && filtered <= valleyThreshold) {
            state.peakArmed = false
            state.peakValue = 0.0
            val elapsed = nowMs - state.lastStepAtMs
            if (state.lastStepAtMs == 0L || elapsed in 320L..1800L) {
                state.lastStepAtMs = nowMs
                return true
            }
            state.lastStepAtMs = nowMs
        }
        return false
    }

    // --- Optical Camera PPG Signal Analyzer ---
    data class PpgSampleFrame(val tSec: Double, val r: Double, val g: Double, val b: Double)

    enum class PpgFrameStatus { GOOD_FINGER, NO_FINGER, TOO_DARK, OVEREXPOSED }

    sealed class PpgAnalysisOutput {
        data class Ok(val bpm: Int, val sqi: Int, val hrvMs: Int, val channelUsed: String) : PpgAnalysisOutput()
        data class Rejected(val reason: String, val message: String) : PpgAnalysisOutput()
    }

    fun assessPpgFrameQuality(meanR: Double, meanG: Double, meanB: Double): PpgFrameStatus {
        if (meanR < 20.0 && meanG < 15.0 && meanB < 15.0) return PpgFrameStatus.TOO_DARK
        if (meanB > 65.0 && meanR < meanG * 1.2) return PpgFrameStatus.NO_FINGER
        if (meanR < meanG * 1.15 && meanR < 95.0) return PpgFrameStatus.NO_FINGER
        if (meanR > 253.0 && meanG > 250.0 && meanB > 250.0) return PpgFrameStatus.OVEREXPOSED
        return PpgFrameStatus.GOOD_FINGER
    }

    fun analyzePpgSamples(raw: List<PpgSampleFrame>): PpgAnalysisOutput {
        if (raw.size < 90) {
            return PpgAnalysisOutput.Rejected("TOO_SHORT", "Hold fingertip steadily for at least 15–30 seconds.")
        }
        val totalDur = raw.last().tSec - raw.first().tSec
        if (totalDur < 10.0) {
            return PpgAnalysisOutput.Rejected("TOO_SHORT", "Recording too short. Hold finger gently over rear camera for a full scan.")
        }

        var goodFrames = 0
        var darkFrames = 0
        var noFingerFrames = 0
        for (f in raw) {
            when (assessPpgFrameQuality(f.r, f.g, f.b)) {
                PpgFrameStatus.GOOD_FINGER -> goodFrames++
                PpgFrameStatus.TOO_DARK -> darkFrames++
                PpgFrameStatus.NO_FINGER, PpgFrameStatus.OVEREXPOSED -> noFingerFrames++
            }
        }
        if (darkFrames > raw.size * 0.45) {
            return PpgAnalysisOutput.Rejected("TOO_DARK", "Lighting too dark. Turn on phone flash/torch or move to brighter light.")
        }
        if (goodFrames < raw.size * 0.65 || noFingerFrames > raw.size * 0.35) {
            return PpgAnalysisOutput.Rejected("NO_FINGER", "No steady fingertip contact detected on lens. Gently cover lens & flash without pressing hard.")
        }

        val targetHz = 25.0
        val startT = raw.first().tSec
        val numUniform = floor(totalDur * targetHz).toInt()
        if (numUniform < 200) {
            return PpgAnalysisOutput.Rejected("TOO_SHORT", "Insufficient uniform frames captured. Please retry.")
        }

        val uniformR = DoubleArray(numUniform)
        val uniformG = DoubleArray(numUniform)
        var srcIdx = 0
        for (i in 0 until numUniform) {
            val tTarget = startT + i / targetHz
            while (srcIdx < raw.size - 2 && raw[srcIdx + 1].tSec < tTarget) {
                srcIdx++
            }
            val a = raw[srcIdx]
            val b = raw[min(raw.size - 1, srcIdx + 1)]
            val span = max(1e-6, b.tSec - a.tSec)
            val frac = ((tTarget - a.tSec) / span).coerceIn(0.0, 1.0)
            uniformR[i] = a.r + (b.r - a.r) * frac
            uniformG[i] = a.g + (b.g - a.g) * frac
        }

        fun variance(arr: DoubleArray): Pair<Double, Double> {
            val mean = arr.average()
            val v = arr.sumOf { (it - mean) * (it - mean) } / arr.size
            return mean to v
        }
        val (rMean, _) = variance(uniformR)
        val (gMean, gVar) = variance(uniformG)
        val channelUsed = if (gVar < 0.015 || rMean > gMean * 1.4) "RED" else "BLEND"
        val primary = DoubleArray(numUniform) { i ->
            if (channelUsed == "RED") uniformR[i] else uniformR[i] * 0.65 + uniformG[i] * 0.35
        }

        fun movingAvg(arr: DoubleArray, halfWin: Int): DoubleArray {
            val out = DoubleArray(arr.size)
            for (i in arr.indices) {
                var sum = 0.0
                var cnt = 0
                val lo = max(0, i - halfWin)
                val hi = min(arr.size - 1, i + halfWin)
                for (j in lo..hi) {
                    sum += arr[j]
                    cnt++
                }
                out[i] = sum / cnt
            }
            return out
        }

        val baseline = movingAvg(primary, 17)
        val detrended = DoubleArray(numUniform) { i -> primary[i] - baseline[i] }
        val bandpassed = movingAvg(detrended, 2)

        val trim = min(25, floor(bandpassed.size * 0.1).toInt())
        val clean = bandpassed.sliceArray(trim until (bandpassed.size - trim))
        val cleanMean = clean.average()
        val cleanStd = sqrt(clean.sumOf { (it - cleanMean) * (it - cleanMean) } / clean.size)

        if (cleanStd < 0.008 || cleanStd > 25.0) {
            return PpgAnalysisOutput.Rejected(
                "LOW_SIGNAL",
                if (cleanStd < 0.008) "Pulse wave too faint. Avoid pressing too hard and rest finger lightly."
                else "Too much finger movement during scan. Rest hand flat on a table and retry."
            )
        }

        val minAmp = max(0.015, cleanStd * 0.42)
        val minPeakGapSec = 0.38
        val peaks = mutableListOf<Double>()
        for (i in 2 until clean.size - 2) {
            if (clean[i] > clean[i - 1] &&
                clean[i] >= clean[i + 1] &&
                clean[i] > clean[i - 2] &&
                clean[i] >= clean[i + 2] &&
                clean[i] > minAmp
            ) {
                val tSec = i / targetHz
                if (peaks.isEmpty() || tSec - peaks.last() >= minPeakGapSec) {
                    peaks.add(tSec)
                } else if (clean[i] > clean[(peaks.last() * targetHz).roundToInt().coerceIn(0, clean.lastIndex)]) {
                    peaks[peaks.lastIndex] = tSec
                }
            }
        }
        if (peaks.size < 6) {
            return PpgAnalysisOutput.Rejected("LOW_SIGNAL", "Could not detect 6 clear heartbeats. Keep fingertip still and warm.")
        }

        val rawIbis = mutableListOf<Double>()
        for (i in 1 until peaks.size) {
            val ibi = peaks[i] - peaks[i - 1]
            if (ibi in 0.38..1.45) rawIbis.add(ibi)
        }
        if (rawIbis.size < 4) {
            return PpgAnalysisOutput.Rejected("NOISY_RHYTHM", "Irregular motion noise during scan. Try again resting hand steady.")
        }

        val sorted = rawIbis.sorted()
        val medianIbi = sorted[sorted.size / 2]
        val consistent = rawIbis.filter { abs(it - medianIbi) / medianIbi <= 0.25 }
        if (consistent.size < max(4, floor(rawIbis.size * 0.55).toInt())) {
            return PpgAnalysisOutput.Rejected("NOISY_RHYTHM", "Pulse rhythm had motion artifacts. Hold steady or use manual tap counter.")
        }

        val meanIbi = consistent.average()
        val bpm = (60.0 / meanIbi).roundToInt()
        if (bpm !in 42..158) {
            return PpgAnalysisOutput.Rejected("NOISY_RHYTHM", "Reading outside reliable optical range. Recheck manually with tap counter.")
        }
        val ibiStd = sqrt(consistent.sumOf { (it - meanIbi) * (it - meanIbi) } / consistent.size)
        val consistencyRatio = consistent.size.toDouble() / rawIbis.size
        val sqi = (consistencyRatio * 100.0 - (ibiStd / meanIbi) * 115.0).roundToInt().coerceIn(30, 98)
        if (sqi < 50) {
            return PpgAnalysisOutput.Rejected("NOISY_RHYTHM", "Signal quality too low after motion check. Keep finger lighter over lens.")
        }
        val hrvMs = (ibiStd * 1000.0).roundToInt().coerceIn(12, 140)
        return PpgAnalysisOutput.Ok(bpm, sqi, hrvMs, channelUsed)
    }

    // --- Lab Report Regex Biomarker Extractor ---
    data class ExtractedBiomarkers(
        val hb: String? = null,
        val wbc: String? = null,
        val plt: String? = null,
        val fbs: String? = null,
        val hba1c: String? = null,
        val creatinine: String? = null,
        val cholesterol: String? = null,
        val altSgpt: String? = null,
    )

    fun extractBiomarkers(text: String): ExtractedBiomarkers {
        val unitOpt = """(?:\s*(?:g/dL|mg/dL|%|/uL|/cmm|U/L|IU/L))?"""
        val sep = """(?:\s*\([^)]+\))?$unitOpt\s*[:=\-]?\s*"""
        val opts = setOf(RegexOption.IGNORE_CASE)

        val hb = Regex("""(?:haemoglobin|hemoglobin|hgb|\bhb\b)$sep(\d{1,2}(?:\.\d{1,2})?)""", opts)
            .find(text)?.groupValues?.getOrNull(1)
        val wbc = Regex("""(?:wbc(?:\s*count)?|total\s+leukocyte(?:\s*count)?|\btlc\b)$sep(\d[\d,]{2,6})""", opts)
            .find(text)?.groupValues?.getOrNull(1)?.replace(",", "")
        val plt = Regex("""(?:platelets?(?:\s*count)?|\bplt\b)$sep(\d[\d,]{3,7})""", opts)
            .find(text)?.groupValues?.getOrNull(1)?.replace(",", "")
        val fbs = Regex("""(?:fasting\s+(?:blood\s+)?(?:glucose|sugar)|random\s+(?:blood\s+)?(?:glucose|sugar)|\bfbs\b|\brbs\b|blood\s+sugar|glucose)$sep(\d{2,3}(?:\.\d)?)""", opts)
            .find(text)?.groupValues?.getOrNull(1)
        val hba1c = Regex("""(?:hb\s*a1c|glycated\s+hemoglobin|\ba1c\b)$sep(\d{1,2}(?:\.\d{1,2})?)""", opts)
            .find(text)?.groupValues?.getOrNull(1)
        val creat = Regex("""(?:serum\s+creatinine|creatinine)$sep(\d(?:\.\d{1,2})?)""", opts)
            .find(text)?.groupValues?.getOrNull(1)
        val chol = Regex("""(?:total\s+cholesterol|cholesterol)$sep(\d{2,3}(?:\.\d)?)""", opts)
            .find(text)?.groupValues?.getOrNull(1)
        val alt = Regex("""(?:\bsgpt\b|\balt\b|alanine\s+aminotransferase)$sep(\d{1,4}(?:\.\d)?)""", opts)
            .find(text)?.groupValues?.getOrNull(1)

        return ExtractedBiomarkers(hb, wbc, plt, fbs, hba1c, creat, chol, alt)
    }
}
