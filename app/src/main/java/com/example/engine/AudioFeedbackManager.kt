package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AudioFeedbackManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    fun playClinicalChime(kind: String) {
        scope.launch {
            runCatching {
                when (kind) {
                    "red_sos" -> {
                        playTone(880.0, 180)
                        Thread.sleep(60)
                        playTone(660.0, 180)
                        Thread.sleep(60)
                        playTone(880.0, 220)
                    }
                    "alarm" -> {
                        playTone(587.33, 160)
                        Thread.sleep(50)
                        playTone(880.0, 240)
                    }
                    "success" -> {
                        playTone(523.25, 110)
                        Thread.sleep(40)
                        playTone(659.25, 160)
                    }
                    else -> playTone(520.0, 70)
                }
            }
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int) {
        val sampleRate = 22050
        val count = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-4.5 * i / count)
            val v = sin(2.0 * PI * freqHz * t) * env * 0.35
            samples[i] = (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        Thread.sleep(durationMs.toLong() + 30L)
        track.release()
    }

    fun speakOfflineGuidance(text: String, langTag: String = "ur-PK") {
        val engine = tts ?: return
        if (!ttsReady || text.isBlank()) return
        val targetLocale = if (langTag.startsWith("ur", ignoreCase = true)) {
            Locale.forLanguageTag("ur-PK")
        } else {
            Locale.US
        }
        val avail = engine.isLanguageAvailable(targetLocale)
        if (avail >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = targetLocale
        } else {
            engine.language = Locale.US
        }
        engine.setSpeechRate(0.92f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tibb_tts_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
