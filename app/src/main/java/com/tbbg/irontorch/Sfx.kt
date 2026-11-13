package com.tbbg.irontorch

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

object Sfx {
    const val SR = 22050
    private val TWO_PI = (2.0 * PI).toFloat()
    private var clickTrack: AudioTrack? = null

    fun player(data: ShortArray): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(data, 0, data.size)
        return t
    }

    private fun toShorts(buf: FloatArray): ShortArray {
        var peak = 0.0001f
        for (v in buf) peak = max(peak, abs(v))
        val g = 0.85f / peak
        val out = ShortArray(buf.size)
        for (i in buf.indices) {
            out[i] = (buf[i] * g * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    // щелчок кнопки: короткий «удар» и тик
    private fun buildClick(): ShortArray {
        val n = (0.045f * SR).toInt()
        val buf = FloatArray(n)
        val rnd = java.util.Random(3)
        for (i in 0 until n) {
            val t = i / SR.toFloat()
            val thunk = sin(TWO_PI * 150f * t) * exp(-t * 95f)
            val tick = sin(TWO_PI * 2200f * t) * exp(-t * 450f)
            val noise = (rnd.nextFloat() * 2f - 1f) * exp(-t * 300f)
            buf[i] = 0.8f * thunk + 0.6f * tick + 0.3f * noise
        }
        return toShorts(buf)
    }

    fun click() {
        try {
            val t = clickTrack ?: player(buildClick()).also { clickTrack = it }
            t.stop()
            t.reloadStaticData()
            t.play()
        } catch (e: Exception) {
        }
    }

    // звук заставки: печатная машинка, звонок каретки, зажигание старой лампы
    fun buildSplash(): ShortArray {
        val secs = 3.9f
        val n = (SR * secs).toInt()
        val buf = FloatArray(n)
        val rnd = java.util.Random(7)

        fun add(i: Int, v: Float) {
            if (i in 0 until n) buf[i] += v
        }

        fun key(at: Float, gain: Float, low: Float) {
            val st = (at * SR).toInt()
            val len = (0.07f * SR).toInt()
            for (i in 0 until len) {
                val t = i / SR.toFloat()
                val noise = (rnd.nextFloat() * 2f - 1f) * exp(-t * 240f)
                val thunk = sin(TWO_PI * 160f * low * t) * exp(-t * 95f)
                val tick = sin(TWO_PI * 2300f * t) * exp(-t * 420f)
                add(st + i, gain * (0.5f * noise + 0.75f * thunk + 0.45f * tick))
            }
        }

        fun tink(at: Float, gain: Float) {
            val st = (at * SR).toInt()
            val len = (0.12f * SR).toInt()
            for (i in 0 until len) {
                val t = i / SR.toFloat()
                val v = sin(TWO_PI * 3100f * t) * exp(-t * 140f) +
                    0.5f * sin(TWO_PI * 5200f * t) * exp(-t * 220f)
                val crackle = (rnd.nextFloat() * 2f - 1f) * exp(-t * 90f)
                add(st + i, gain * (0.55f * v + 0.55f * crackle))
            }
        }

        // печатаем IRON TORCH: 10 клавиш, пятая это пробел
        for (k in 0 until 10) {
            val space = k == 4
            val gain = (if (space) 1.0f else 0.78f) + 0.2f * rnd.nextFloat()
            key(0.39f + 0.10f * k, gain, if (space) 0.8f else 1f)
        }

        // звонок в конце строки
        val b0 = (1.36f * SR).toInt()
        for (i in 0 until SR) {
            val t = i / SR.toFloat()
            val e = exp(-t * 4.2f)
            add(
                b0 + i,
                0.30f * e * (sin(TWO_PI * 2093f * t) + 0.5f * sin(TWO_PI * 4186f * t) +
                    0.25f * sin(TWO_PI * 3135f * t))
            )
        }

        // мигание лампы вместе с вспышками на экране
        tink(1.50f, 0.55f)
        tink(1.55f, 0.25f)
        tink(1.60f, 0.65f)
        tink(1.65f, 0.25f)

        // зажигание: глухой удар
        val ig = (1.71f * SR).toInt()
        for (i in 0 until (0.5f * SR).toInt()) {
            val t = i / SR.toFloat()
            add(
                ig + i,
                0.5f * sin(TWO_PI * 85f * t) * exp(-t * 14f) +
                    0.2f * (rnd.nextFloat() * 2f - 1f) * exp(-t * 40f)
            )
        }
        tink(1.71f, 0.6f)

        // гудение балласта: 100 Гц и гармоники
        for (i in 0 until n) {
            val t = i / SR.toFloat()
            val up = ((t - 1.71f) / 0.35f).coerceIn(0f, 1f)
            val down = ((3.85f - t) / 0.45f).coerceIn(0f, 1f)
            val env = up * down
            if (env > 0f) {
                var h = 0f
                for (k in 1..6) h += sin(TWO_PI * 100f * k * t) / k
                val flick = if (t < 2.05f) 0.7f + 0.3f * sin(TWO_PI * 17f * t) else 1f
                buf[i] += 0.16f * env * flick * h
            }
        }
        return toShorts(buf)
    }
}
